package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 清算确认资金事实流程测试。
 */
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
class FundsClearingTransactionFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsClearingTransactionService clearingTransactionService;

    @Test
    void testConfirmShouldMoveClearingToAvailableIdempotently() {
        FundsAccountId accountId = fundingAccount("clearing_merchant");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        FundsAccountId payerAccountId = fundingAccount("funding_user");
        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payerAccountId)
                .setFundsSourceAccountId(FundsAccountId.immutable(
                        "external_clearing_source", DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("CLEARING_SOURCE_CHANNEL_001")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(1_000L, CURRENCY)))
                .setBusinessScene("CLEARING_TEST_TOPUP")
                .setBusinessSn("CLEARING_TEST_TOPUP_001"), WindOperatorFactory.system());
        String sourceTransactionSn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payerAccountId)
                .setPayeeId(accountId)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(1_000L, CURRENCY)))
                .setBusinessScene("CLEARING_TEST_PAY")
                .setBusinessSn("CLEARING_TEST_PAY_001"), WindOperatorFactory.system());
        BalanceSnapshot before = snapshot(balance(accountId));
        FundsClearingConfirmRequest request = new FundsClearingConfirmRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(600L, CURRENCY))
                .setClearingBatchSn("CLEARING_BATCH_001")
                .setSourceTransactionSns(List.of(sourceTransactionSn))
                .setDescription("merchant clearing confirmation");

        String first = clearingTransactionService.confirm(request, WindOperatorFactory.system());
        String replay = clearingTransactionService.confirm(request, WindOperatorFactory.system());

        BalanceSnapshot after = snapshot(balance(accountId));
        assertOnlyBalanceDeltas(before, after,
                delta(accountId, LedgerSubjectCode.CLEARING, -600L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 600L, CURRENCY));
        assertThat(replay).isEqualTo(first);
        assertThat(fundsTransactionsByBusinessSn("CLEARING_BATCH_001"))
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getTransactionMode()).isEqualTo(FundsTransactionMode.DIRECT);
                    assertThat(transaction.getState()).isEqualTo(FundsTransactionState.CLOSED);
                    assertThat(transaction.getCompletedAmount()).isEqualTo(600L);
                });
        assertThat(ledgerTransactionByBusinessSn("CLEARING_BATCH_001").getEventType())
                .isEqualTo(FundsTransactionEventType.CLEARING_CONFIRM.name());
        assertThat(fundsTransactionDetailsByBusinessSn("CLEARING_BATCH_001"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.RELEASE));
        assertThat(postingPlansOf(ledgerTransactionByBusinessSn("CLEARING_BATCH_001")))
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT.name());
                    assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.WITHIN_SUBJECT.name());
                    assertThat(plan.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.RELEASE.name());
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, first))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.getLegs())
                        .singleElement()
                        .satisfies(leg -> {
                            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.NON_REPLAYABLE);
                            assertThat(leg.getSourceNode().getSubjectRef())
                                    .isEqualTo(leg.getTargetNode().getSubjectRef());
                        }));
        assertThat(entriesByFundsTransactionSn(first)).hasSize(2);
        assertLedgerFactsFollowRouteSnapshot("CLEARING_BATCH_001");

        assertThatThrownBy(() -> clearingTransactionService.confirm(new FundsClearingConfirmRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(500L, CURRENCY))
                .setClearingBatchSn("CLEARING_BATCH_001")
                .setSourceTransactionSns(List.of(sourceTransactionSn))
                .setDescription("merchant clearing confirmation"), WindOperatorFactory.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balance(accountId));
        assertOnlyBalanceDeltas(after, afterConflict,
                delta(accountId, LedgerSubjectCode.CLEARING, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("CLEARING_BATCH_001", 1, 1, 2);
    }

    @Test
    void testConfirmWithInsufficientClearingBalanceShouldRejectWithoutLedgerSideEffects() {
        FundsAccountId accountId = fundingAccount("clearing_insufficient");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        FundsAccountId payerAccountId = fundingAccount("clr_ins_payer");
        ensureLedger(payerAccountId, LedgerSubjectCode.AVAILABLE);
        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payerAccountId)
                .setFundsSourceAccountId(FundsAccountId.immutable(
                        "external_clr_ins", DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("CLEARING_INSUFFICIENT_SOURCE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(1L, CURRENCY)))
                .setBusinessScene("CLEARING_INSUFFICIENT_SOURCE_TOPUP")
                .setBusinessSn("CLEARING_INSUFFICIENT_SOURCE_TOPUP_001"), WindOperatorFactory.system());
        String sourceTransactionSn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payerAccountId)
                .setPayeeId(accountId)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(1L, CURRENCY)))
                .setBusinessScene("CLEARING_INSUFFICIENT_SOURCE_PAY")
                .setBusinessSn("CLEARING_INSUFFICIENT_SOURCE_PAY_001"), WindOperatorFactory.system());
        BalanceSnapshot before = snapshot(balance(accountId));
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> clearingTransactionService.confirm(new FundsClearingConfirmRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(2L, CURRENCY))
                .setClearingBatchSn("CLEARING_BATCH_INSUFFICIENT")
                .setSourceTransactionSns(List.of(sourceTransactionSn))
                .setDescription("insufficient clearing balance"), WindOperatorFactory.system()))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot after = snapshot(balance(accountId));
        assertOnlyBalanceDeltas(before, after,
                delta(accountId, LedgerSubjectCode.CLEARING, 0L, CURRENCY),
                delta(accountId, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertFailedFundsTransactionWithoutLedgerFacts("CLEARING_BATCH_INSUFFICIENT");
    }

    @Test
    void testConfirmShouldRejectRefundedSourceBeforeCreatingClearingFacts() {
        FundsAccountId accountId = fundingAccount("clr_refund_merchant");
        ensureFundingAccount(accountId, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(accountId, LedgerSubjectCode.CLEARING);
        ensureLedger(accountId, LedgerSubjectCode.AVAILABLE);
        FundsAccountId payerAccountId = fundingAccount("clr_refund_payer");
        ensureLedger(payerAccountId, LedgerSubjectCode.AVAILABLE);
        directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payerAccountId)
                .setFundsSourceAccountId(FundsAccountId.immutable(
                        "external_clr_refund", DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.BANK_TRANSFER)
                .setChannelTransactionSn("CLEARING_REFUNDED_SOURCE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setBusinessScene("CLEARING_REFUNDED_SOURCE_TOPUP")
                .setBusinessSn("CLEARING_REFUNDED_SOURCE_TOPUP_001"), WindOperatorFactory.system());
        String sourceTransactionSn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payerAccountId)
                .setPayeeId(accountId)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.CLEARING)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(100L, CURRENCY)))
                .setBusinessScene("CLEARING_REFUNDED_SOURCE_PAY")
                .setBusinessSn("CLEARING_REFUNDED_SOURCE_PAY_001"), WindOperatorFactory.system());
        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setReferenceTransactionSn(sourceTransactionSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("CLEARING_REFUNDED_SOURCE_REFUND")
                .setBusinessSn("CLEARING_REFUNDED_SOURCE_REFUND_001"), WindOperatorFactory.system());
        var beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> clearingTransactionService.confirm(new FundsClearingConfirmRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(90L, CURRENCY))
                .setClearingBatchSn("CLEARING_BATCH_REFUNDED_SOURCE")
                .setSourceTransactionSns(List.of(sourceTransactionSn)), WindOperatorFactory.system()))
                .hasMessageContaining("清算来源交易已发生退款");

        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("CLEARING_BATCH_REFUNDED_SOURCE");
    }
}
