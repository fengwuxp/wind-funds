package com.capte.funds.ledger;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.support.LedgerNormalBalanceUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerNormalBalanceUtilsTests {

    @Test
    void computeNormalBalanceShouldUseDebitMinusCreditForDebitNormalAccount() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.DEBIT,
                1_200L,
                200L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }

    @Test
    void computeNormalBalanceShouldUseCreditMinusDebitForCreditNormalAccount() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.CREDIT,
                200L,
                1_200L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }

    @Test
    void computeNormalBalanceShouldReturnNullWhenNormalSideUndefined() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                null,
                100L,
                50L
        );

        assertThat(normalBalance).isNull();
    }

    @Test
    void computeNormalBalanceShouldUseDebitNormalForLimitSubject() {
        Long normalBalance = LedgerNormalBalanceUtils.computeNormalBalance(
                EntrySide.DEBIT,
                1_000L,
                0L
        );

        assertThat(normalBalance).isEqualTo(1_000L);
    }
}
