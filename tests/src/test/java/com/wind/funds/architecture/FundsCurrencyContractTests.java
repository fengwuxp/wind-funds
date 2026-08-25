package com.wind.funds.architecture;

import com.wind.funds.governance.dal.entities.FundsTransactionProjection;
import com.wind.funds.governance.projection.FundsTransactionProjectionFact;
import com.wind.funds.governance.projection.FundsTransactionProjectionRow;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.reconciliation.dal.mapper.RecoveryOrderMapper;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

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

    /**
     * 场景：治理投影落库前使用内部 Entity 承载币种。
     * 预期：持久化模型保持 CurrencyIsoCode，不在 Entity 内退化为字符串。
     */
    @Test
    void testGovernanceProjectionPersistenceCurrencyShouldUseCurrencyIsoCode() throws NoSuchMethodException {
        assertThat(FundsTransactionProjection.class.getMethod("getCurrency").getReturnType())
                .isEqualTo(CurrencyIsoCode.class);
    }

    /**
     * 场景：追偿单按来源和币种查询既有事实。
     * 预期：两个 Mapper 查询入口都使用 CurrencyIsoCode 参数。
     */
    @Test
    void testRecoveryOrderLookupCurrencyShouldUseCurrencyIsoCode() {
        assertThat(Stream.of(RecoveryOrderMapper.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("selectBySource")
                        || method.getName().equals("selectBySourceForUpdate"))
                .map(method -> method.getParameterTypes()[5]))
                .containsOnly(CurrencyIsoCode.class);
    }
}
