package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.model.dto.NegativeAvailablePolicyDTO;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerProfileRequiredItemTests {

    private final DefaultLedgerProfileServiceImpl service = new DefaultLedgerProfileServiceImpl();

    /**
     * 场景：信用账户 profile 用于额度、可用额度和授权占用。
     * 输入：CREDIT_BASIC profile 的 LIMIT 与 AVAILABLE 账目。
     * 输出：额度控制类借方余额、可用额度控制类贷方余额。
     * 预期：额度不允许负数，可用额度允许受控负数。
     * 红线：信用额度账户不是真实资金账户，不得按资金本金入账。
     */
    @Test
    void testGetRequiredItemShouldExposeCreditLimitAndAvailableRules() {
        LedgerProfileItemDTO limit =
                service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.LIMIT);
        LedgerProfileItemDTO available =
                service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.AVAILABLE);

        assertThat(limit.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(limit.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(limit.getAllowNegative()).isFalse();
        assertThat(available.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(available.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(available.getAllowNegative()).isTrue();
    }

    /**
     * 场景：可用余额或可用额度可通过业务规则受控透支。
     * 输入：资金账户、商户资金账户、信用账户和预算组的 AVAILABLE 账目。
     * 输出：四类 AVAILABLE 均声明允许负数。
     * 预期：是否可用由上层限额、授信、预算和余额控制共同约束。
     * 红线：允许负数不等于静默透支，冻结、授权、结算等约束桶不得继承该规则。
     */
    @Test
    void testAvailableBucketShouldAllowControlledNegativeForFundingCreditAndBudgetSubjects() {
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
    }

    /**
     * 场景：LedgerProfile 对允许受控负余额的账目暴露治理策略元数据。
     * 输入：资金账户、商户资金账户、信用账户、预算组的 AVAILABLE，以及平台 CLEARING/ADJUSTMENT。
     * 输出：允许负余额的账目均带策略编码、版本、来源、原因、审批/风控、风险状态、上限、账龄和后续治理路径。
     * 预期：profile 调用方能区分“允许负余额”与“可静默扣负”，并知道后续交易必须重新校验策略。
     * 红线：不得只用 allowNegative=true 表达受控负余额，也不得给不可负账目伪造负余额策略。
     */
    @Test
    void testAllowNegativeItemsShouldExposeControlledNegativePolicyMetadata() {
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE),
                "FUNDING_AVAILABLE_CONTROLLED_NEGATIVE",
                "风控、对账、追偿、结算抵扣、后续入金抵扣、人工处理"
        );
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.AVAILABLE),
                "FUNDING_AVAILABLE_CONTROLLED_NEGATIVE",
                "风控、对账、追偿、结算抵扣、后续入金抵扣、人工处理"
        );
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.AVAILABLE),
                "CREDIT_AVAILABLE_CONTROLLED_NEGATIVE",
                "新授权策略、额度治理、账龄、报表"
        );
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.AVAILABLE),
                "BUDGET_AVAILABLE_CONTROLLED_NEGATIVE",
                "新授权策略、预算治理、周期报表"
        );
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.CLEARING),
                "PLATFORM_CLEARING_CONTROLLED_NEGATIVE",
                "对账、差错核销、人工处理"
        );
        assertPolicy(
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.ADJUSTMENT),
                "PLATFORM_ADJUSTMENT_CONTROLLED_NEGATIVE",
                "调账审批、差错核销、人工处理"
        );
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.FROZEN)
                .getNegativeAvailablePolicy()).isNull();
        assertThat(service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.AUTHORIZATION)
                .getNegativeAvailablePolicy()).isNull();
    }

    /**
     * 场景：调用方请求 profile 中不存在的账目。
     * 输入：CREDIT_BASIC profile 请求 PREPAYMENT 账目。
     * 输出：抛出异常。
     * 预期：错误路径阻止交易按错误账目继续组装 route 或 posting。
     * 红线：缺失账目不得自动补齐，也不得回退到其他 profile。
     */
    @Test
    void testGetRequiredItemShouldRejectMissingSubject() {
        assertThatThrownBy(() -> service.getRequiredItem(
                LedgerProfileCode.CREDIT_BASIC,
                LedgerSubjectCode.PREPAYMENT
        )).isInstanceOf(RuntimeException.class);
    }

    private static void assertPolicy(LedgerProfileItemDTO item, String policyCode, String governancePath) {
        assertThat(item.getAllowNegative()).isTrue();
        NegativeAvailablePolicyDTO policy = item.getNegativeAvailablePolicy();
        assertThat(policy).isNotNull();
        assertThat(policy.getPolicyCode()).isEqualTo(policyCode);
        assertThat(policy.getPolicyVersion()).isEqualTo(1);
        assertThat(policy.getRequireSourceFact()).isTrue();
        assertThat(policy.getRequireReason()).isTrue();
        assertThat(policy.getRequireApprovalOrRiskRule()).isTrue();
        assertThat(policy.getRequireRiskStatus()).isTrue();
        assertThat(policy.getRequireSingleLimit()).isTrue();
        assertThat(policy.getRequireCumulativeLimit()).isTrue();
        assertThat(policy.getRequireAgingTracking()).isTrue();
        assertThat(policy.getRecheckFutureTransaction()).isTrue();
        assertThat(policy.getGovernancePath()).isEqualTo(governancePath);
    }
}
