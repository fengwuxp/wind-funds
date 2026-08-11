package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class FundsSettlementTransactionFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsSettlementTransactionService settlementTransactionService;

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

    private FundsSettlementLockRequest request(FundsAccountId accountId, long amount, String settlementOrderSn) {
        return new FundsSettlementLockRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setSettlementOrderSn(settlementOrderSn)
                .setDescription("settlement funds lock");
    }
}
