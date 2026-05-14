package com.capte.funds.transaction.services.impl;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.model.dto.LedgerProfileDTO;
import com.capte.funds.transaction.model.dto.LedgerProfileItemDTO;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerProfileServiceImplTests {

    private final DefaultLedgerProfileServiceImpl service = new DefaultLedgerProfileServiceImpl();

    @Test
    void getProfileShouldExposeFundingAccountRequiredSubjects() {
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

    @Test
    void getRequiredItemShouldExposeCreditLimitAndAvailableRules() {
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

    @Test
    void availableBucketShouldAllowControlledNegativeForFundingCreditAndBudgetSubjects() {
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.CREDIT_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
        assertThat(service.getRequiredItem(LedgerProfileCode.BUDGET_BASIC, LedgerSubjectCode.AVAILABLE)
                .getAllowNegative()).isTrue();
    }

    @Test
    void getRequiredItemShouldRejectMissingSubject() {
        assertThatThrownBy(() -> service.getRequiredItem(
                LedgerProfileCode.CREDIT_BASIC,
                LedgerSubjectCode.PREPAYMENT
        )).isInstanceOf(RuntimeException.class);
    }
}
