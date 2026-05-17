package com.capte.funds.wallet.services.impl;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
     * 场景：预算组 profile 用于预算总量、可用预算和授权占用。
     * 输入：BUDGET_BASIC profile。
     * 输出：LIMIT、AVAILABLE、AUTHORIZATION 三个 required 控制账目。
     * 预期：LIMIT 为控制类借方余额，AVAILABLE/AUTHORIZATION 为控制类贷方余额，只有 AVAILABLE 可按策略受控为负。
     * 红线：预算组不是真实资金账户，不得引入 CASH、FROZEN、SETTLEMENT 或 CONSUMED 等资金/报表账目。
     */
    @Test
    void testGetProfileShouldExposeBudgetGroupRequiredSubjects() {
        LedgerProfileDTO profile = service.getProfile(LedgerProfileCode.BUDGET_BASIC);

        assertThat(profile.getCode()).isEqualTo(LedgerProfileCode.BUDGET_BASIC);
        assertThat(profile.getVersion()).isEqualTo(1);
        assertThat(profile.getSubjectType()).isEqualTo(FundsSubjectType.BUDGET_GROUP);
        assertThat(profile.getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .containsExactly(
                        LedgerSubjectCode.LIMIT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.AUTHORIZATION
                );

        LedgerProfileItemDTO limit =
                service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.LIMIT);
        LedgerProfileItemDTO available =
                service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.AVAILABLE);
        LedgerProfileItemDTO authorization =
                service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.AUTHORIZATION);

        assertThat(limit.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(limit.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(limit.getAllowNegative()).isFalse();
        assertThat(available.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(available.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(available.getAllowNegative()).isTrue();
        assertThat(authorization.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(authorization.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(authorization.getAllowNegative()).isFalse();

        assertThat(profile.getItems())
                .extracting(LedgerProfileItemDTO::getLedgerSubjectCode)
                .doesNotContain(
                        LedgerSubjectCode.CASH,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.SETTLEMENT
                );
    }

}
