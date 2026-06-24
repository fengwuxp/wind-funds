package com.wind.funds.wallet.application.instrument;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支付工具能力准入应用服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PaymentInstrumentCapabilityApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentInstrumentCapabilityApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String PAYMENT_INSTRUMENT_SN = "pi_capability_card";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_capability_receive";

    private static final String PAYMENT_BINDING_SN = "pi_capability_binding";

    private static final String OWNER_ID = "owner_pi_capability";

    private static final String SUBJECT_ID = "funding_pi_capability_subject";

    private static final String INSTRUMENT_TYPE_CARD = "CARD";

    private static final String CHANNEL_CODE = "capability_channel";

    private static final String EXTERNAL_INSTRUMENT_ID = "tok_capability_4242";

    private static final String MASKED_INSTRUMENT_NO = "****4242";

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private PaymentInstrumentCapabilityApplicationService capabilityApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testResolvePaymentInstrumentCapabilityShouldReturnActiveBindingSnapshotWithoutLedgerSideEffect() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentDirection.PAYMENT));
        Long bindingId = paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PaymentInstrumentCapabilityDecisionDTO decision =
                capabilityApplicationService.resolvePaymentInstrumentCapability(resolveRequest()
                        .setAction(PaymentInstrumentAction.AUTHORIZE));

        assertThat(decision)
                .satisfies(result -> {
                    assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(result.getInstrumentId()).isNotNull();
                    assertThat(result.getInstrumentSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
                    assertThat(result.getInstrumentNo()).isEqualTo(MASKED_INSTRUMENT_NO);
                    assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
                    assertThat(result.getOwnerType()).isEqualTo(FundsAccountOwnerType.USER);
                    assertThat(result.getInstrumentType()).isEqualTo(INSTRUMENT_TYPE_CARD);
                    assertThat(result.getInstrumentDirection()).isEqualTo(PaymentInstrumentDirection.PAYMENT);
                    assertThat(result.getChannelCode()).isEqualTo(CHANNEL_CODE);
                    assertThat(result.getAction()).isEqualTo(PaymentInstrumentAction.AUTHORIZE);
                    assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(result.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
                    assertThat(result.getBindingId()).isEqualTo(bindingId);
                    assertThat(result.getBindingSn()).isEqualTo(PAYMENT_BINDING_SN);
                    assertThat(result.getBindingRole()).isEqualTo(PaymentInstrumentBindingRole.PAYMENT_SUBJECT);
                    assertThat(result.getSubjectId()).isEqualTo(SUBJECT_ID);
                    assertThat(result.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
                    assertThat(result.getBindingVersion()).isEqualTo(1);
                    assertThat(result.getDefaultBinding()).isTrue();
                    assertThat(result.toString()).doesNotContain(MASKED_INSTRUMENT_NO, EXTERNAL_INSTRUMENT_ID);
                });
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolvePaymentInstrumentCapabilityShouldRejectDirectionMismatchWithoutLedgerSideEffect() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(RECEIVE_INSTRUMENT_SN,
                PaymentInstrumentDirection.RECEIVE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> capabilityApplicationService.resolvePaymentInstrumentCapability(resolveRequest()
                .setInstrumentSn(RECEIVE_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.PAY)))
                .hasMessageContaining("支付工具方向不支持当前动作");

        assertThat(countBindingHistoryRows()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testResolvePaymentInstrumentCapabilityShouldRejectStaleBindingVersionWithoutLedgerSideEffect() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentDirection.PAYMENT));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> capabilityApplicationService.resolvePaymentInstrumentCapability(resolveRequest()
                .setExpectedBindingVersion(0)))
                .hasMessageContaining("支付工具绑定版本已变更");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpPaymentInstrumentCapabilityTestData() {
        cleanupPaymentInstrumentCapabilityTestData();
    }

    @AfterEach
    void tearDownPaymentInstrumentCapabilityTestData() {
        cleanupPaymentInstrumentCapabilityTestData();
    }

    private void cleanupPaymentInstrumentCapabilityTestData() {
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RECEIVE_INSTRUMENT_SN);
    }

    private ResolvePaymentInstrumentCapabilityRequest resolveRequest() {
        return new ResolvePaymentInstrumentCapabilityRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.PAY)
                .setCurrency(CurrencyIsoCode.USD)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentDirection direction) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType(INSTRUMENT_TYPE_CARD)
                .setInstrumentDirection(direction)
                .setInstrumentNo(MASKED_INSTRUMENT_NO)
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId(EXTERNAL_INSTRUMENT_ID)
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(SUBJECT_ID)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private long countBindingHistoryRows() {
        return paymentInstrumentService.queryPaymentInstrumentBindingHistories(
                new PaymentInstrumentBindingHistoryQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(RECEIVE_INSTRUMENT_SN),
                DefaultPageQueryOptions.defaults(10)).getTotal();
    }

    @Configuration
    @Import({
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class
    })
    static class Config {
    }
}
