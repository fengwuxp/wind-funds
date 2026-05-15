package com.capte.funds.wallet.services.impl;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.model.dto.NegativeAvailablePolicyDTO;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerProfileServiceImplTests {

    private final DefaultLedgerProfileServiceImpl service = new DefaultLedgerProfileServiceImpl();

    /**
     * 场景：普通 FundingAccount 开户时初始化基础余额桶。
     * 输入：FUNDING_BASIC profile。
     * 输出：AVAILABLE、FROZEN、AUTHORIZATION 三个 required 账目。
     * 预期：普通资金账户采用 FUNDING_ACCOUNT 主体类型，负债类贷方余额，可用余额允许受控负数。
     * 红线：冻结和授权占用不得允许静默负数。
     */
    @Test
    void testGetProfileShouldExposeFundingAccountRequiredSubjects() {
        LedgerProfileDTO profile = service.getProfile(LedgerProfileCode.FUNDING_BASIC);

        assertThat(profile.getCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(profile.getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .containsExactly(
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION
                );
        assertThat(profile.getItems())
                .allSatisfy(item -> {
                    assertThat(item.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
                    assertThat(item.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
                    assertThat(item.getRequired()).isTrue();
                });
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.FROZEN)
                .getAllowNegative()).isFalse();
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AUTHORIZATION)
                .getAllowNegative()).isFalse();
    }

    /**
     * 场景：商户 FundingAccount 需要表达清算、可用、结算、冻结和调整挂账。
     * 输入：FUNDING_MERCHANT profile。
     * 输出：CLEARING、AVAILABLE、SETTLEMENT、FROZEN、ADJUSTMENT 五个 required 账目。
     * 预期：清算和挂账使用专门类别，结算和冻结不允许负数，可用余额允许受控负数。
     * 红线：商户清结算资金不得混入平台费用或平台挂账账户。
     */
    @Test
    void testGetProfileShouldExposeMerchantFundingAccountRequiredSubjects() {
        LedgerProfileDTO profile = service.getProfile(LedgerProfileCode.FUNDING_MERCHANT);

        assertThat(profile.getCode()).isEqualTo(LedgerProfileCode.FUNDING_MERCHANT);
        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(profile.getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .containsExactly(
                        LedgerSubjectCode.CLEARING,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.SETTLEMENT,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.ADJUSTMENT
                );

        LedgerProfileItemDTO clearing =
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.CLEARING);
        LedgerProfileItemDTO available =
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.AVAILABLE);
        LedgerProfileItemDTO settlement =
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.SETTLEMENT);
        LedgerProfileItemDTO frozen =
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.FROZEN);
        LedgerProfileItemDTO adjustment =
                service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.ADJUSTMENT);

        assertThat(clearing.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CLEARING);
        assertThat(clearing.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(clearing.getAllowNegative()).isFalse();
        assertThat(available.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(available.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(available.getAllowNegative()).isTrue();
        assertThat(settlement.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(settlement.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(settlement.getAllowNegative()).isFalse();
        assertThat(frozen.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(frozen.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(frozen.getAllowNegative()).isFalse();
        assertThat(adjustment.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.SUSPENSE);
        assertThat(adjustment.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(adjustment.getAllowNegative()).isFalse();
    }

    /**
     * 场景：平台 FundingAccount 按角色解析后需要落到对应平台 profile 账目。
     * 输入：FUNDING_PLATFORM profile。
     * 输出：CASH、PREPAYMENT、CLEARING、SETTLEMENT、FEE、ADJUSTMENT 六个 required 账目。
     * 预期：现金映射为资产借方，预收待付和结算应付为负债贷方，费用为收入贷方，清算和调整允许受控负数。
     * 红线：平台角色不得缺少 ADJUSTMENT，也不得混入 FROZEN、AUTHORIZATION 等交易状态账目。
     */
    @Test
    void testGetProfileShouldExposePlatformFundingAccountRequiredSubjects() {
        LedgerProfileDTO profile = service.getProfile(LedgerProfileCode.FUNDING_PLATFORM);

        assertThat(profile.getCode()).isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(profile.getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .containsExactly(
                        LedgerSubjectCode.CASH,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.CLEARING,
                        LedgerSubjectCode.SETTLEMENT,
                        LedgerSubjectCode.FEE,
                        LedgerSubjectCode.ADJUSTMENT
                );

        LedgerProfileItemDTO cash =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.CASH);
        LedgerProfileItemDTO prepayment =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.PREPAYMENT);
        LedgerProfileItemDTO clearing =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.CLEARING);
        LedgerProfileItemDTO settlement =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.SETTLEMENT);
        LedgerProfileItemDTO fee =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.FEE);
        LedgerProfileItemDTO adjustment =
                service.getRequiredItem(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.ADJUSTMENT);

        assertThat(cash.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.ASSET);
        assertThat(cash.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(cash.getAllowNegative()).isFalse();
        assertThat(prepayment.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(prepayment.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(prepayment.getAllowNegative()).isFalse();
        assertThat(clearing.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CLEARING);
        assertThat(clearing.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(clearing.getAllowNegative()).isTrue();
        assertThat(settlement.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(settlement.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(settlement.getAllowNegative()).isFalse();
        assertThat(fee.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.REVENUE);
        assertThat(fee.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(fee.getAllowNegative()).isFalse();
        assertThat(adjustment.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.SUSPENSE);
        assertThat(adjustment.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(adjustment.getAllowNegative()).isTrue();
    }

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
