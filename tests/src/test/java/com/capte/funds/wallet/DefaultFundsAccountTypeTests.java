package com.capte.funds.wallet;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsAccountTypeTests {

    @Test
    void testIsExternalAccountShouldReturnTrueForExternalAccountType() {
        FundsAccountId accountId = FundsAccountId.immutable("external_bank_001",
                DefaultFundsAccountType.EXTERNAL_BANK);

        assertThat(DefaultFundsAccountType.isExternalAccount(accountId)).isTrue();
        assertThat(DefaultFundsAccountType.isExternalAccount(DefaultFundsAccountType.EXTERNAL_MERCHANT.name()))
                .isTrue();
    }

    @Test
    void testIsExternalAccountShouldReturnFalseForFundsSubjectType() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT);

        assertThat(DefaultFundsAccountType.isExternalAccount(accountId)).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(FundsSubjectType.FUNDING_ACCOUNT.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(FundsSubjectType.BUDGET_GROUP.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(RouteNodeType.SUBJECT.name())).isFalse();
        assertThat(DefaultFundsAccountType.isExternalAccount(RouteNodeType.PAYMENT_INSTRUMENT.name())).isFalse();
    }

    @Test
    void testIsExternalAccountShouldExposeUnknownAccountType() {
        assertThatThrownBy(() -> DefaultFundsAccountType.isExternalAccount("UNKNOWN_ACCOUNT_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN_ACCOUNT_TYPE");
    }
}
