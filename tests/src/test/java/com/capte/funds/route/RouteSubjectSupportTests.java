package com.capte.funds.route;

import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteSubjectSupportTests {

    private final RouteSubjectSupport support = new RouteSubjectSupport();

    @Test
    void testResolveLedgerProfileCodeShouldUseMerchantProfileForMerchantFundingAccountType() {
        FundsAccountId merchantAccount = FundsAccountId.immutable(
                "merchant_funding_001",
                DefaultFundsAccountType.PLATFORM_MERCHANT
        );

        assertThat(support.resolveSubjectType(merchantAccount)).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(support.resolveLedgerProfileCode(merchantAccount)).isEqualTo(LedgerProfileCode.FUNDING_MERCHANT);
        assertThat(support.isFundingAccount(merchantAccount)).isTrue();
    }
}
