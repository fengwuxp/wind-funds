package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService;
import com.wind.funds.reconciliation.application.settlement.impl.SettlementOrderApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.funds.reconciliation.enums.SettlementReleaseCoverageStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import com.wind.funds.reconciliation.enums.SettlementReleaseLateDataStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseResultReplacementStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseLineageSupersessionStatus;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseAuthorityContextDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseDecisionDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.ReleaseSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReturnSettlementOrderToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.reconciliation.service.SettlementReleaseAuthority;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.application.impl.FundsSettlementTransactionServiceImpl;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.model.dto.FundsSettlementReleaseResultDTO;
import com.wind.funds.transaction.model.request.FundsSettlementReleaseRequest;
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

    @Autowired
    private TestSettlementReleaseAuthority settlementReleaseAuthority;

    @BeforeEach
    void clearSettlementFacts() {
        jdbcTemplate.update("DELETE FROM t_payout_receipt");
        jdbcTemplate.update("DELETE FROM t_payout_order");
        jdbcTemplate.update("DELETE FROM t_settlement_order_item");
        jdbcTemplate.update("DELETE FROM t_settlement_order");
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        failAfterLockFundsSettlementTransactionService.reset();
        settlementReleaseAuthority.reset();
    }

    @Test
    void testReleaseShouldAtomicallyMoveLockedFundsToProtectedFrozenAndReplay() {
        FundsAccountId accountId = fundingAccount("stl_release_merchant");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_TOPUP");
        insertConfirmedClearingBatch(accountId, "CLB_SETTLEMENT_RELEASE", 600L, "release");
        SettlementOrderDTO order = approve(createRequest("CLB_SETTLEMENT_RELEASE", "policy-v1"));
        GateFixture gate = prepareSettlementGateFixture(order.getSn(), "release");
        settlementOrderApplicationService.lockOrder(new LockSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                .setReconciliationRunResultSn(gate.runResultSn()), WindOperatorFactory.system());
        var before = snapshot(balance(accountId));
        ReleaseSettlementOrderRequest request = releaseRequest(order, gate, "release approved");

        SettlementOrderDTO released = settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system());
        settlementReleaseAuthority.reject();
        SettlementOrderDTO replay = settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system());

        assertThat(released.getState()).isEqualTo(SettlementOrderState.RELEASED);
        assertThat(released.getReleaseDisposition()).isEqualTo(SettlementReleaseDisposition.FROZEN);
        assertThat(released.getReleaseFundsTransactionSn()).isNotBlank();
        assertThat(released.getReleaseFreezeOrderSn()).isNotBlank();
        assertThat(released.getReleaseDigest()).hasSize(64);
        assertThat(replay.getReleaseFundsTransactionSn()).isEqualTo(released.getReleaseFundsTransactionSn());
        assertThat(replay.getReleaseFreezeOrderSn()).isEqualTo(released.getReleaseFreezeOrderSn());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_settlement_order_item WHERE settlement_order_sn = ? AND active_source_claim = 1",
                Integer.class, order.getSn())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT active_order_digest FROM t_settlement_order WHERE sn = ?", String.class, order.getSn()))
                .isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT freeze_type FROM t_funds_frozen_order WHERE sn = ?", String.class,
                released.getReleaseFreezeOrderSn())).isEqualTo("SETTLEMENT_RELEASE_HOLD");
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, -600L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.FROZEN, 600L, CURRENCY));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ledger_transaction WHERE business_sn IN (?, ?)", Integer.class,
                order.getSn() + ":RELEASE", order.getSn() + ":HOLD")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ledger_posting_plan p "
                        + "JOIN t_ledger_transaction t ON t.sn = p.ledger_transaction_sn "
                        + "WHERE t.business_sn IN (?, ?)", Integer.class,
                order.getSn() + ":RELEASE", order.getSn() + ":HOLD")).isEqualTo(2);

        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                releaseRequest(order, gate, "different reason"), WindOperatorFactory.system()))
                .hasMessageContaining("不同释放请求");
        SettlementOrderDTO rebuilt = settlementOrderApplicationService.createOrder(
                createRequest("CLB_SETTLEMENT_RELEASE", "policy-v2"), WindOperatorFactory.system());
        assertThat(rebuilt.getSn()).isNotEqualTo(order.getSn());
    }

    /**
     * 已完成资金释放的结算单不能再被取消命令当作幂等取消处理。
     */
    @Test
    void testReleasedOrderShouldRejectCancelCommand() {
        FundsAccountId accountId = fundingAccount("stl_release_cancel");
        SettlementOrderDTO order = lockedReleaseOrder(
                accountId, "CLB_RELEASE_CANCEL_REJECTED", "release-cancel-rejected");
        settlementOrderApplicationService.releaseOrder(
                releaseRequest(order, currentGate(order, "release-cancel-rejected"), "release before cancel"),
                WindOperatorFactory.system());

        assertThatThrownBy(() -> settlementOrderApplicationService.cancelOrder(
                new CancelSettlementOrderRequest()
                        .setTenantId(TENANT_ID)
                        .setSettlementOrderSn(order.getSn())
                        .setReason("cancel after release"),
                WindOperatorFactory.system()))
                .hasMessageContaining("只有 DRAFT 或 REVIEWING 结算单可以取消");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_settlement_order WHERE sn = ?", String.class, order.getSn()))
                .isEqualTo("RELEASED");
    }

    @Test
    void testConcurrentReleaseReplayShouldReturnSameFundsFacts() throws Exception {
        FundsAccountId accountId = fundingAccount("stl_release_concurrent");
        SettlementOrderDTO order = lockedReleaseOrder(
                accountId, "CLB_RELEASE_CONCURRENT", "release-concurrent");
        ReleaseSettlementOrderRequest request = releaseRequest(
                order, currentGate(order, "release-concurrent"), "concurrent release");
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SettlementOrderDTO> first = executor.submit(concurrentReleaseAttempt(startGate, request));
            Future<SettlementOrderDTO> second = executor.submit(concurrentReleaseAttempt(startGate, request));
            startGate.countDown();
            List<SettlementOrderDTO> results = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).extracting(SettlementOrderDTO::getReleaseFundsTransactionSn)
                    .containsOnly(results.getFirst().getReleaseFundsTransactionSn());
            assertThat(results).extracting(SettlementOrderDTO::getReleaseFreezeOrderSn)
                    .containsOnly(results.getFirst().getReleaseFreezeOrderSn());
            assertSingleFundsAndLedgerFactsForBusinessSn(order.getSn() + ":RELEASE", 1, 1, 2);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_funds_frozen_order WHERE business_sn = ?",
                    Integer.class, order.getSn() + ":HOLD")).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testReleaseShouldCancelCreatedPayoutAndRejectSubmittedPayout() {
        FundsAccountId createdAccount = fundingAccount("stl_pyo_created");
        SettlementOrderDTO createdOrder = lockedReleaseOrder(
                createdAccount, "CLB_RELEASE_PAYOUT_CREATED", "payout-created");
        insertPayoutOrder(createdOrder, "PYO_RELEASE_CREATED", PayoutOrderState.CREATED);
        GateFixture createdGate = currentGate(createdOrder, "payout-created");

        settlementOrderApplicationService.releaseOrder(
                releaseRequest(createdOrder, createdGate, "cancel draft payout"), WindOperatorFactory.system());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_payout_order WHERE sn = ?", String.class, "PYO_RELEASE_CREATED"))
                .isEqualTo(PayoutOrderState.CANCELLED.name());
        assertThat(settlementReleaseAuthority.lastContext().getPayoutOrder().getState())
                .isEqualTo(PayoutOrderState.CREATED);

        clearSettlementFacts();
        FundsAccountId submittedAccount = fundingAccount("stl_pyo_submitted");
        SettlementOrderDTO submittedOrder = lockedReleaseOrder(
                submittedAccount, "CLB_RELEASE_PAYOUT_SUBMITTED", "payout-submitted");
        insertPayoutOrder(submittedOrder, "PYO_RELEASE_SUBMITTED", PayoutOrderState.SUBMITTED);
        GateFixture submittedGate = currentGate(submittedOrder, "payout-submitted");
        var before = snapshot(balance(submittedAccount));

        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                releaseRequest(submittedOrder, submittedGate, "must reject"), WindOperatorFactory.system()))
                .hasMessageContaining("出款单状态不允许释放");
        assertThat(settlementOrderApplicationService.getOrder(TENANT_ID, submittedOrder.getSn()).getState())
                .isEqualTo(SettlementOrderState.LOCKED);
        assertOnlyBalanceDeltas(before, snapshot(balance(submittedAccount)),
                delta(submittedAccount, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(submittedAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY));
    }

    @Test
    void testReleaseShouldFailClosedAndRollbackAllFundsFacts() {
        FundsAccountId accountId = fundingAccount("stl_release_rollback");
        SettlementOrderDTO order = lockedReleaseOrder(accountId, "CLB_RELEASE_ROLLBACK", "rollback");
        GateFixture gate = currentGate(order, "rollback");
        ReleaseSettlementOrderRequest request = releaseRequest(order, gate, "rollback test");
        var before = snapshot(balance(accountId));

        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                releaseRequest(order, new GateFixture("missing-run", "b".repeat(64), "missing-batch"),
                        "missing gate"), WindOperatorFactory.system()))
                .hasMessageContaining("Gate 未通过");
        settlementReleaseAuthority.reject();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).hasMessageContaining("释放授权未通过");
        settlementReleaseAuthority.reset();
        failAfterLockFundsSettlementTransactionService.failReleaseWithLedgerRejection();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).isInstanceOf(LedgerPostingRejectedException.class);
        failAfterLockFundsSettlementTransactionService.failReleaseWithRuntimeException();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).isInstanceOf(IllegalStateException.class);

        SettlementOrderDTO unchanged = settlementOrderApplicationService.getOrder(TENANT_ID, order.getSn());
        assertThat(unchanged.getState()).isEqualTo(SettlementOrderState.LOCKED);
        assertThat(unchanged.getReleaseFundsTransactionSn()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_transaction WHERE business_sn = ?", Integer.class,
                order.getSn() + ":RELEASE")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_funds_frozen_order WHERE business_sn = ?", Integer.class,
                order.getSn() + ":HOLD")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_settlement_order_item WHERE settlement_order_sn = ? AND active_source_claim = 1",
                Integer.class, order.getSn())).isOne();
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.FROZEN, 0L, CURRENCY));
    }

    @Test
    void testReleaseShouldRejectInvalidAuthorityEvidenceWithoutSideEffects() {
        FundsAccountId accountId = fundingAccount("stl_release_authority");
        SettlementOrderDTO order = lockedReleaseOrder(accountId, "CLB_RELEASE_AUTHORITY", "authority");
        GateFixture gate = currentGate(order, "authority");
        ReleaseSettlementOrderRequest request = releaseRequest(order, gate, "authority validation");
        var before = snapshot(balance(accountId));

        settlementReleaseAuthority.invalidDigest();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).hasMessageContaining("SHA-256");
        settlementReleaseAuthority.emptyEvidence();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).hasMessageContaining("授权证据引用不能为空");
        settlementReleaseAuthority.expired();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).hasMessageContaining("授权结果已过期");
        settlementReleaseAuthority.emptyDisposition();
        assertThatThrownBy(() -> settlementOrderApplicationService.releaseOrder(
                request, WindOperatorFactory.system())).hasMessageContaining("处置必须为 FROZEN");

        assertThat(settlementOrderApplicationService.getOrder(TENANT_ID, order.getSn()).getState())
                .isEqualTo(SettlementOrderState.LOCKED);
        assertNoFundsOrLedgerFactsForBusinessSn(order.getSn() + ":RELEASE");
        assertNoFundsOrLedgerFactsForBusinessSn(order.getSn() + ":HOLD");
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.FROZEN, 0L, CURRENCY));
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

        CancelSettlementOrderRequest cancelRequest = new CancelSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(first.getSn()).setReason("重建策略快照");
        SettlementOrderDTO cancelled = settlementOrderApplicationService.cancelOrder(
                cancelRequest, WindOperatorFactory.system());
        LocalDateTime persistedCancelledTime = jdbcTemplate.queryForObject(
                "SELECT cancelled_time FROM t_settlement_order WHERE sn = ?",
                LocalDateTime.class, first.getSn());
        SettlementOrderDTO cancelReplay = settlementOrderApplicationService.cancelOrder(
                cancelRequest, WindOperatorFactory.system());

        assertThat(cancelReplay.getCancelledTime()).isEqualTo(persistedCancelledTime);
        assertThat(cancelReplay.getReason()).isEqualTo(cancelled.getReason());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_settlement_order_item "
                        + "WHERE settlement_order_sn = ? AND active_source_claim = 1",
                Integer.class, first.getSn())).isZero();
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
        return prepareSettlementGateFixture(settlementOrderSn, suffix).runResultSn();
    }

    private GateFixture prepareSettlementGateFixture(String settlementOrderSn, String suffix) {
        String batchSn = "settlement_recon_batch_" + suffix;
        String referenceSourceRef = "internal:settlement:" + suffix;
        String comparisonSourceRef = "external:settlement:" + suffix;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                ReconciliationGateObjectType.SETTLEMENT, settlementOrderSn, "recon-rule-1",
                "report:settlement-" + suffix, referenceSourceRef, comparisonSourceRef);
        String runResultSn = reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest().setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setMatchResults(List.of(new ReconciliationMatchResultItem()
                                .setReferenceSourceRef(referenceSourceRef)
                                .setComparisonSourceRef(comparisonSourceRef)
                                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                                .setEvidenceRef("report:settlement-" + suffix + "#line-1"))),
                WindOperatorFactory.system()).getSn();
        String resultDigest = jdbcTemplate.queryForObject(
                "SELECT result_digest FROM t_reconciliation_run_result WHERE sn = ?", String.class, runResultSn);
        return new GateFixture(runResultSn, resultDigest, batchSn);
    }

    private SettlementOrderDTO lockedReleaseOrder(FundsAccountId accountId,
                                                   String clearingBatchSn,
                                                   String suffix) {
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_TOPUP_" + suffix);
        insertConfirmedClearingBatch(accountId, clearingBatchSn, 600L, suffix);
        SettlementOrderDTO order = approve(createRequest(clearingBatchSn, "policy-v1"));
        GateFixture gate = prepareSettlementGateFixture(order.getSn(), suffix);
        return settlementOrderApplicationService.lockOrder(new LockSettlementOrderRequest()
                .setTenantId(TENANT_ID).setSettlementOrderSn(order.getSn())
                .setReconciliationRunResultSn(gate.runResultSn()), WindOperatorFactory.system());
    }

    private GateFixture currentGate(SettlementOrderDTO order, String suffix) {
        return new GateFixture(order.getReconciliationRunResultSn(), order.getReconciliationResultDigest(),
                "settlement_recon_batch_" + suffix);
    }

    private ReleaseSettlementOrderRequest releaseRequest(SettlementOrderDTO order,
                                                          GateFixture gate,
                                                          String reason) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);
        return new ReleaseSettlementOrderRequest()
                .setTenantId(TENANT_ID)
                .setSettlementOrderSn(order.getSn())
                .setReconciliationRunResultSn(gate.runResultSn())
                .setReconciliationResultDigest(gate.resultDigest())
                .setCoverageStatus(SettlementReleaseCoverageStatus.COMPLETE)
                .setCoverageDigest(FundsStableHashSupport.sha256("coverage:" + order.getSn()))
                .setWatermark(cutoff)
                .setCutoff(cutoff)
                .setRuleVersion("recon-rule-1")
                .setRuleDecisionDigest(FundsStableHashSupport.sha256("rule:" + order.getSn()))
                .setCurrentLineageBatchSn(gate.batchSn())
                .setLateDataStatus(SettlementReleaseLateDataStatus.CLOSED)
                .setResultReplacementStatus(SettlementReleaseResultReplacementStatus.CURRENT)
                .setLineageSupersessionStatus(SettlementReleaseLineageSupersessionStatus.CURRENT)
                .setApprovalRef("RELEASE_APPROVAL_" + order.getSn())
                .setReason(reason)
                .setEvidenceRefs(List.of("release:evidence:" + order.getSn()));
    }

    private void insertPayoutOrder(SettlementOrderDTO order, String payoutOrderSn, PayoutOrderState state) {
        jdbcTemplate.update("""
                        INSERT INTO t_payout_order (
                            sn, tenant_id, settlement_order_sn, settlement_subject_type, settlement_subject_id,
                            amount, currency, status, created_by, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                payoutOrderSn, TENANT_ID, order.getSn(), order.getSettlementSubjectType(),
                order.getSettlementSubjectId(), order.getNetAmount(), order.getCurrency().name(), state.name(),
                "system", 0);
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

    private Callable<SettlementOrderDTO> concurrentReleaseAttempt(CountDownLatch startGate,
                                                                   ReleaseSettlementOrderRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                return settlementOrderApplicationService.releaseOrder(request, WindOperatorFactory.system());
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

        @Bean
        TestSettlementReleaseAuthority settlementReleaseAuthority() {
            return new TestSettlementReleaseAuthority();
        }
    }

    private static final class FailAfterLockFundsSettlementTransactionService
            implements FundsSettlementTransactionService {

        private final FundsSettlementTransactionService delegate;

        private boolean failNext;

        private boolean failReleaseWithLedgerRejection;

        private boolean failReleaseWithRuntimeException;

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

        @Override
        public FundsSettlementReleaseResultDTO release(FundsSettlementReleaseRequest request, WindOperator operator) {
            FundsSettlementReleaseResultDTO result = delegate.release(request, operator);
            if (failReleaseWithLedgerRejection) {
                failReleaseWithLedgerRejection = false;
                throw new LedgerPostingRejectedException(result.getReleaseFundsTransactionSn(),
                        "simulated release freeze posting rejected");
            }
            if (failReleaseWithRuntimeException) {
                failReleaseWithRuntimeException = false;
                throw new IllegalStateException("simulated release persistence failure");
            }
            return result;
        }

        private void failNext() {
            failNext = true;
        }

        private void failReleaseWithLedgerRejection() {
            failReleaseWithLedgerRejection = true;
        }

        private void failReleaseWithRuntimeException() {
            failReleaseWithRuntimeException = true;
        }

        private void reset() {
            failNext = false;
            failReleaseWithLedgerRejection = false;
            failReleaseWithRuntimeException = false;
        }
    }

    private static final class TestSettlementReleaseAuthority implements SettlementReleaseAuthority {

        private DecisionMode mode = DecisionMode.ALLOWED;

        private SettlementReleaseAuthorityContextDTO lastContext;

        @Override
        public SettlementReleaseDecisionDTO authorize(SettlementReleaseAuthorityContextDTO context,
                                                       WindOperator operator) {
            lastContext = context;
            LocalDateTime now = LocalDateTime.now();
            SettlementReleaseDecisionDTO result = new SettlementReleaseDecisionDTO()
                    .setReleaseAllowed(mode != DecisionMode.REJECTED)
                    .setReleaseDisposition(SettlementReleaseDisposition.FROZEN)
                    .setDecisionDigest("a".repeat(64))
                    .setEvidenceRefs(List.of("authority:release-approved"))
                    .setExpiresAt(now.plusMinutes(5))
                    .setAuthorizedBy(operator.getOperatorAsText())
                    .setAuthorizedAt(now)
                    .setBlockingReason(mode == DecisionMode.REJECTED ? "release rejected for test" : null);
            return switch (mode) {
                case INVALID_DIGEST -> result.setDecisionDigest("invalid");
                case EMPTY_EVIDENCE -> result.setEvidenceRefs(List.of());
                case EXPIRED -> result.setExpiresAt(now.minusMinutes(1));
                case EMPTY_DISPOSITION -> result.setReleaseDisposition(null);
                case ALLOWED, REJECTED -> result;
            };
        }

        private void reject() {
            mode = DecisionMode.REJECTED;
        }

        private void invalidDigest() {
            mode = DecisionMode.INVALID_DIGEST;
        }

        private void emptyEvidence() {
            mode = DecisionMode.EMPTY_EVIDENCE;
        }

        private void expired() {
            mode = DecisionMode.EXPIRED;
        }

        private void emptyDisposition() {
            mode = DecisionMode.EMPTY_DISPOSITION;
        }

        private SettlementReleaseAuthorityContextDTO lastContext() {
            return lastContext;
        }

        private void reset() {
            mode = DecisionMode.ALLOWED;
            lastContext = null;
        }

        private enum DecisionMode {
            ALLOWED,
            REJECTED,
            INVALID_DIGEST,
            EMPTY_EVIDENCE,
            EXPIRED,
            EMPTY_DISPOSITION
        }
    }

    private record GateFixture(String runResultSn, String resultDigest, String batchSn) {
    }

    private record CreateAttempt(boolean succeeded, String sn, String message) {
    }
}
