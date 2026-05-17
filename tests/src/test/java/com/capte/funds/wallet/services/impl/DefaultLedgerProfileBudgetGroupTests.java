package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLedgerProfileBudgetGroupTests {

    private final DefaultLedgerProfileServiceImpl service = new DefaultLedgerProfileServiceImpl();

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
