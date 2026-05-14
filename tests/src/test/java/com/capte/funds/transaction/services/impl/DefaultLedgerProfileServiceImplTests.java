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
        assertThat(service.getRequiredItem(LedgerProfileCode.FUNDING_MERCHANT, LedgerSubjectCode.AVAILABLE)
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
