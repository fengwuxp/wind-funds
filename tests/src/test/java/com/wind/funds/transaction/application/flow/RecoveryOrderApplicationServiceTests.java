package com.wind.funds.transaction.application.flow;

import com.wind.common.exception.BaseException;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.reconciliation.application.recovery.RecoveryOrderApplicationService;
import com.wind.funds.reconciliation.application.recovery.impl.RecoveryOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.dal.mapper.RecoveryOrderMapper;
import com.wind.funds.reconciliation.dal.mapper.RecoveryResultMapper;
import com.wind.funds.reconciliation.enums.RecoveryOrderState;
import com.wind.funds.reconciliation.model.dto.RecoveryOrderDTO;
import com.wind.funds.reconciliation.model.request.CreateRecoveryOrderRequest;
import com.wind.funds.reconciliation.model.request.RecordRecoveryResultRequest;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        RecoveryOrderApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class RecoveryOrderApplicationServiceTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private RecoveryOrderApplicationService recoveryOrderApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecoveryOrderMapper recoveryOrderMapper;

    @Autowired
    private RecoveryResultMapper recoveryResultMapper;

    @BeforeEach
    void clearRecoveryFacts() {
        jdbcTemplate.update("DELETE FROM t_recovery_result");
        jdbcTemplate.update("DELETE FROM t_recovery_order");
    }

    @Test
    void testCreateShouldBeIdempotentAndRejectSourceFactDrift() {
        CreateRecoveryOrderRequest request = createRequest("source-idempotent", "merchant-idempotent", 150L);

        RecoveryOrderDTO created = recoveryOrderApplicationService.createOrder(
                request, WindOperatorFactory.system());
        RecoveryOrderDTO replay = recoveryOrderApplicationService.createOrder(
                request, WindOperatorFactory.system());

        assertThat(replay.getSn()).isEqualTo(created.getSn());
        assertThat(replay.getState()).isEqualTo(RecoveryOrderState.CREATED);
        assertThat(replay.getRemainingAmount()).isEqualTo(150L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_order", Integer.class)).isOne();

        assertThatThrownBy(() -> recoveryOrderApplicationService.createOrder(
                createRequest("source-idempotent", "merchant-idempotent", 151L),
                WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("来源事实");
    }

    @Test
    void testRecordShouldTrackPartialAndFullRecoveryWithoutCreatingFundsFacts() {
        FundsAccountId responsible = fundingAccount("rc_partial_payer");
        FundsAccountId collector = fundingAccount("rc_partial_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 200L, "recovery_partial_topup");
        RecoveryOrderDTO order = recoveryOrderApplicationService.createOrder(
                createRequest("source-partial", responsible.id(), 150L), WindOperatorFactory.system());

        String firstTransactionSn = recoveryTransfer(responsible, collector, 50L, order.getSn(), "first");
        var afterFirstFunds = snapshot(balances(responsible, collector));
        RecoveryOrderDTO partial = recoveryOrderApplicationService.recordResult(
                recordRequest(order.getSn(), firstTransactionSn, "partial-1"), WindOperatorFactory.system());

        assertThat(partial.getState()).isEqualTo(RecoveryOrderState.PARTIALLY_RECOVERED);
        assertThat(partial.getRecoveredAmount()).isEqualTo(50L);
        assertThat(partial.getRemainingAmount()).isEqualTo(100L);
        assertThat(snapshot(balances(responsible, collector))).isEqualTo(afterFirstFunds);

        String secondTransactionSn = recoveryTransfer(responsible, collector, 100L, order.getSn(), "second");
        var afterSecondFunds = snapshot(balances(responsible, collector));
        RecordRecoveryResultRequest secondResult = recordRequest(order.getSn(), secondTransactionSn, "partial-2");
        RecoveryOrderDTO recovered = recoveryOrderApplicationService.recordResult(
                secondResult, WindOperatorFactory.system());
        RecoveryOrderDTO replay = recoveryOrderApplicationService.recordResult(
                secondResult, WindOperatorFactory.system());

        assertThat(recovered.getState()).isEqualTo(RecoveryOrderState.RECOVERED);
        assertThat(recovered.getRecoveredAmount()).isEqualTo(150L);
        assertThat(recovered.getRemainingAmount()).isZero();
        assertThat(replay.getRecoveredAmount()).isEqualTo(150L);
        assertThat(snapshot(balances(responsible, collector))).isEqualTo(afterSecondFunds);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_result", Integer.class)).isEqualTo(2);
    }

    @Test
    void testRecordShouldRejectOverRecoveryAndUnrelatedFundsTransaction() {
        FundsAccountId responsible = fundingAccount("rc_guard_payer");
        FundsAccountId collector = fundingAccount("rc_guard_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 300L, "recovery_guard_topup");
        RecoveryOrderDTO order = recoveryOrderApplicationService.createOrder(
                createRequest("source-guard", responsible.id(), 100L), WindOperatorFactory.system());

        String unrelatedTransactionSn = transferWithScene(
                responsible, collector, 50L, "TRANSFER", order.getSn());
        assertThatThrownBy(() -> recoveryOrderApplicationService.recordResult(
                recordRequest(order.getSn(), unrelatedTransactionSn, "unrelated"), WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("RECOVERY");

        String excessTransactionSn = recoveryTransfer(responsible, collector, 101L, order.getSn(), "excess");
        assertThatThrownBy(() -> recoveryOrderApplicationService.recordResult(
                recordRequest(order.getSn(), excessTransactionSn, "excess"), WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("超过剩余应追金额");
        assertThat(recoveryOrderApplicationService.getOrder(TENANT_ID, order.getSn()).getRecoveredAmount()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_result", Integer.class)).isZero();
    }

    @Test
    void testFundsTransactionShouldNotBeClaimedByAnotherRecoveryOrder() {
        FundsAccountId responsible = fundingAccount("rc_claim_payer");
        FundsAccountId collector = fundingAccount("rc_claim_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 100L, "recovery_claim_topup");
        RecoveryOrderDTO owner = recoveryOrderApplicationService.createOrder(
                createRequest("source-owner", responsible.id(), 50L), WindOperatorFactory.system());
        RecoveryOrderDTO contender = recoveryOrderApplicationService.createOrder(
                createRequest("source-contender", responsible.id(), 50L), WindOperatorFactory.system());
        String transactionSn = recoveryTransfer(responsible, collector, 50L, owner.getSn(), "claim");

        recoveryOrderApplicationService.recordResult(
                recordRequest(owner.getSn(), transactionSn, "owner-result"), WindOperatorFactory.system());

        assertThatThrownBy(() -> recoveryOrderApplicationService.recordResult(
                recordRequest(contender.getSn(), transactionSn, "contender-result"), WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("其他追偿单");
        assertThat(recoveryOrderApplicationService.getOrder(TENANT_ID, contender.getSn()).getRecoveredAmount()).isZero();
    }

    @Test
    void testResultReplayShouldRejectFactDrift() {
        FundsAccountId responsible = fundingAccount("rc_replay_payer");
        FundsAccountId collector = fundingAccount("rc_replay_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 50L, "recovery_replay_topup");
        RecoveryOrderDTO order = recoveryOrderApplicationService.createOrder(
                createRequest("source-replay", responsible.id(), 50L), WindOperatorFactory.system());
        String transactionSn = recoveryTransfer(responsible, collector, 50L, order.getSn(), "replay");
        RecordRecoveryResultRequest request = recordRequest(order.getSn(), transactionSn, "replay");

        recoveryOrderApplicationService.recordResult(request, WindOperatorFactory.system());
        request.setEvidenceRef("evidence:replay-drifted");

        assertThatThrownBy(() -> recoveryOrderApplicationService.recordResult(request, WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("事实已发生漂移");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_result", Integer.class)).isOne();
    }

    @Test
    void testRecordShouldRejectInvalidCompletedFundsFacts() {
        RecoveryOrderDTO order = recoveryOrderApplicationService.createOrder(
                createRequest("source-fact-guards", "guard-subject", 100L), WindOperatorFactory.system());
        TestFundsTransactionQueryService queryService = new TestFundsTransactionQueryService();
        RecoveryOrderApplicationService guardService = new RecoveryOrderApplicationServiceImpl(
                recoveryOrderMapper, recoveryResultMapper, queryService);

        queryService.put(transaction("open-transaction", TENANT_ID,
                FundsTransactionState.PROCESSING, CURRENCY));
        assertThatThrownBy(() -> guardService.recordResult(
                recordRequest(order.getSn(), "open-transaction", "open"), WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("必须已关闭");

        queryService.put(transaction("cross-tenant-transaction", TENANT_ID + 1,
                FundsTransactionState.CLOSED, CURRENCY));
        assertThatThrownBy(() -> guardService.recordResult(
                recordRequest(order.getSn(), "cross-tenant-transaction", "tenant"), WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("租户不一致");

        queryService.put(transaction("wrong-currency-transaction", TENANT_ID,
                FundsTransactionState.CLOSED, CurrencyIsoCode.EUR));
        assertThatThrownBy(() -> guardService.recordResult(
                recordRequest(order.getSn(), "wrong-currency-transaction", "currency"),
                WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("币种不一致");

        FundsTransactionDTO missingSubject = transaction(
                "missing-subject-transaction", TENANT_ID, FundsTransactionState.CLOSED, CURRENCY);
        queryService.put(missingSubject);
        assertThatThrownBy(() -> guardService.recordResult(
                recordRequest(order.getSn(), "missing-subject-transaction", "subject"),
                WindOperatorFactory.system()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未包含责任主体成功明细");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_result", Integer.class)).isZero();
    }

    @Test
    void testConcurrentCreateShouldReturnOneRecoveryOrder() throws Exception {
        CreateRecoveryOrderRequest request = createRequest("source-create-race", "race-subject", 100L);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RecoveryAttempt> first = executor.submit(concurrentCreate(startGate, request));
            Future<RecoveryAttempt> second = executor.submit(concurrentCreate(startGate, request));
            startGate.countDown();
            List<RecoveryAttempt> attempts = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.failure()).isNull());
            assertThat(attempts).extracting(attempt -> attempt.result().getSn())
                    .containsOnly(attempts.get(0).result().getSn());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_order", Integer.class)).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testConcurrentResultIdempotencyShouldBeOwnedByOneOrder() throws Exception {
        FundsAccountId responsible = fundingAccount("rc_idem_race_payer");
        FundsAccountId collector = fundingAccount("rc_idem_race_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 100L, "recovery_idempotency_race_topup");
        RecoveryOrderDTO firstOrder = recoveryOrderApplicationService.createOrder(
                createRequest("source-idempotency-race-first", responsible.id(), 50L), WindOperatorFactory.system());
        RecoveryOrderDTO secondOrder = recoveryOrderApplicationService.createOrder(
                createRequest("source-idempotency-race-second", responsible.id(), 50L), WindOperatorFactory.system());
        RecordRecoveryResultRequest firstRequest = recordRequest(firstOrder.getSn(),
                recoveryTransfer(responsible, collector, 50L, firstOrder.getSn(), "idempotency-first"),
                "idempotency-first").setIdempotencyKey("recovery-result:shared-idempotency");
        RecordRecoveryResultRequest secondRequest = recordRequest(secondOrder.getSn(),
                recoveryTransfer(responsible, collector, 50L, secondOrder.getSn(), "idempotency-second"),
                "idempotency-second").setIdempotencyKey("recovery-result:shared-idempotency");

        assertSingleConcurrentResult(firstRequest, secondRequest, "幂等键");
    }

    @Test
    void testConcurrentFundsTransactionShouldBeClaimedByOneOrder() throws Exception {
        FundsAccountId responsible = fundingAccount("rc_tx_race_payer");
        FundsAccountId collector = fundingAccount("rc_tx_race_payee");
        ensureLedger(responsible, LedgerSubjectCode.AVAILABLE);
        ensureLedger(collector, LedgerSubjectCode.AVAILABLE);
        topup(responsible, 50L, "recovery_transaction_race_topup");
        RecoveryOrderDTO firstOrder = recoveryOrderApplicationService.createOrder(
                createRequest("source-transaction-race-first", responsible.id(), 50L), WindOperatorFactory.system());
        RecoveryOrderDTO secondOrder = recoveryOrderApplicationService.createOrder(
                createRequest("source-transaction-race-second", responsible.id(), 50L), WindOperatorFactory.system());
        String transactionSn = recoveryTransfer(
                responsible, collector, 50L, firstOrder.getSn(), "transaction-race");

        assertSingleConcurrentResult(
                recordRequest(firstOrder.getSn(), transactionSn, "transaction-first"),
                recordRequest(secondOrder.getSn(), transactionSn, "transaction-second"),
                "其他追偿单");
    }

    private CreateRecoveryOrderRequest createRequest(String sourceSn, String responsibleSubjectId, long amount) {
        return new CreateRecoveryOrderRequest()
                .setTenantId(TENANT_ID)
                .setSourceType("PAYOUT_POST_EVENT")
                .setSourceSn(sourceSn)
                .setResponsibleSubjectType("FUNDING_ACCOUNT")
                .setResponsibleSubjectId(responsibleSubjectId)
                .setExpectedAmount(amount)
                .setCurrency(CURRENCY)
                .setSourceDigest(FundsStableHashSupport.sha256("source:" + sourceSn))
                .setApprovalRef("approval:" + sourceSn)
                .setEvidenceRef("evidence:" + sourceSn);
    }

    private RecordRecoveryResultRequest recordRequest(String recoveryOrderSn,
                                                      String fundsTransactionSn,
                                                      String suffix) {
        return new RecordRecoveryResultRequest()
                .setTenantId(TENANT_ID)
                .setRecoveryOrderSn(recoveryOrderSn)
                .setFundsTransactionSn(fundsTransactionSn)
                .setIdempotencyKey("recovery-result:" + suffix)
                .setApprovalRef("approval:" + suffix)
                .setEvidenceRef("evidence:" + suffix);
    }

    private String recoveryTransfer(FundsAccountId responsible,
                                    FundsAccountId collector,
                                    long amount,
                                    String recoveryOrderSn,
                                    String suffix) {
        return transferWithScene(responsible, collector, amount, "RECOVERY", recoveryOrderSn + ":" + suffix);
    }

    private String transferWithScene(FundsAccountId responsible,
                                     FundsAccountId collector,
                                     long amount,
                                     String businessScene,
                                     String businessSn) {
        return directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(responsible)
                .setPayeeAccountId(collector)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setDescription("recovery test transfer"), WindOperatorFactory.system());
    }

    private FundsTransactionDTO transaction(String sn,
                                            Long tenantId,
                                            FundsTransactionState state,
                                            CurrencyIsoCode currency) {
        return new FundsTransactionDTO()
                .setSn(sn)
                .setTenantId(tenantId)
                .setBusinessScene("RECOVERY")
                .setState(state)
                .setAmount(10L)
                .setCurrency(currency);
    }

    private Callable<RecoveryAttempt> concurrentCreate(CountDownLatch startGate,
                                                       CreateRecoveryOrderRequest request) {
        return () -> executeConcurrently(startGate,
                () -> recoveryOrderApplicationService.createOrder(request, WindOperatorFactory.system()));
    }

    private Callable<RecoveryAttempt> concurrentRecord(CountDownLatch startGate,
                                                       RecordRecoveryResultRequest request) {
        return () -> executeConcurrently(startGate,
                () -> recoveryOrderApplicationService.recordResult(request, WindOperatorFactory.system()));
    }

    private RecoveryAttempt executeConcurrently(CountDownLatch startGate,
                                                Supplier<RecoveryOrderDTO> action) {
        TenantContextHolder.setTenantId(TENANT_ID);
        try {
            startGate.await();
            return new RecoveryAttempt(action.get(), null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new RecoveryAttempt(null, "interrupted");
        } catch (RuntimeException exception) {
            return new RecoveryAttempt(null, exception.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void assertSingleConcurrentResult(RecordRecoveryResultRequest firstRequest,
                                              RecordRecoveryResultRequest secondRequest,
                                              String expectedFailure) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RecoveryAttempt> first = executor.submit(concurrentRecord(startGate, firstRequest));
            Future<RecoveryAttempt> second = executor.submit(concurrentRecord(startGate, secondRequest));
            startGate.countDown();
            List<RecoveryAttempt> attempts = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(attempts).filteredOn(attempt -> attempt.failure() == null).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> attempt.failure() != null).singleElement()
                    .extracting(RecoveryAttempt::failure).asString().contains(expectedFailure);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_recovery_result", Integer.class)).isOne();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT SUM(recovered_amount) FROM t_recovery_order", Long.class)).isEqualTo(50L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Configuration
    @Import(RecoveryOrderApplicationServiceImpl.class)
    static class Config {
    }

    private static final class TestFundsTransactionQueryService implements FundsTransactionQueryService {

        private final Map<String, FundsTransactionDTO> transactions = new HashMap<>();

        private void put(FundsTransactionDTO transaction) {
            transactions.put(transaction.getSn(), transaction);
        }

        @Override
        public Optional<FundsTransactionDTO> queryFundsTransaction(String transactionSn) {
            return Optional.ofNullable(transactions.get(transactionSn));
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionByBusiness(Long tenantId,
                                                                            String businessScene,
                                                                            String businessSn) {
            return Optional.empty();
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionByExternalFundsFact(
                Long tenantId, String externalSourceCode, String externalFundsFactSn, FundsEffectType effectType) {
            return Optional.empty();
        }

        @Override
        public List<FundsTransactionDetailDTO> queryFundsTransactionDetails(String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(String referenceTransactionSn,
                                            FundsTransactionEventType eventType,
                                            String replayRefLegId) {
            return false;
        }

        @Override
        public Money sumConsumedReplayLegAmount(String referenceTransactionSn,
                                                FundsTransactionEventType eventType,
                                                String replayRefLegId,
                                                CurrencyIsoCode currency) {
            return Money.immutable(0L, currency);
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(String freezeOrderSn) {
            return Optional.empty();
        }
    }

    private record RecoveryAttempt(RecoveryOrderDTO result, String failure) {
    }
}
