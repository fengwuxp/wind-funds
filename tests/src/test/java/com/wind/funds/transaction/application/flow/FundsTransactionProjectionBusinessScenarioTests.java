package com.wind.funds.transaction.application.flow;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易投影典型业务场景集成验收。
 *
 * <p>本测试只验证资金底座已经具备的交易事实、账务事实和单笔解释能力，不把测试场景冒充为
 * VCC、全球账户或收单上层产品已经完成。</p>
 */
class FundsTransactionProjectionBusinessScenarioTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsTransactionProjectionExplainApplicationService projectionExplainApplicationService;

    /**
     * 场景：VCC 类授权发生部分清算，剩余授权仍需要后续清算或可信释放。
     * 输入：账户充值 100、授权 80、部分清算 30。
     * 输出：当前解释展示剩余授权 50 和待处理动作，账务余额与交易累计金额一致。
     * 红线：不能只看最后一条 COMPLETE 明细就把整笔授权展示为全部完成。
     */
    @Test
    void testPartialAuthorizationCompletionShouldRemainActionableInCurrentExplanation() {
        FundsAccountId account = fundingAccount("funding_user");
        var before = snapshot(balances(account, cashMappingAccount(), settlementAccount()));

        topup(account, 100L, "PROJECTION_VCC_TOPUP");
        var afterTopup = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(account, 80L, true, "PROJECTION_VCC_AUTHORIZE");
        var afterAuthorize = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(account, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(account, 30L, authorizationSn, "PROJECTION_VCC_PARTIAL_COMPLETE");
        var afterComplete = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));

        FundsTransactionProjectionExplanation explanation = explain(authorizationSn);
        assertThat(explanation.factStatus()).isEqualTo("HELD");
        assertThat(explanation.displayStatus()).isEqualTo("AUTHORIZED_HOLD");
        assertThat(explanation.operationStatus()).isEqualTo("WAITING_CAPTURE_OR_RELEASE");
        assertThat(explanation.nextAction()).isEqualTo("WAIT_FOR_CAPTURE_OR_RELEASE");
        assertThat(explanation.payload()).containsKey("transactionSummary");
        assertThat(asMap(explanation.payload().get("transactionSummary")))
                .containsEntry("status", "OPEN")
                .containsEntry("authorizedAmount", 80L)
                .containsEntry("completedAmount", 30L)
                .containsEntry("reversedAmount", 0L)
                .containsEntry("remainingAuthorizationAmount", 50L);
        assertThat(fundsTransaction(authorizationSn).getStatus().name()).isEqualTo("OPEN");
    }

    /**
     * 场景：VCC 类授权全额清算后分两次退回本金。
     * 输入：账户充值 100、授权并清算 80、先退款 30、再退款 50。
     * 输出：部分退款后原授权当前解释仍是已入账成功，本金全退后才展示 REFUNDED。
     * 红线：退款事件语义不能替代原授权聚合当前态，资金 OPEN/CLOSED 也不能直接映射 VCC 主状态。
     */
    @Test
    void testCompletedAuthorizationPartialRefundShouldKeepCurrentStateUntilPrincipalFullyRefunded() {
        FundsAccountId account = fundingAccount("funding_user");
        var before = snapshot(balances(account, cashMappingAccount(), settlementAccount()));

        topup(account, 100L, "PROJECTION_VCC_REFUND_TOPUP");
        var afterTopup = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(account, 80L, true, "PROJECTION_VCC_REFUND_AUTHORIZE");
        var afterAuthorize = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(account, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        completeAuthorization(account, 80L, authorizationSn, "PROJECTION_VCC_REFUND_COMPLETE");
        var afterComplete = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterComplete,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, -80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 80L, CURRENCY));

        refundCompletedAuthorization(account, 30L, authorizationSn, "PROJECTION_VCC_REFUND_PARTIAL");
        var afterPartialRefund = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterComplete, afterPartialRefund,
                delta(account, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY));

        FundsTransactionProjectionExplanation partialRefund = explain(authorizationSn);
        assertThat(partialRefund.factStatus()).isEqualTo("POSTED");
        assertThat(partialRefund.displayStatus()).isEqualTo("SUCCEEDED");
        assertThat(partialRefund.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(partialRefund.statusMeaning()).isEqualTo("FUNDS_POSTED");
        assertThat(asMap(partialRefund.payload().get("transactionSummary")))
                .containsEntry("status", "OPEN")
                .containsEntry("authorizedAmount", 80L)
                .containsEntry("completedAmount", 80L)
                .containsEntry("refundedAmount", 30L)
                .containsEntry("remainingAuthorizationAmount", 0L);

        refundCompletedAuthorization(account, 50L, authorizationSn, "PROJECTION_VCC_REFUND_FULL");
        var afterFullRefund = snapshot(balances(account, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterPartialRefund, afterFullRefund,
                delta(account, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(account, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -50L, CURRENCY));

        FundsTransactionProjectionExplanation fullRefund = explain(authorizationSn);
        assertThat(fullRefund.factStatus()).isEqualTo("POSTED");
        assertThat(fullRefund.displayStatus()).isEqualTo("REFUNDED");
        assertThat(fullRefund.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(fullRefund.statusMeaning()).isEqualTo("FUNDS_REFUNDED");
        assertThat(asMap(fullRefund.payload().get("transactionSummary")))
                .containsEntry("status", "CLOSED")
                .containsEntry("authorizedAmount", 80L)
                .containsEntry("completedAmount", 80L)
                .containsEntry("refundedAmount", 80L)
                .containsEntry("remainingAuthorizationAmount", 0L);
    }

    /**
     * 场景：内部余额钱包完成同币种账户间转账。
     * 输入：付款钱包充值 100，向收款钱包转账 40。
     * 输出：双方 AVAILABLE、平台账户、交易和账务事实逐步闭合，单笔解释可供详情查询。
     * 红线：交易投影不得参与余额计算或转账准入。
     */
    @Test
    void testInternalWalletTransferShouldProduceTraceableExplanation() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("projection_wallet_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        var before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        topup(payer, 100L, "PROJECTION_WALLET_TOPUP");
        var afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        transfer(payer, payee, 40L, "PROJECTION_WALLET_TRANSFER");
        var afterTransfer = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        FundsTransactionProjectionExplanation explanation = explainByBusinessSn("PROJECTION_WALLET_TRANSFER");
        assertPostedExplanation(explanation, "TRANSFER", "PROJECTION_WALLET_TRANSFER");
        assertThat(explanation.ledgerTransactionSn())
                .isEqualTo(ledgerTransactionByBusinessSn("PROJECTION_WALLET_TRANSFER").getSn());
    }

    /**
     * 场景：全球账户收到已确认外部入金，随后完成一笔已确认外部出款。
     * 输入：外部 wire 入金 120、冻结 70、出款确认 70。
     * 输出：AVAILABLE/FROZEN/CASH 逐步变化，入金和出款解释保留脱敏 external account/rail 引用。
     * 红线：本测试不模拟银行指令发送、合规判断、到账回调或通道对账。
     */
    @Test
    void testGlobalAccountCollectionAndPayoutShouldExplainExternalRailReferences() {
        FundsAccountId globalAccount = fundingAccount("global_usd");
        ensureLedger(globalAccount, LedgerSubjectCode.AVAILABLE);
        ensureLedger(globalAccount, LedgerSubjectCode.FROZEN);
        var before = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));

        topup(globalAccount, 120L, "PROJECTION_GLOBAL_COLLECTION");
        var afterCollection = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterCollection,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 120L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -120L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(globalAccount, 70L, "PROJECTION_GLOBAL_PAYOUT_FREEZE");
        var afterFreeze = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterCollection, afterFreeze,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(globalAccount, 70L, freezeSn, "PROJECTION_GLOBAL_PAYOUT");
        var afterPayout = snapshot(balances(globalAccount, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterPayout,
                delta(globalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(globalAccount, LedgerSubjectCode.FROZEN, -70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 70L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        FundsTransactionProjectionExplanation collection = explainByBusinessSn("PROJECTION_GLOBAL_COLLECTION");
        FundsTransactionProjectionExplanation payout = explainByBusinessSn("PROJECTION_GLOBAL_PAYOUT");
        assertPostedExplanation(collection, "TOPUP", "PROJECTION_GLOBAL_COLLECTION");
        assertPostedExplanation(payout, "WITHDRAW", "PROJECTION_GLOBAL_PAYOUT");
        assertExternalAccountRef(collection, "BANK_TRANSFER");
        assertExternalAccountRef(payout, null);
    }

    /**
     * 场景：收单资金等价路径完成付款入商户待清算账户和部分退款。
     * 输入：付款账户充值 100、向商户 CLEARING 付款 70、按原交易路径退款 30。
     * 输出：付款和退款分别形成账务事实与可解释交易，最终商户待清算余额 40。
     * 红线：capture 不得直入 SETTLEMENT；本测试不声明 payment attempt、PSP 协议、正式清分清算、
     * 商户出款或争议运营完成。
     */
    @Test
    void testAcquiringPaymentAndPartialRefundShouldKeepSeparateExplanations() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("acq_merchant");
        ensureLedger(merchant, LedgerSubjectCode.CLEARING);
        var before = snapshot(balances(payer, merchant, cashMappingAccount(), prepaymentAccount()));

        topup(payer, 100L, "PROJECTION_ACQUIRING_TOPUP");
        var afterTopup = snapshot(balances(payer, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(merchant, LedgerSubjectCode.CLEARING, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String paymentSn = pay(payer, merchant, LedgerSubjectCode.CLEARING, 70L,
                "PROJECTION_ACQUIRING_PAYMENT");
        var afterPayment = snapshot(balances(payer, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPayment,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(merchant, LedgerSubjectCode.CLEARING, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(30L, CURRENCY)))
                .setReferenceTransactionSn(paymentSn)
                .setBusinessScene("REFUND")
                .setBusinessSn("PROJECTION_ACQUIRING_PARTIAL_REFUND")
                .setDescription("acquiring partial refund by original route"), WindOperatorFactory.system());
        var afterRefund = snapshot(balances(payer, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPayment, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(merchant, LedgerSubjectCode.CLEARING, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        FundsTransactionProjectionExplanation payment = explain(paymentSn);
        FundsTransactionProjectionExplanation refund = explainByBusinessSn("PROJECTION_ACQUIRING_PARTIAL_REFUND");
        assertPostedExplanation(payment, "PAY", "PROJECTION_ACQUIRING_PAYMENT");
        assertThat(refund.displayStatus()).isEqualTo("REFUNDED");
        assertThat(refund.statusMeaning()).isEqualTo("FUNDS_REFUNDED");
        assertThat(refund.ledgerTransactionSn())
                .isEqualTo(ledgerTransactionByBusinessSn("PROJECTION_ACQUIRING_PARTIAL_REFUND").getSn());
    }

    private FundsTransactionProjectionExplanation explainByBusinessSn(String businessSn) {
        assertThat(fundsTransactionsByBusinessSn(businessSn)).hasSize(1);
        return explain(fundsTransactionsByBusinessSn(businessSn).getFirst().getSn());
    }

    private FundsTransactionProjectionExplanation explain(String transactionSn) {
        return projectionExplainApplicationService.explain(FundsTransactionProjectionExplainQuery.builder()
                .fundsTransactionSn(transactionSn)
                .build());
    }

    private void assertPostedExplanation(FundsTransactionProjectionExplanation explanation,
                                         String businessScene,
                                         String businessSn) {
        assertThat(explanation.businessScene()).isEqualTo(businessScene);
        assertThat(explanation.businessSn()).isEqualTo(businessSn);
        assertThat(explanation.factStatus()).isEqualTo("POSTED");
        assertThat(explanation.displayStatus()).isEqualTo("SUCCEEDED");
        assertThat(explanation.operationStatus()).isEqualTo("NO_ACTION_REQUIRED");
        assertThat(explanation.statusMeaning()).isEqualTo("FUNDS_POSTED");
        assertThat(explanation.evidenceRefs())
                .contains("fundsTransaction:" + explanation.fundsTransactionSn(),
                        "routeSnapshot:" + explanation.routeSnapshotId(),
                        "ledgerTransaction:" + explanation.ledgerTransactionSn());
    }

    private void assertExternalAccountRef(FundsTransactionProjectionExplanation explanation,
                                          String channelCode) {
        assertThat(explanation.evidenceRefs()).contains("externalAccount:external_bank_001");
        assertThat(explanation.payload()).containsKey("externalAccountRef");
        Map<String, Object> externalAccountRef = asMap(explanation.payload().get("externalAccountRef"));
        assertThat(externalAccountRef)
                .containsEntry("externalAccountId", "external_bank_001")
                .containsEntry("externalAccountType", "EXTERNAL_BANK")
                .doesNotContainKey("externalAccountNo");
        if (channelCode == null) {
            assertThat(externalAccountRef).doesNotContainKey("channelCode");
        } else {
            assertThat(externalAccountRef).containsEntry("channelCode", channelCode);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }
}
