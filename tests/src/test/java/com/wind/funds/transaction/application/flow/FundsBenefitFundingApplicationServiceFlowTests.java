package com.wind.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.transaction.application.FundsBenefitFundingApplicationService;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsBenefitFundingSourceType;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.dto.FundsBenefitFundingSourceDTO;
import com.wind.funds.transaction.model.request.FundsBenefitFundingApplyRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingReverseRequest;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 权益让利资金交易应用服务流程测试。
 *
 * <p>验证权益让利应用服务不直接写账务事实，而是委派标准直接交易链路生成 route snapshot、
 * 交易事实、posting plan、ledger entry 和余额影响。</p>
 */
class FundsBenefitFundingApplicationServiceFlowTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsBenefitFundingApplicationService benefitFundingApplicationService;

    /**
     * 场景：平台或商户责任账户向用户权益余额账户入账让利，随后发生部分退款和业务撤销。
     * 输入：让利方充值 100；权益正向入账 30；退款 10；撤销 5。
     * 输出：三笔权益资金交易均返回资金交易流水，并通过标准 route、交易事实和账本分录入账。
     * 红线：权益服务不得绕过直接交易链路写 LedgerEntry；逆向事件必须引用原权益资金交易回放。
     */
    @Test
    void testApplyRefundAndReverseShouldPostThroughStandardTransactionLedgerChain() {
        FundsAccountId costBearer = fundingAccount("ben_cost");
        FundsAccountId receiver = fundingAccount("ben_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);

        topup(costBearer, 100L, "BENEFIT_CHAIN_TOPUP");
        var afterTopup = snapshot(balances(costBearer, receiver));

        String applyTransactionSn = benefitFundingApplicationService.apply(applyRequest(costBearer, receiver, 30L,
                "BENEFIT_APPLY_001"), WindOperator.system());

        assertThat(applyTransactionSn).isNotBlank();
        var afterApply = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterTopup, afterApply,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_APPLY_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_APPLY_001");
        assertLedgerEventAndBuckets("BENEFIT_APPLY_001", FundsTransactionEventType.PAY,
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);

        String refundTransactionSn = benefitFundingApplicationService.refund(new FundsBenefitFundingRefundRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(applyTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(10L, CURRENCY))
                .setBusinessScene("BENEFIT_REFUND")
                .setBusinessSn("BENEFIT_REFUND_001")
                .setOriginalOrderSn("ORDER_001")
                .setRefundReason("partial order refund"), WindOperator.system());

        assertThat(refundTransactionSn).isNotBlank();
        var afterRefund = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterApply, afterRefund,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 10L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, -10L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_REFUND_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_REFUND_001");
        assertLedgerEventAndBuckets("BENEFIT_REFUND_001", FundsTransactionEventType.REFUND,
                LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);

        String reverseTransactionSn = benefitFundingApplicationService.reverse(new FundsBenefitFundingReverseRequest()
                .setTenantId(TENANT_ID)
                .setReferenceBenefitTransactionSn(applyTransactionSn)
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("BENEFIT_REVERSE")
                .setBusinessSn("BENEFIT_REVERSE_001")
                .setOriginalOrderSn("ORDER_001")
                .setReverseReason("benefit correction"), WindOperator.system());

        assertThat(reverseTransactionSn).isNotBlank();
        var afterReverse = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(afterRefund, afterReverse,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, -5L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("BENEFIT_REVERSE_001", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("BENEFIT_REVERSE_001");
        assertLedgerEventAndBuckets("BENEFIT_REVERSE_001", FundsTransactionEventType.REFUND,
                LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);

        assertBucket(balance(costBearer), LedgerSubjectCode.AVAILABLE, 85L, CURRENCY);
        assertBucket(balance(receiver), LedgerSubjectCode.SETTLEMENT, 15L, CURRENCY);
        assertThat(fundsTransaction(applyTransactionSn).getRefundedAmount()).isEqualTo(15L);
    }

    /**
     * 场景：业务侧传入展示优惠、商户折扣等不需要入账的权益解释事实。
     * 输入：账务效果为 NO_LEDGER。
     * 输出：服务 fail-fast，不生成资金交易、交易明细、账务交易或分录。
     * 红线：不能为了返回交易流水而伪造无账务资金交易事实。
     */
    @Test
    void testApplyWithNonPostingEffectShouldFailWithoutFundsOrLedgerFacts() {
        FundsAccountId costBearer = fundingAccount("ben_no_cost");
        FundsAccountId receiver = fundingAccount("ben_no_recv");
        ensureLedger(costBearer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(receiver, LedgerSubjectCode.SETTLEMENT);
        var before = snapshot(balances(costBearer, receiver));

        assertThatThrownBy(() -> benefitFundingApplicationService.apply(applyRequest(costBearer, receiver, 20L,
                "BENEFIT_NO_LEDGER_001").setLedgerEffect(FundsBenefitLedgerEffect.NO_LEDGER), WindOperator.system()))
                .hasMessageContaining("权益让利账务效果");

        var after = snapshot(balances(costBearer, receiver));
        assertOnlyBalanceDeltas(before, after,
                delta(costBearer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(receiver, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertNoFundsOrLedgerFactsForBusinessSn("BENEFIT_NO_LEDGER_001");
    }

    private FundsBenefitFundingApplyRequest applyRequest(FundsAccountId costBearer,
                                                         FundsAccountId receiver,
                                                         long amount,
                                                         String businessSn) {
        return new FundsBenefitFundingApplyRequest()
                .setTenantId(TENANT_ID)
                .setBusinessScene("BENEFIT_APPLY")
                .setBusinessSn(businessSn)
                .setOriginalOrderSn("ORDER_001")
                .setReferenceTransactionSn("PAY_ORDER_001")
                .setCostBearerSubjectRef(subjectRef(costBearer))
                .setBenefitReceiverSubjectRef(subjectRef(receiver))
                .setAmount(Money.immutable(amount, CURRENCY))
                .setFundingNature(FundsBenefitFundingNature.PLATFORM_OWN_FUNDS)
                .setLedgerEffect(FundsBenefitLedgerEffect.POSTING_REQUIRED)
                .setBenefitFundingSources(List.of(new FundsBenefitFundingSourceDTO()
                        .setSourceType(FundsBenefitFundingSourceType.COUPON)
                        .setSourceId("COUPON_001")
                        .setRuleId("RULE_001")
                        .setRuleVersion("v1")
                        .setAmount(Money.immutable(amount, CURRENCY))));
    }

    private SubjectRef subjectRef(FundsAccountId accountId) {
        return ImmutableSubjectRef.builder()
                .tenantId(TENANT_ID)
                .subjectId(accountId.id())
                .subjectType(FundsSubjectType.valueOf(accountId.type()))
                .currency(CURRENCY.name())
                .build();
    }

    private void assertLedgerEventAndBuckets(String businessSn,
                                             FundsTransactionEventType eventType,
                                             LedgerSubjectCode sourceBucket,
                                             LedgerSubjectCode targetBucket) {
        LedgerTransaction transaction = ledgerTransactionByBusinessSn(businessSn);
        assertThat(transaction.getEventType()).isEqualTo(eventType.name());
        assertThat(entriesOf(transaction))
                .extracting(LedgerEntry::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(sourceBucket, targetBucket);
    }
}
