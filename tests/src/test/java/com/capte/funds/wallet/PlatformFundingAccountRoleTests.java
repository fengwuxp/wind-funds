package com.capte.funds.wallet;

import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformFundingAccountRoleTests {

    /**
     * 场景：产品目标态要求平台 FundingAccount 使用明确资金账户角色。
     * 输入：平台资金账户角色枚举全集。
     * 输出：现金映射、预收待付、清算过渡、结算应付、费用归集、调整挂账六类角色。
     * 预期：角色名称与中文语义稳定，供 route DSL 和配置资产引用。
     * 红线：不得把授权、冻结、消费等交易状态混入平台账户角色。
     */
    @Test
    void testPlatformFundingAccountRolesShouldUseTargetProductSemantics() {
        assertThat(PlatformFundingAccountRole.values())
                .extracting(PlatformFundingAccountRole::name)
                .containsExactly(
                        "CASH_MAPPING",
                        "PREPAYMENT",
                        "CLEARING",
                        "SETTLEMENT",
                        "FEE",
                        "ADJUSTMENT"
                );
        assertThat(PlatformFundingAccountRole.CASH_MAPPING.getDesc()).isEqualTo("现金映射");
        assertThat(PlatformFundingAccountRole.PREPAYMENT.getDesc()).isEqualTo("预收待付");
        assertThat(PlatformFundingAccountRole.CLEARING.getDesc()).isEqualTo("清算过渡");
        assertThat(PlatformFundingAccountRole.SETTLEMENT.getDesc()).isEqualTo("结算应付");
        assertThat(PlatformFundingAccountRole.FEE.getDesc()).isEqualTo("费用归集");
        assertThat(PlatformFundingAccountRole.ADJUSTMENT.getDesc()).isEqualTo("调整挂账");
    }

    /**
     * 场景：平台 FundingAccount 角色要落到目标态账簿科目。
     * 输入：平台资金账户角色枚举全集。
     * 输出：统一使用 FUNDING_PLATFORM profile，并映射到 CASH/PREPAYMENT/CLEARING/SETTLEMENT/FEE/ADJUSTMENT。
     * 预期：route 解析拿到的是配置账户的科目边界，不是交易状态或资金流动作。
     * 红线：平台账户角色不得替代真实 FundingAccount，也不得直接生成跨主体资金转移。
     */
    @Test
    void testPlatformFundingAccountRolesShouldDeclareTargetLedgerSubject() {
        assertThat(PlatformFundingAccountRole.CASH_MAPPING.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(PlatformFundingAccountRole.PREPAYMENT.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(PlatformFundingAccountRole.CLEARING.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(PlatformFundingAccountRole.SETTLEMENT.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(PlatformFundingAccountRole.FEE.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(PlatformFundingAccountRole.ADJUSTMENT.getLedgerProfileCode())
                .isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);

        assertThat(PlatformFundingAccountRole.CASH_MAPPING.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.CASH);
        assertThat(PlatformFundingAccountRole.PREPAYMENT.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.PREPAYMENT);
        assertThat(PlatformFundingAccountRole.CLEARING.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.CLEARING);
        assertThat(PlatformFundingAccountRole.SETTLEMENT.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.SETTLEMENT);
        assertThat(PlatformFundingAccountRole.FEE.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.FEE);
        assertThat(PlatformFundingAccountRole.ADJUSTMENT.getLedgerSubjectCode())
                .isEqualTo(LedgerSubjectCode.ADJUSTMENT);
    }
}
