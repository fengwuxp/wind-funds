package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.capte.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.capte.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.capte.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.capte.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.capte.funds.wallet.model.query.PaymentInstrumentQuery;
import com.capte.funds.wallet.model.request.ChangePaymentInstrumentBindingRequest;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.capte.funds.wallet.service.PaymentInstrumentService;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentDirection;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支付工具和绑定关系服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PaymentInstrumentServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentInstrumentServiceImplTests extends AbstractFundsServiceTest {

    private static final String PAYMENT_INSTRUMENT_SN = "pi_service_masked_card";

    private static final String RAW_PAYMENT_INSTRUMENT_SN = "pi_service_raw_card";

    private static final String SUSPENDED_PAYMENT_INSTRUMENT_SN = "pi_service_suspended_card";

    private static final String RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN = "pi_service_receive_card";

    private static final String BINDING_SN = "pi_binding_service_candidate";

    private static final String OWNER_ID = "owner_pi_service";

    private static final String FUNDING_ACCOUNT_ID = "funding_pi_binding_target";

    private static final String OPERATOR_ID = "ops_pi_service";

    private static final String CREATE_BINDING_REQUEST_SN = "req_pi_binding_create";

    private static final String CHANGE_BINDING_REQUEST_SN = "req_pi_binding_change";

    private static final String INSTRUMENT_TYPE_CARD = "CARD";

    private static final String CHANNEL_CODE = "card_test_channel";

    private static final String EXTERNAL_INSTRUMENT_ID = "tok_card_4242";

    private static final String MASKED_INSTRUMENT_NO = "****4242";

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreatePaymentInstrumentShouldPersistMaskedReferenceWithoutLedgerSubject() {
        LedgerFacts before = loadLedgerFacts();

        Long instrumentId = paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());

        PaymentInstrumentDTO instrument = paymentInstrumentService.getPaymentInstrumentById(instrumentId);
        List<PaymentInstrumentDTO> records = paymentInstrumentService.queryPaymentInstruments(
                new PaymentInstrumentQuery()
                        .setTenantId(TENANT_ID)
                        .setOwnerId(OWNER_ID)
                        .setInstrumentDirection(PaymentInstrumentDirection.PAYMENT)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(instrument.getSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(instrument.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instrument.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(instrument.getOwnerType()).isEqualTo(FundsAccountOwnerType.USER);
        assertThat(instrument.getInstrumentType()).isEqualTo(INSTRUMENT_TYPE_CARD);
        assertThat(instrument.getInstrumentDirection()).isEqualTo(PaymentInstrumentDirection.PAYMENT);
        assertThat(instrument.getInstrumentNo()).isEqualTo(MASKED_INSTRUMENT_NO);
        assertThat(instrument.getChannelCode()).isEqualTo(CHANNEL_CODE);
        assertThat(instrument.getExternalInstrumentId()).isEqualTo(EXTERNAL_INSTRUMENT_ID);
        assertThat(instrument.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(instrument.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(records).extracting(PaymentInstrumentDTO::getSn).containsExactly(PAYMENT_INSTRUMENT_SN);
        assertThat(countRows("t_ledger", "subject_id", PAYMENT_INSTRUMENT_SN)).isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testCreatePaymentInstrumentShouldRejectRawSensitiveInstrumentNo() {
        LedgerFacts before = loadLedgerFacts();

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(
                createPaymentInstrumentRequest()
                        .setSn(RAW_PAYMENT_INSTRUMENT_SN)
                        .setInstrumentNo("4242424242424242")))
                .hasMessageContaining("instrumentNo must be masked or token reference");

        assertThat(countRows("t_payment_instrument", "sn", RAW_PAYMENT_INSTRUMENT_SN)).isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldPersistCandidateWithoutLedgerMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFacts before = loadLedgerFacts();

        Long bindingId = paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .setDefaultBinding(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(bindingId).isPositive();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst())
                .satisfies(binding -> {
                    assertThat(binding.getSn()).isEqualTo(BINDING_SN);
                    assertThat(binding.getInstrumentSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
                    assertThat(binding.getBindingRole()).isEqualTo(PaymentInstrumentBindingRole.FUNDING_SUBJECT);
                    assertThat(binding.getSubjectId()).isEqualTo(FUNDING_ACCOUNT_ID);
                    assertThat(binding.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
                    assertThat(binding.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
                    assertThat(binding.getPriority()).isEqualTo(10);
                    assertThat(binding.getDefaultBinding()).isTrue();
                    assertThat(binding.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
                    assertThat(binding.getVersion()).isEqualTo(1);
                });
        assertThat(countRows("t_ledger", "subject_id", BINDING_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", PAYMENT_INSTRUMENT_SN)).isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectUnavailableInstrumentWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SUSPENDED_PAYMENT_INSTRUMENT_SN)
                .setStatus(FundsAccountStatus.SUSPENDED));
        LedgerFacts before = loadLedgerFacts();

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setInstrumentSn(SUSPENDED_PAYMENT_INSTRUMENT_SN)))
                .hasMessageContaining("支付工具不可用于绑定");

        assertThat(countRows("t_payment_instrument_binding", "instrument_sn", SUSPENDED_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "instrument_sn", SUSPENDED_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectDirectionMismatchWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN)
                .setInstrumentDirection(PaymentInstrumentDirection.RECEIVE));
        LedgerFacts before = loadLedgerFacts();

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setInstrumentSn(RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN)))
                .hasMessageContaining("支付工具方向不支持绑定角色");

        assertThat(countRows("t_payment_instrument_binding", "instrument_sn", RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "instrument_sn", RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectCurrencyMismatchWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFacts before = loadLedgerFacts();

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setCurrency(CurrencyIsoCode.CNY)))
                .hasMessageContaining("支付工具币种与绑定币种不一致");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFacts(before);
    }

    @Test
    void testChangePaymentInstrumentBindingShouldAppendHistoryWithoutOverwritingEvidence() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFacts before = loadLedgerFacts();

        Long changedBindingId = paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setDefaultBinding(Boolean.FALSE)
                        .setStatus(FundsAccountStatus.SUSPENDED)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                        .setContextVariables("{\"ticket\":\"PI-007\"}"));

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        List<PaymentInstrumentBindingHistoryDTO> histories = new ArrayList<>(paymentInstrumentService
                .queryPaymentInstrumentBindingHistories(new PaymentInstrumentBindingHistoryQuery()
                                .setTenantId(TENANT_ID)
                                .setBindingSn(BINDING_SN),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords());
        histories.sort(Comparator.comparing(PaymentInstrumentBindingHistoryDTO::getVersion));

        assertThat(changedBindingId).isEqualTo(binding.getId());
        assertThat(binding.getPriority()).isEqualTo(20);
        assertThat(binding.getDefaultBinding()).isFalse();
        assertThat(binding.getStatus()).isEqualTo(FundsAccountStatus.SUSPENDED);
        assertThat(binding.getVersion()).isEqualTo(2);
        assertThat(histories)
                .extracting(PaymentInstrumentBindingHistoryDTO::getChangeType)
                .containsExactly(PaymentInstrumentBindingChangeType.CREATE, PaymentInstrumentBindingChangeType.UPDATE);
        assertThat(histories)
                .extracting(PaymentInstrumentBindingHistoryDTO::getVersion)
                .containsExactly(1, 2);
        assertThat(histories.getFirst())
                .satisfies(history -> {
                    assertThat(history.getBeforeSnapshot()).isNull();
                    assertThat(history.getAfterSnapshot()).contains("\"priority\":10");
                    assertThat(history.getOperatorId()).isEqualTo(OPERATOR_ID);
                    assertThat(history.getChangeReason()).isEqualTo("bind funding account");
                    assertThat(history.getRequestSn()).isEqualTo(CREATE_BINDING_REQUEST_SN);
                });
        assertThat(histories.get(1))
                .satisfies(history -> {
                    assertThat(history.getBeforeSnapshot()).contains("\"priority\":10");
                    assertThat(history.getBeforeSnapshot()).contains("\"version\":1");
                    assertThat(history.getAfterSnapshot()).contains("\"priority\":20");
                    assertThat(history.getAfterSnapshot()).contains("\"defaultBinding\":false");
                    assertThat(history.getAfterSnapshot()).contains("\"status\":\"SUSPENDED\"");
                    assertThat(history.getAfterSnapshot()).contains("\"version\":2");
                    assertThat(history.getOperatorId()).isEqualTo(OPERATOR_ID);
                    assertThat(history.getChangeReason()).isEqualTo("risk review");
                    assertThat(history.getRequestSn()).isEqualTo(CHANGE_BINDING_REQUEST_SN);
                });
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isEqualTo(2);
        assertLedgerFacts(before);
    }

    @BeforeEach
    void setUpPaymentInstrumentTestData() {
        cleanupPaymentInstrumentTestData();
    }

    @AfterEach
    void tearDownPaymentInstrumentTestData() {
        cleanupPaymentInstrumentTestData();
    }

    private void cleanupPaymentInstrumentTestData() {
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE binding_sn = ?", BINDING_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE sn = ?", BINDING_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?, ?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RAW_PAYMENT_INSTRUMENT_SN,
                SUSPENDED_PAYMENT_INSTRUMENT_SN,
                RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest() {
        return new CreatePaymentInstrumentRequest()
                .setSn(PAYMENT_INSTRUMENT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType(INSTRUMENT_TYPE_CARD)
                .setInstrumentDirection(PaymentInstrumentDirection.PAYMENT)
                .setInstrumentNo(MASKED_INSTRUMENT_NO)
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId(EXTERNAL_INSTRUMENT_ID)
                .setCurrency(CurrencyIsoCode.USD);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(BINDING_SN)
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                .setSubjectId(FUNDING_ACCOUNT_ID)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE)
                .setOperatorId(OPERATOR_ID)
                .setChangeReason("bind funding account")
                .setRequestSn(CREATE_BINDING_REQUEST_SN);
    }

    private LedgerFacts loadLedgerFacts() {
        return new LedgerFacts(
                countRows("t_ledger"),
                countRows("t_ledger_transaction"),
                countRows("t_ledger_posting_plan"),
                countRows("t_ledger_entry"));
    }

    private void assertLedgerFacts(LedgerFacts expected) {
        assertThat(loadLedgerFacts()).isEqualTo(expected);
    }

    private long countRows(String tableName) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return result;
    }

    private long countRows(String tableName, String columnName, String value) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value);
        return result;
    }

    private record LedgerFacts(long ledgers, long transactions, long postingPlans, long entries) {
    }

    @Configuration
    @Import(PaymentInstrumentServiceImpl.class)
    static class Config {
    }
}
