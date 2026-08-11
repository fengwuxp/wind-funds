package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService;
import com.wind.funds.reconciliation.application.settlement.impl.SettlementOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.ReturnSettlementOrderToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.application.impl.FundsSettlementTransactionServiceImpl;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.integration.operator.WindOperatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        SettlementOrderApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class SettlementOrderApplicationServiceTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private SettlementOrderApplicationService settlementOrderApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FailAfterLockFundsSettlementTransactionService failAfterLockFundsSettlementTransactionService;

    @BeforeEach
    void clearSettlementFacts() {
        jdbcTemplate.update("DELETE FROM t_settlement_order_item");
        jdbcTemplate.update("DELETE FROM t_settlement_order");
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    @Test
    void testLifecycleShouldLockConfirmedClearingPrincipalIdempotently() {
        assertThatCode(this::assertSuccessfulLifecycle).doesNotThrowAnyException();
    }

    private void assertSuccessfulLifecycle() {
        FundsAccountId accountId = fundingAccount("stl_order_merchant");
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1_000L, "SETTLEMENT_ORDER_TOPUP_001");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_001", 600L, "source-001");

        CreateSettlementOrderRequest request = createRequest("CLB_SETTLEMENT_001", "policy-v1");
        SettlementOrderDTO created = settlementOrderApplicationService.createOrder(
                request, WindOperatorFactory.system());
        SettlementOrderDTO replay = settlementOrderApplicationService.createOrder(
                request, WindOperatorFactory.system());

        assertThat(replay.getSn()).isEqualTo(created.getSn());
        assertThat(created.getState()).isEqualTo(SettlementOrderState.DRAFT);
        assertThat(created.getNetAmount()).isEqualTo(600L);
        assertThat(created.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getSourceSn()).isEqualTo("CLB_SETTLEMENT_001");
            assertThat(item.getItemType()).isEqualTo("PRINCIPAL");
            assertThat(item.getDirection()).isEqualTo("ADD");
        });

        SettlementOrderDTO reviewing = settlementOrderApplicationService.submitOrder(
                new SubmitSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()), WindOperatorFactory.system());
        assertThat(reviewing.getState()).isEqualTo(SettlementOrderState.REVIEWING);
        SettlementOrderDTO returned = settlementOrderApplicationService.returnToDraft(
                new ReturnSettlementOrderToDraftRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()).setReason("补充结算审批证据"),
                WindOperatorFactory.system());
        assertThat(returned.getState()).isEqualTo(SettlementOrderState.DRAFT);
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest().setTenantId(TENANT_ID)
                .setSettlementOrderSn(created.getSn()), WindOperatorFactory.system());
        SettlementOrderDTO approved = settlementOrderApplicationService.approveOrder(
                new ApproveSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()).setSettlementApprovalRef("SETTLEMENT_APPROVAL_001"),
                WindOperatorFactory.system());
        assertThat(approved.getState()).isEqualTo(SettlementOrderState.APPROVED);

        String runResultSn = prepareSettlementGate(created.getSn(), "001");
        var before = snapshot(balance(accountId));
        SettlementOrderDTO locked = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()).setReconciliationRunResultSn(runResultSn),
                WindOperatorFactory.system());
        SettlementOrderDTO lockReplay = settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()).setReconciliationRunResultSn(runResultSn),
                WindOperatorFactory.system());

        assertThat(locked.getState()).isEqualTo(SettlementOrderState.LOCKED);
        assertThat(lockReplay.getLockFundsTransactionSn()).isEqualTo(locked.getLockFundsTransactionSn());
        assertThat(locked.getReconciliationRunResultSn()).isEqualTo(runResultSn);
        assertThat(locked.getReconciliationResultDigest()).hasSize(64);
        assertThat(locked.getReconciliationEvidenceDigest()).hasSize(64);
        assertThatThrownBy(() -> settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID)
                        .setSettlementOrderSn(created.getSn()).setReconciliationRunResultSn("different-run-result"),
                WindOperatorFactory.system()))
                .hasMessageContaining("不同对账运行结果");
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.AVAILABLE, -600L, CURRENCY),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 600L, CURRENCY));
    }

    @Test
    void testCreateShouldRejectUnsupportedSettlementModes() {
        FundsAccountId accountId = fundingAccount("stl_mode_merchant");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_MODE", 100L, "mode");

        assertThatThrownBy(() -> settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_MODE", "policy-v1").setSettlementMode(SettlementMode.BILL),
                WindOperatorFactory.system()))
                .hasMessageContaining("只支持中间户模式");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_settlement_order", Integer.class)).isZero();
    }

    @Test
    void testActiveSourceShouldBlockAnotherOrderUntilDraftIsCancelled() {
        FundsAccountId accountId = fundingAccount("stl_claim_merchant");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_CLAIM", 100L, "claim");
        SettlementOrderDTO first = settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_CLAIM", "policy-v1"), WindOperatorFactory.system());

        assertThatThrownBy(() -> settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_CLAIM", "policy-v2"), WindOperatorFactory.system()))
                .hasMessageContaining("已被其他有效结算单占用");

        settlementOrderApplicationService.cancelOrder(new CancelSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(first.getSn()).setReason("重建策略快照"),
                WindOperatorFactory.system());
        SettlementOrderDTO recreated = settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_CLAIM", "policy-v2"), WindOperatorFactory.system());
        assertThat(recreated.getSn()).isNotEqualTo(first.getSn());
    }

    @Test
    void testLockShouldFailOrderReleaseSourceAndAllowRebuildWhenBalanceIsInsufficient() {
        FundsAccountId accountId = fundingAccount("stl_insufficient");
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1L, "SETTLEMENT_INSUFFICIENT_TOPUP");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_INSUFFICIENT", 2L, "insufficient");
        CreateSettlementOrderRequest request = createRequest("CLB_SETTLEMENT_INSUFFICIENT", "policy-v1");
        SettlementOrderDTO order = approve(request);
        String runResultSn = prepareSettlementGate(order.getSn(), "insufficient");

        assertThatThrownBy(() -> settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                        .setReconciliationRunResultSn(runResultSn), WindOperatorFactory.system()))
                .isInstanceOf(LedgerPostingRejectedException.class)
                .hasMessageContaining("账本余额不足");
        SettlementOrderDTO failed = settlementOrderApplicationService.getOrder(TENANT_ID, order.getSn());
        assertThat(failed.getState()).isEqualTo(SettlementOrderState.FAILED);
        assertThat(failed.getLockFundsTransactionSn()).isNotBlank();
        assertFailedFundsTransactionWithoutLedgerFacts(order.getSn());
        SettlementOrderDTO rebuilt = settlementOrderApplicationService.createOrder(request, WindOperatorFactory.system());
        assertThat(rebuilt.getSn()).isNotEqualTo(order.getSn());
        assertThat(rebuilt.getState()).isEqualTo(SettlementOrderState.DRAFT);
    }

    @Test
    void testPolicySnapshotDigestShouldCoverSettlementPeriod() {
        FundsAccountId accountId = fundingAccount("stl_period");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_PERIOD_1", 100L, "period-1");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_PERIOD_2", 100L, "period-2");

        SettlementOrderDTO first = settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_PERIOD_1", "policy-v1").setSettlementPeriod("2026-07"),
                WindOperatorFactory.system());
        SettlementOrderDTO second = settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_PERIOD_2", "policy-v1").setSettlementPeriod("2026-08"),
                WindOperatorFactory.system());

        assertThat(first.getPolicySnapshot().getSettlementPeriod()).isEqualTo("2026-07");
        assertThat(second.getPolicySnapshot().getSettlementPeriod()).isEqualTo("2026-08");
        assertThat(first.getPolicySnapshotDigest()).isNotEqualTo(second.getPolicySnapshotDigest());
    }

    @Test
    void testTechnicalFailureAfterFundsPostingShouldRollbackAndKeepApproved() {
        FundsAccountId accountId = fundingAccount("stl_unknown");
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 100L, "SETTLEMENT_UNKNOWN_TOPUP");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_UNKNOWN", 100L, "unknown");
        SettlementOrderDTO order = approve(createRequest("CLB_SETTLEMENT_UNKNOWN", "policy-v1"));
        String runResultSn = prepareSettlementGate(order.getSn(), "unknown");
        var before = snapshot(balance(accountId));
        failAfterLockFundsSettlementTransactionService.failNext();

        assertThatThrownBy(() -> settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                        .setReconciliationRunResultSn(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("simulated settlement result unknown");
        assertThat(settlementOrderApplicationService.getOrder(TENANT_ID, order.getSn()).getState())
                .isEqualTo(SettlementOrderState.APPROVED);
        assertNoFundsOrLedgerFactsForBusinessSn(order.getSn());
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
    }

    @Test
    void testGateRejectionShouldKeepApprovedWithoutFundsFact() {
        FundsAccountId accountId = fundingAccount("stl_gate");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_GATE", 100L, "gate");
        SettlementOrderDTO order = approve(createRequest("CLB_SETTLEMENT_GATE", "policy-v1"));

        assertThatThrownBy(() -> settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                        .setReconciliationRunResultSn("missing-run-result"), WindOperatorFactory.system()))
                .hasMessageContaining("对账 Gate 未通过");
        assertThat(settlementOrderApplicationService.getOrder(TENANT_ID, order.getSn()).getState())
                .isEqualTo(SettlementOrderState.APPROVED);
        assertNoFundsOrLedgerFactsForBusinessSn(order.getSn());
    }

    @Test
    void testConcurrentOrdersShouldClaimSourceOnlyOnce() throws Exception {
        FundsAccountId accountId = fundingAccount("stl_concurrent");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_CONCURRENT", 100L, "concurrent");
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CreateAttempt> first = executor.submit(concurrentCreateAttempt(startGate,
                    createRequest("CLB_SETTLEMENT_CONCURRENT", "policy-v1")));
            Future<CreateAttempt> second = executor.submit(concurrentCreateAttempt(startGate,
                    createRequest("CLB_SETTLEMENT_CONCURRENT", "policy-v2")));
            startGate.countDown();
            List<CreateAttempt> attempts = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(attempts).filteredOn(CreateAttempt::succeeded).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.succeeded()).singleElement()
                    .extracting(CreateAttempt::message).asString().contains("已被其他有效结算单占用");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_settlement_order_item WHERE active_source_claim = 1", Integer.class))
                    .isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testLockShouldRejectChangedClearingSourceWithoutFundsFact() {
        FundsAccountId accountId = fundingAccount("stl_changed_merchant");
        ensureFundingAccount(accountId);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 100L, "SETTLEMENT_CHANGED_TOPUP");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_CHANGED", 100L, "changed");
        SettlementOrderDTO order = approve(createRequest("CLB_SETTLEMENT_CHANGED", "policy-v1"));
        String runResultSn = prepareSettlementGate(order.getSn(), "changed");
        jdbcTemplate.update("UPDATE t_clearing_batch SET total_amount = 101 WHERE sn = ?", "CLB_SETTLEMENT_CHANGED");

        assertThatThrownBy(() -> settlementOrderApplicationService.lockOrder(
                new LockSettlementOrderRequest().setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                        .setReconciliationRunResultSn(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("来源金额已变化");
        assertNoFundsOrLedgerFactsForBusinessSn(order.getSn());
    }

    private SettlementOrderDTO approve(CreateSettlementOrderRequest request) {
        SettlementOrderDTO created = settlementOrderApplicationService.createOrder(request, WindOperatorFactory.system());
        settlementOrderApplicationService.submitOrder(new SubmitSettlementOrderRequest().setTenantId(TENANT_ID)
                .setSettlementOrderSn(created.getSn()), WindOperatorFactory.system());
        return settlementOrderApplicationService.approveOrder(new ApproveSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(created.getSn())
                .setSettlementApprovalRef("SETTLEMENT_APPROVAL_" + created.getSn()), WindOperatorFactory.system());
    }

    private CreateSettlementOrderRequest createRequest(String clearingBatchSn, String policyVersion) {
        return new CreateSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSns(List.of(clearingBatchSn))
                .setSettlementPeriod("2026-07")
                .setSettlementMode(SettlementMode.INTERMEDIARY_ACCOUNT)
                .setSettlementDestination(SettlementDestination.INTERNAL_ACCOUNT)
                .setTriggerMode(SettlementTriggerMode.HOST_COMMAND)
                .setTimezone("Asia/Shanghai")
                .setCutoff("23:00")
                .setPolicyCode("MERCHANT_SETTLEMENT")
                .setPolicyVersion(policyVersion)
                .setPolicyApprovalRef("POLICY_APPROVAL_001");
    }

    private void insertConfirmedClearingBatch(FundsAccountId accountId, String sn, long amount, String suffix) {
        String amountDigest = FundsStableHashSupport.sha256Json(Map.of(
                "source", sn, "amount", amount, "currency", CURRENCY.name()));
        jdbcTemplate.update("""
                        INSERT INTO t_clearing_batch (
                            sn, tenant_id, subject_type, subject_id, currency, business_line, clearing_period,
                            clearing_rule_code, clearing_rule_version, candidate_count, total_amount, amount_digest,
                            funds_transaction_sn, status, created_by, confirmed_by, confirmed_time
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                sn, TENANT_ID, accountId.type(), accountId.id(), CURRENCY.name(), "ACQUIRING", "2026-07",
                "CLEARING_RULE", "v1", 1, amount, amountDigest, "FT_CLEARING_" + suffix,
                "CONFIRMED", "system", "system", LocalDateTime.now());
    }

    private String prepareSettlementGate(String settlementOrderSn, String suffix) {
        String batchSn = "settlement_recon_batch_" + suffix;
        String referenceSourceRef = "internal:settlement:" + suffix;
        String comparisonSourceRef = "external:settlement:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                ReconciliationGateObjectType.SETTLEMENT, settlementOrderSn, "recon-rule-1",
                "report:settlement-" + suffix, referenceSourceRef, comparisonSourceRef);
        return reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest().setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setMatchResults(List.of(new ReconciliationMatchResultItem()
                                .setReferenceSourceRef(referenceSourceRef)
                                .setComparisonSourceRef(comparisonSourceRef)
                                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                                .setEvidenceRef("report:settlement-" + suffix + "#line-1"))),
                WindOperatorFactory.system()).getSn();
    }

    private Callable<CreateAttempt> concurrentCreateAttempt(CountDownLatch startGate,
                                                             CreateSettlementOrderRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                SettlementOrderDTO result = settlementOrderApplicationService.createOrder(
                        request, WindOperatorFactory.system());
                return new CreateAttempt(true, result.getSn(), null);
            } catch (RuntimeException exception) {
                return new CreateAttempt(false, null, exception.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        };
    }

    @Configuration
    @Import({
            SettlementOrderApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class
    })
    static class Config {

        @Bean
        @Primary
        FailAfterLockFundsSettlementTransactionService failAfterLockFundsSettlementTransactionService(
                FundsSettlementTransactionServiceImpl delegate) {
            return new FailAfterLockFundsSettlementTransactionService(delegate);
        }
    }

    private static final class FailAfterLockFundsSettlementTransactionService
            implements FundsSettlementTransactionService {

        private final FundsSettlementTransactionService delegate;

        private boolean failNext;

        private FailAfterLockFundsSettlementTransactionService(FundsSettlementTransactionService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String lock(FundsSettlementLockRequest request, WindOperator operator) {
            String result = delegate.lock(request, operator);
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("simulated settlement result unknown");
            }
            return result;
        }

        private void failNext() {
            failNext = true;
        }
    }

    private record CreateAttempt(boolean succeeded, String sn, String message) {
    }
}
