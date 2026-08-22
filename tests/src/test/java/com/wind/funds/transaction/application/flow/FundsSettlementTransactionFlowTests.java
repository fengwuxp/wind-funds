package com.wind.funds.transaction.application.flow;

import com.wind.common.exception.BaseException;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.FundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsSettlementReleaseRequest;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.assertj.core.api.SoftAssertions;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class FundsSettlementTransactionFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsSettlementTransactionService settlementTransactionService;

    @Autowired
    private FundsBalanceControlInstructionConverter balanceControlInstructionConverter;

    @Autowired
    private FundsInstructionOrchestrator<FundsInstructionSpec> fundsInstructionOrchestrator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testLockShouldMoveAvailableToSettlementIdempotently() {
        assertThatCode(this::assertSuccessfulSettlementLock).doesNotThrowAnyException();
    }

    private void assertSuccessfulSettlementLock() {
        FundsAccountId accountId = fundingAccount("settlement_merchant");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1_000L, "SETTLEMENT_TOPUP_001");
        BalanceSnapshot before = snapshot(balance(accountId));
        FundsSettlementLockRequest request = request(accountId, 600L, "SETTLEMENT_ORDER_001");

        String first = settlementTransactionService.lock(request, WindOperatorFactory.system());
        String replay = settlementTransactionService.lock(request, WindOperatorFactory.system());

        assertThat(replay).isEqualTo(first);
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.AVAILABLE, -600L, CURRENCY),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, 600L, CURRENCY));
        assertThat(ledgerTransactionByBusinessSn("SETTLEMENT_ORDER_001").getEventType())
                .isEqualTo(FundsTransactionEventType.SETTLEMENT_LOCK.name());
        assertThat(postingPlansOf(ledgerTransactionByBusinessSn("SETTLEMENT_ORDER_001")))
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT.name());
                    assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME.name());
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(first))
                .hasValueSatisfying(route -> assertThat(route.getLegs())
                        .singleElement()
                        .satisfies(leg -> {
                            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.NON_REPLAYABLE);
                            assertThat(leg.getSourceNode().getSubjectRef())
                                    .isEqualTo(leg.getTargetNode().getSubjectRef());
                        }));
        assertSingleFundsAndLedgerFactsForBusinessSn("SETTLEMENT_ORDER_001", 1, 1, 2);
        assertLedgerFactsFollowRouteSnapshot("SETTLEMENT_ORDER_001");

        assertThatThrownBy(() -> settlementTransactionService.lock(
                request(accountId, 500L, "SETTLEMENT_ORDER_001"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");
    }

    @Test
    void testLockShouldRejectInsufficientAvailableBalanceWithoutLedgerFacts() {
        FundsAccountId accountId = fundingAccount("settlement_insufficient");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1L, "SETTLEMENT_TOPUP_INSUFFICIENT");
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.lock(
                request(accountId, 2L, "SETTLEMENT_ORDER_INSUFFICIENT"), WindOperatorFactory.system()))
                .hasMessageContaining("账本余额不足");

        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertFailedFundsTransactionWithoutLedgerFacts("SETTLEMENT_ORDER_INSUFFICIENT");
    }

    @Test
    void testReleaseShouldMoveSettlementIntoProtectedFrozenHoldIdempotently() {
        FundsAccountId accountId = fundingAccount("settlement_release");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_RELEASE";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        var originalLock = fundsTransaction(lockTransactionSn);
        var before = snapshot(balance(accountId));

        FundsSettlementReleaseRequest releaseRequest = new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn);
        var first = settlementTransactionService.release(releaseRequest, WindOperatorFactory.system());
        var beforeReplayFacts = ledgerFactSnapshot();
        updateAccountState(accountId, FundsAccountState.SUSPENDED);
        var replay = settlementTransactionService.release(releaseRequest, WindOperatorFactory.system());

        assertThat(replay).isEqualTo(first);
        assertLedgerTransactionFactsUnchanged(beforeReplayFacts);
        assertOnlyBalanceDeltas(before, snapshot(balance(accountId)),
                delta(accountId, LedgerSubjectCode.SETTLEMENT, -600L, CURRENCY),
                delta(accountId, LedgerSubjectCode.FROZEN, 600L, CURRENCY));
        assertThat(fundsTransaction(first.getReleaseFundsTransactionSn()))
                .satisfies(transaction -> {
                    assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
                    assertThat(transaction.getReferenceTransactionSn()).isEqualTo(lockTransactionSn);
                    assertThat(transaction.getBusinessSn()).isEqualTo(settlementOrderSn + ":RELEASE");
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                first.getReleaseFundsTransactionSn()))
                .hasValueSatisfying(route -> {
                    assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.SETTLEMENT_RELEASE);
                    assertThat(route.getLegs()).singleElement().satisfies(leg -> {
                        assertThat(leg.getSourceNode().getSubjectRef())
                                .isEqualTo(leg.getTargetNode().getSubjectRef());
                    });
                });
        assertThat(frozenOrderByBusinessSn(settlementOrderSn + ":HOLD"))
                .satisfies(order -> {
                    assertThat(order.getSn()).isEqualTo(first.getReleaseFreezeOrderSn());
                    assertThat(order.getTransactionSn()).isEqualTo(first.getReleaseFundsTransactionSn());
                    assertThat(order.getFreezeType()).isEqualTo("SETTLEMENT_RELEASE_HOLD");
                    assertThat(order.getAmount()).isEqualTo(600L);
                    assertThat(order.getCurrency()).isEqualTo(CURRENCY);
                });
        assertThat(fundsTransaction(lockTransactionSn)).isEqualTo(originalLock);
        assertSingleFundsAndLedgerFactsForBusinessSn(settlementOrderSn + ":RELEASE", 1, 1, 2);
        assertThat(ledgerTransactionByBusinessSn(settlementOrderSn + ":HOLD").getEventType())
                .isEqualTo(FundsTransactionEventType.FREEZE.name());
        assertThat(postingPlansOf(ledgerTransactionByBusinessSn(settlementOrderSn + ":HOLD")))
                .hasSize(1);
        assertThat(entriesByBusinessSn(settlementOrderSn + ":HOLD")).hasSize(2);
    }

    @Test
    void testProtectedSettlementReleaseHoldShouldRejectGenericConsumptionWithoutSideEffects() {
        FundsAccountId accountId = fundingAccount("release_protected");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_PROTECTED_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_RELEASE_PROTECTED";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        var release = settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system());
        BalanceSnapshot beforeBalance = snapshot(balance(accountId));
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> unfreeze(accountId, 600L, release.getReleaseFreezeOrderSn(),
                "GENERIC_UNFREEZE_RELEASE_HOLD"))
                .hasMessageContaining("SETTLEMENT_RELEASE_HOLD");
        assertThatThrownBy(() -> withdraw(accountId, 600L, release.getReleaseFreezeOrderSn(),
                "GENERIC_WITHDRAW_RELEASE_HOLD"))
                .hasMessageContaining("SETTLEMENT_RELEASE_HOLD");
        assertThatThrownBy(() -> unfreeze(accountId, 600L, release.getReleaseFundsTransactionSn(),
                "GENERIC_UNFREEZE_RELEASE_TXN"))
                .hasMessageContaining("未找到原路径快照");
        assertThatThrownBy(() -> withdraw(accountId, 600L, release.getReleaseFundsTransactionSn(),
                "GENERIC_WITHDRAW_RELEASE_TXN"))
                .hasMessageContaining("不存在或缺少原冻结路径");

        assertThat(snapshot(balance(accountId))).isEqualTo(beforeBalance);
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("GENERIC_UNFREEZE_RELEASE_HOLD");
        assertNoFundsOrLedgerFactsForBusinessSn("GENERIC_WITHDRAW_RELEASE_HOLD");
        assertNoFundsOrLedgerFactsForBusinessSn("GENERIC_UNFREEZE_RELEASE_TXN");
        assertNoFundsOrLedgerFactsForBusinessSn("GENERIC_WITHDRAW_RELEASE_TXN");
        assertThat(frozenOrderByBusinessSn(settlementOrderSn + ":HOLD").getSn())
                .isEqualTo(release.getReleaseFreezeOrderSn());
    }

    @Test
    void testGenericFreezeShouldRejectReservedSettlementReleaseHoldWithoutSideEffects() {
        FundsAccountId accountId = fundingAccount("release_reserved_scene");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 600L, "SETTLEMENT_RELEASE_RESERVED_TOPUP");
        BalanceSnapshot beforeBalance = snapshot(balance(accountId));
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(600L, CURRENCY))
                .setBusinessScene("SETTLEMENT_RELEASE_HOLD")
                .setBusinessSn("SETTLEMENT_ORDER_RESERVED:HOLD")
                .setDescription("must be rejected"), WindOperatorFactory.system()))
                .hasMessageContaining("SETTLEMENT_RELEASE_HOLD")
                .hasMessageContaining("结算释放");

        assertThat(snapshot(balance(accountId))).isEqualTo(beforeBalance);
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("SETTLEMENT_ORDER_RESERVED:HOLD");
    }

    @Test
    void testReleaseShouldRejectPreexistingHoldWithoutReleaseProvenanceAndRollback() {
        FundsAccountId accountId = fundingAccount("release_spoofed_hold");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_200L, "SETTLEMENT_RELEASE_SPOOF_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_SPOOFED_HOLD";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        FundsBalanceFreezeRequest spoofRequest = new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(600L, CURRENCY))
                .setBusinessScene("SETTLEMENT_RELEASE_HOLD")
                .setBusinessSn(settlementOrderSn + ":HOLD")
                .setDescription("legacy writer without release provenance");
        fundsInstructionOrchestrator.execute(
                balanceControlInstructionConverter.convertToFreezeInstruction(
                        spoofRequest, WindOperatorFactory.system()));
        BalanceSnapshot beforeRelease = snapshot(balance(accountId));
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system()))
                .hasMessageContaining("资金冻结单请求参数不一致");

        assertThat(snapshot(balance(accountId))).isEqualTo(beforeRelease);
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":RELEASE");
        assertThat(frozenOrderByBusinessSn(settlementOrderSn + ":HOLD").getTransactionSn()).isNull();
    }

    @Test
    void testReleaseShouldRollbackReleasedFundsWhenFrozenLedgerIsMissing() {
        FundsAccountId accountId = fundingAccount("release_no_frozen");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_MISSING_LEDGER_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_MISSING_FROZEN_LEDGER";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        BalanceSnapshot beforeRelease = snapshot(balance(accountId));
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system()))
                .hasMessageContaining("账本");

        assertThat(snapshot(balance(accountId))).isEqualTo(beforeRelease);
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":RELEASE");
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":HOLD");
    }

    @Test
    void testReleaseShouldRejectWhenAvailableLedgerIsMissingWithoutNewFacts() {
        FundsAccountId accountId = fundingAccount("release_no_available");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_MISSING_AVAILABLE_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_MISSING_AVAILABLE_LEDGER";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        jdbcTemplate.update("DELETE FROM t_ledger WHERE tenant_id = ? AND subject_id = ?"
                        + " AND ledger_subject_code = ?",
                TENANT_ID, accountId.id(), LedgerSubjectCode.AVAILABLE.name());
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system()))
                .hasMessageContaining("账本");

        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":RELEASE");
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":HOLD");
    }

    @Test
    void testReleaseShouldRejectWhenSettlementLedgerIsMissingWithoutNewFacts() {
        FundsAccountId accountId = fundingAccount("release_no_settlement");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountId, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(accountId, LedgerSubjectCode.FROZEN);
        topup(accountId, 1_000L, "SETTLEMENT_RELEASE_MISSING_SETTLEMENT_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_MISSING_SETTLEMENT_LEDGER";
        String lockTransactionSn = settlementTransactionService.lock(
                request(accountId, 600L, settlementOrderSn), WindOperatorFactory.system());
        jdbcTemplate.update("DELETE FROM t_ledger WHERE tenant_id = ? AND subject_id = ?"
                        + " AND ledger_subject_code = ?",
                TENANT_ID, accountId.id(), LedgerSubjectCode.SETTLEMENT.name());
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system()))
                .hasMessageContaining("账本");

        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":RELEASE");
        assertNoFundsOrLedgerFactsForBusinessSn(settlementOrderSn + ":HOLD");
    }

    @Test
    void testReleaseShouldRejectWrongOriginalTransactionAndProfileDriftWithoutSideEffects() {
        FundsAccountId wrongOriginalAccount = fundingAccount("release_wrong_original");
        ensureFundingAccount(wrongOriginalAccount, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(wrongOriginalAccount, LedgerSubjectCode.AVAILABLE);
        topup(wrongOriginalAccount, 600L, "SETTLEMENT_RELEASE_WRONG_ORIGINAL_TOPUP");
        String topupTransactionSn = fundsTransactionsByBusinessSn(
                "SETTLEMENT_RELEASE_WRONG_ORIGINAL_TOPUP").getFirst().getSn();
        var wrongOriginalFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(topupTransactionSn)
                .setSettlementOrderSn("SETTLEMENT_ORDER_WRONG_ORIGINAL"), WindOperatorFactory.system()))
                .hasMessageContaining("不是当前结算单已完成的 SETTLEMENT_LOCK");
        assertLedgerTransactionFactsUnchanged(wrongOriginalFacts);

        FundsAccountId driftedAccount = fundingAccount("release_profile_drift");
        ensureFundingAccount(driftedAccount, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(driftedAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(driftedAccount, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(driftedAccount, LedgerSubjectCode.FROZEN);
        topup(driftedAccount, 1_000L, "SETTLEMENT_RELEASE_PROFILE_DRIFT_TOPUP");
        String settlementOrderSn = "SETTLEMENT_ORDER_PROFILE_DRIFT";
        String lockTransactionSn = settlementTransactionService.lock(
                request(driftedAccount, 600L, settlementOrderSn), WindOperatorFactory.system());
        BalanceSnapshot beforeRelease = snapshot(balance(driftedAccount));
        var beforeProfileFacts = ledgerFactSnapshot();
        jdbcTemplate.update("UPDATE t_ledger SET ledger_profile_version = 2"
                        + " WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = ?",
                TENANT_ID, driftedAccount.id(), LedgerSubjectCode.SETTLEMENT.name());

        Throwable rejection = catchThrowable(() -> settlementTransactionService.release(new FundsSettlementReleaseRequest()
                .setLockFundsTransactionSn(lockTransactionSn)
                .setSettlementOrderSn(settlementOrderSn), WindOperatorFactory.system()));

        var afterProfileFacts = ledgerFactSnapshot();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(rejection).isInstanceOf(BaseException.class);
        softly.assertThat(snapshot(balance(driftedAccount))).isEqualTo(beforeRelease);
        softly.assertThat(afterProfileFacts.transactions()).isEqualTo(beforeProfileFacts.transactions());
        softly.assertThat(afterProfileFacts.postingPlans()).isEqualTo(beforeProfileFacts.postingPlans());
        softly.assertThat(afterProfileFacts.entries()).isEqualTo(beforeProfileFacts.entries());
        for (String businessSn : List.of(settlementOrderSn + ":RELEASE", settlementOrderSn + ":HOLD")) {
            softly.assertThat(fundsTransactionsByBusinessSn(businessSn)).isEmpty();
            softly.assertThat(fundsTransactionDetailsByBusinessSn(businessSn)).isEmpty();
            softly.assertThat(ledgerTransactionsForBusinessSn(businessSn)).isEmpty();
            softly.assertThat(entriesByBusinessSn(businessSn)).isEmpty();
        }
        softly.assertAll();
    }

    private FundsSettlementLockRequest request(FundsAccountId accountId, long amount, String settlementOrderSn) {
        return new FundsSettlementLockRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setSettlementOrderSn(settlementOrderSn)
                .setDescription("settlement funds lock");
    }
}
