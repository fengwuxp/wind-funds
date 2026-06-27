package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.FundsBenefitFundingRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingSettleRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权益让利资金应用门面契约测试。
 */
class FundsBenefitContributionTransactionServiceContractTests {

    /**
     * 场景：业务侧已经完成优惠、代金券、支付立减或商户让利决策。
     * 预期：交易层把权益让利作为可理解交易和记账的 application command service，返回资金交易流水号。
     * 红线：不得停留在“资金责任准备”DTO，也不得新增独立 marketing transaction lifecycle。
     */
    @Test
    void testBenefitContributionTransactionServiceShouldExposeTransactionCommandContract()
            throws NoSuchMethodException {
        Method settleMethod = FundsBenefitContributionTransactionService.class.getMethod("settle",
                FundsBenefitFundingSettleRequest.class,
                WindOperator.class);
        Method refundMethod = FundsBenefitContributionTransactionService.class.getMethod("refund",
                FundsBenefitFundingRefundRequest.class,
                WindOperator.class);

        assertThat(settleMethod.getReturnType()).isEqualTo(String.class);
        assertThat(refundMethod.getReturnType()).isEqualTo(String.class);
        assertThat(serviceMethodNames())
                .containsExactlyInAnyOrder("settle", "refund")
                .doesNotContain("prepareBenefitFunding",
                        "apply",
                        "reverse",
                        "authorizeBenefit",
                        "settleBenefit",
                        "refundBenefit",
                        "chargebackBenefit");
    }

    /**
     * 场景：业务、订单或营销系统已经完成让利决策和分摊。
     * 预期：请求模型一等表达成本承担主体、让利承接账务主体、金额、资金性质、原订单或交易引用和业务场景。
     * 红线：入口不要求调用方构造完整复杂权益快照、确认审批状态或外部摘要；
     * 也不把用户、营销规则、券实例、来源归因或支付工具当账务主体。
     */
    @Test
    void testSettleRequestShouldModelBenefitContributionWithoutMarketingAttribution()
            throws NoSuchMethodException {
        List<String> getterNames = Arrays.stream(FundsBenefitFundingSettleRequest.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(getterNames)
                .contains("getTenantId",
                        "getBusinessScene",
                        "getBusinessSn",
                        "getOriginalOrderSn",
                        "getReferenceTransactionSn",
                        "getCostBearerSubjectRef",
                        "getBenefitReceiverSubjectRef",
                        "getAmount",
                        "getFundingNature")
                .doesNotContain("getBenefitSnapshot",
                        "getBenefitTransactionSn",
                        "getBenefitFundingSources",
                        "getLedgerEffect",
                        "getRebateAccountRef",
                        "getCommissionParticipantRef",
                        "getConfirmationStatus",
                        "getConfirmationReferenceSn",
                        "getBenefitFundingDigest",
                        "getDescription");
    }

    /**
     * 场景：权益让利退款、业务取消或人工纠错需要引用原权益资金交易。
     * 预期：逆向请求以原交易流水号、本次业务流水号和金额作为回放、幂等和审计入口。
     * 红线：逆向请求不重新携带当前权益工具或确认状态，避免调用方按当前规则重算历史权益。
     */
    @Test
    void testRefundRequestShouldReferenceOriginalBenefitFundingTransaction() {
        List<String> refundGetterNames = Arrays.stream(FundsBenefitFundingRefundRequest.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(refundGetterNames)
                .contains("getReferenceBenefitTransactionSn",
                        "getAmount",
                        "getBusinessScene",
                        "getBusinessSn",
                        "getOriginalOrderSn");
        assertThat(refundGetterNames)
                .doesNotContain("getBenefitRefundSn",
                        "getBenefitFundingSources",
                        "getConfirmationStatus",
                        "getConfirmationReferenceSn",
                        "getBenefitFundingDigest",
                        "getDescription");
    }

    private List<String> serviceMethodNames() {
        return Arrays.stream(FundsBenefitContributionTransactionService.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }
}
