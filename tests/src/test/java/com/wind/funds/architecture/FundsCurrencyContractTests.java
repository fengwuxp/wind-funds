package com.wind.funds.architecture;

import com.wind.funds.governance.projection.FundsTransactionProjectionFact;
import com.wind.funds.governance.projection.FundsTransactionProjectionRow;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsCurrencyContractTests {

    @Test
    void testPublicCurrencyContractsShouldUseCurrencyIsoCode() throws NoSuchMethodException {
        assertThat(SubjectRef.class.getMethod("getCurrency").getReturnType()).isEqualTo(CurrencyIsoCode.class);
        assertThat(PaymentInstrumentRefSpec.class.getMethod("getCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(ExternalAccountRefSpec.class.getMethod("getCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(RouteParticipantSpec.class.getMethod("getCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(LedgerEntryQuery.class.getMethod("getCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(LedgerEntryQuery.class.getMethod("getOriginalCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(FundsTransactionProjectionFact.class.getMethod("currency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
        assertThat(FundsTransactionProjectionRow.class.getMethod("currency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
    }
}
