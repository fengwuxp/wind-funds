package com.wind.funds.wallet.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.dal.entities.PaymentInstrument;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentQuery;
import com.wind.funds.wallet.model.request.ChangePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

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

    private static final String SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN = "pi_service_sensitive_context_card";

    private static final String INVALID_WINDOW_PAYMENT_INSTRUMENT_SN = "pi_service_invalid_window_card";

    private static final String SUSPENDED_PAYMENT_INSTRUMENT_SN = "pi_service_suspended_card";

    private static final String RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN = "pi_service_receive_card";

    private static final String BINDING_SN = "pi_binding_service_candidate";

    private static final String DUPLICATE_DEFAULT_BINDING_SN = "pi_binding_service_default_duplicate";

    private static final String PRIORITY_CONFLICT_BINDING_SN = "pi_binding_service_priority_conflict";

    private static final String PRIORITY_ORDER_BINDING_SN = "pi_binding_service_priority_order";

    private static final String CONCURRENT_DEFAULT_BINDING_SN = "pi_binding_service_default_concurrent";

    private static final String OWNER_ID = "owner_pi_service";

    private static final String FUNDING_ACCOUNT_ID = "funding_pi_binding_target";

    private static final String LONG_FUNDING_ACCOUNT_ID =
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    private static final String SECOND_FUNDING_ACCOUNT_ID = "funding_pi_binding_second";

    private static final String THIRD_FUNDING_ACCOUNT_ID = "funding_pi_binding_third";

    private static final String OPERATOR_ID = "ops_pi_service";

    private static final String CREATE_BINDING_REQUEST_SN = "req_pi_binding_create";

    private static final String CHANGE_BINDING_REQUEST_SN = "req_pi_binding_change";

    private static final String LONG_SUBJECT_BINDING_SN = "pi_binding_service_long_subject";

    private static final String LONG_SUBJECT_BINDING_REQUEST_SN = "req_pi_binding_long_subject";

    private static final String DUPLICATE_DEFAULT_CREATE_REQUEST_SN = "req_pi_binding_duplicate_default_create";

    private static final String DUPLICATE_DEFAULT_CHANGE_REQUEST_SN = "req_pi_binding_duplicate_default_change";

    private static final String PRIORITY_CONFLICT_CREATE_REQUEST_SN = "req_pi_binding_priority_conflict_create";

    private static final String PRIORITY_CONFLICT_CHANGE_REQUEST_SN = "req_pi_binding_priority_conflict_change";

    private static final String PRIORITY_ORDER_CREATE_REQUEST_SN = "req_pi_binding_priority_order_create";

    private static final String CONCURRENT_DEFAULT_CREATE_REQUEST_SN = "req_pi_binding_default_concurrent_create";

    private static final String INSTRUMENT_TYPE_CARD = "CARD";

    private static final String CHANNEL_CODE = "card_test_channel";

    private static final String EXTERNAL_INSTRUMENT_ID = "tok_card_4242";

    private static final String MASKED_INSTRUMENT_NO = "****4242";

    private static final String RAW_PAN_INSTRUMENT_NO = "4242424242424242";

    private static final String RAW_PAN_CONTEXT_VARIABLES =
            "{\"processorPayload\":{\"networkReference\":\"" + RAW_PAN_INSTRUMENT_NO + "\"}}";

    private static final String RAW_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{\"processorPayload\":{\"bankAccountNo\":\"123456789012\"}}";

    private static final String UNQUOTED_SENSITIVE_CONTEXT_VARIABLES =
            "{processorPayload:{secretKey:\"secret-value\"";

    private static final String UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{externalAccount:{bankAccountNo:\"123456789012\"";

    private static final String UNQUOTED_CORE_BENEFIT_CONTEXT_VARIABLES =
            "{benefitPayload:{currentMarketingRule:\"RULE-PI-001\"";

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreatePaymentInstrumentShouldPersistMaskedReferenceWithoutLedgerSubject() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

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
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentShouldRejectRawSensitiveInstrumentNo() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(
                createPaymentInstrumentRequest()
                        .setSn(RAW_PAYMENT_INSTRUMENT_SN)
                        .setInstrumentNo(RAW_PAN_INSTRUMENT_NO)))
                .hasMessageContaining("instrumentNo must be masked or token reference");

        assertThat(countRows("t_payment_instrument", "sn", RAW_PAYMENT_INSTRUMENT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具时把通道 token secret 放入扩展上下文。
     * 输入：contextVariables 中包含嵌套 secretKey 字段、伪装字段名中的 PAN 值、外部账户原文字段或坏 JSON 敏感字段名。
     * 输出：创建被拒绝，不留下支付工具引用或账本事实。
     * 红线：token secret、密钥和 CVV 不得进入支付工具普通快照、日志、导出或报表。
     */
    @Test
    void testCreatePaymentInstrumentShouldRejectSensitiveContextVariables() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN)
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_pan_value")
                .setContextVariables(RAW_PAN_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_external_account")
                .setContextVariables(RAW_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_unquoted")
                .setContextVariables(UNQUOTED_SENSITIVE_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_unquoted_external_account")
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");

        assertThat(countRows("t_payment_instrument", "sn", SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN)).isZero();
        assertThat(countRows("t_payment_instrument", "sn", SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_pan_value"))
                .isZero();
        assertThat(countRows("t_payment_instrument", "sn", SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN
                + "_external_account")).isZero();
        assertThat(countRows("t_payment_instrument", "sn", SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_unquoted"))
                .isZero();
        assertThat(countRows("t_payment_instrument", "sn", SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN
                + "_unquoted_external_account")).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具时把权益实时规则或资金责任放入扩展上下文。
     * 输入：contextVariables 含 fundingNature 或当前营销规则，包含标准 JSON 和坏 JSON 两类输入。
     * 输出：创建被拒绝，不留下支付工具引用或账本事实。
     * 红线：支付工具管理对象不得成为权益核心事实的旁路存储。
     */
    @Test
    void testCreatePaymentInstrumentShouldRejectCoreBenefitContextVariables() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_benefit")
                .setContextVariables("{\"benefitPayload\":{\"fundingNature\":\"COUPON\"}}")))
                .hasMessageContaining("paymentInstrument.contextVariables must not contain core benefit field: "
                        + "fundingNature");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_benefit_unquoted")
                .setContextVariables(UNQUOTED_CORE_BENEFIT_CONTEXT_VARIABLES)))
                .hasMessageContaining("paymentInstrument.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        assertThat(countRows("t_payment_instrument", "sn",
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_benefit")).isZero();
        assertThat(countRows("t_payment_instrument", "sn",
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_benefit_unquoted")).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具时配置了倒置或空的生效窗口。
     * 输入：validTo 早于或等于 validFrom 的 ACTIVE 支付工具。
     * 输出：创建被拒绝，不留下支付工具引用。
     * 红线：无效工具窗口不得进入绑定或 route 候选池，也不得写账。
     */
    @Test
    void testCreatePaymentInstrumentShouldRejectInvalidValidityWindowWithoutInstrument() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(INVALID_WINDOW_PAYMENT_INSTRUMENT_SN)
                .setValidFrom(now)
                .setValidTo(now.minusSeconds(1))))
                .hasMessageContaining("支付工具生效时间必须早于失效时间");

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(INVALID_WINDOW_PAYMENT_INSTRUMENT_SN)
                .setValidFrom(now)
                .setValidTo(now)))
                .hasMessageContaining("支付工具生效时间必须早于失效时间");

        assertThat(countRows("t_payment_instrument", "sn", INVALID_WINDOW_PAYMENT_INSTRUMENT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testPaymentInstrumentModelsShouldHideSensitiveIdentifiersInToString() {
        PaymentInstrument entity = new PaymentInstrument();
        entity.setInstrumentNo(MASKED_INSTRUMENT_NO);
        entity.setExternalInstrumentId(EXTERNAL_INSTRUMENT_ID);

        PaymentInstrumentDTO dto = new PaymentInstrumentDTO()
                .setInstrumentNo(MASKED_INSTRUMENT_NO)
                .setExternalInstrumentId(EXTERNAL_INSTRUMENT_ID);
        PaymentInstrumentQuery query = new PaymentInstrumentQuery()
                .setInstrumentNo(MASKED_INSTRUMENT_NO)
                .setExternalInstrumentId(EXTERNAL_INSTRUMENT_ID);

        assertPaymentInstrumentToStringDoesNotExposeSensitiveIdentifiers(entity);
        assertPaymentInstrumentToStringDoesNotExposeSensitiveIdentifiers(dto);
        assertPaymentInstrumentToStringDoesNotExposeSensitiveIdentifiers(query);
        assertPaymentInstrumentToStringDoesNotExposeSensitiveIdentifiers(createPaymentInstrumentRequest());
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldPersistCandidateWithoutLedgerMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

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
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldSupportSixtyFourCharSubjectId() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setSn(LONG_SUBJECT_BINDING_SN)
                .setSubjectId(LONG_FUNDING_ACCOUNT_ID)
                .setRequestSn(LONG_SUBJECT_BINDING_REQUEST_SN));

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(LONG_SUBJECT_BINDING_SN)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .setSubjectId(LONG_FUNDING_ACCOUNT_ID),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getSubjectId()).isEqualTo(LONG_FUNDING_ACCOUNT_ID);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldReturnExistingWhenRequestSnReplayed() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long firstBindingId = paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        Long replayedBindingId = paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());

        assertThat(replayedBindingId).isEqualTo(firstBindingId);
        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isOne();
        assertThat(countRows("t_payment_instrument_binding_history", "request_sn", CREATE_BINDING_REQUEST_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具绑定时缺少幂等流水。
     * 输入：requestSn 为空的绑定创建请求。
     * 输出：创建被拒绝，不留下绑定当前态或历史证据。
     * 红线：支付工具绑定创建必须可幂等、可回放、可对账追踪，缺流水时不得写入任何资金事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectMissingRequestSnWithoutBindingMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setRequestSn("   ")))
                .hasMessageContaining("支付工具绑定创建 requestSn 不能为空");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectUnavailableInstrumentWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(SUSPENDED_PAYMENT_INSTRUMENT_SN)
                .setStatus(FundsAccountStatus.SUSPENDED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setInstrumentSn(SUSPENDED_PAYMENT_INSTRUMENT_SN)))
                .hasMessageContaining("支付工具不可用于绑定");

        assertThat(countRows("t_payment_instrument_binding", "instrument_sn", SUSPENDED_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "instrument_sn", SUSPENDED_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectDirectionMismatchWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setSn(RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN)
                .setInstrumentDirection(PaymentInstrumentDirection.RECEIVE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setInstrumentSn(RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN)))
                .hasMessageContaining("支付工具方向不支持绑定角色");

        assertThat(countRows("t_payment_instrument_binding", "instrument_sn", RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "instrument_sn", RECEIVE_ONLY_PAYMENT_INSTRUMENT_SN))
                .isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营把支付工具的信用控制主体绑定误配置为资金账户。
     * 输入：bindingRole = CREDIT_SUBJECT，但 subjectType 不是 CREDIT_ACCOUNT。
     * 输出：创建被拒绝，不留下绑定候选或历史。
     * 红线：信用控制主体只能指向信用账户，不能用资金账户替代额度责任主体。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectNonCreditAccountAsCreditSubject() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setBindingRole(PaymentInstrumentBindingRole.CREDIT_SUBJECT)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)))
                .hasMessageContaining("信用控制主体绑定必须指向信用账户");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营尝试用支付工具绑定维护预算控制范围。
     * 输入：bindingRole = BUDGET_SUBJECT。
     * 输出：创建被拒绝，不留下绑定候选或历史。
     * 红线：预算组是 Spend Rule / Spend Control 的控制范围对象，不通过支付工具资金主体绑定维护。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectBudgetSubjectBinding() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setBindingRole(PaymentInstrumentBindingRole.BUDGET_SUBJECT)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)))
                .hasMessageContaining("预算控制范围不通过支付工具资金主体绑定维护");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setBindingRole(PaymentInstrumentBindingRole.BUDGET_SUBJECT)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)))
                .hasMessageContaining("预算控制范围不通过支付工具资金主体绑定维护");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreatePaymentInstrumentBindingShouldRejectCurrencyMismatchWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setCurrency(CurrencyIsoCode.CNY)))
                .hasMessageContaining("支付工具币种与绑定币种不一致");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具绑定时把通道 token secret 放入扩展上下文。
     * 输入：contextVariables 中包含嵌套 secretKey 字段、伪装字段名中的 PAN 值、外部账户原文字段或坏 JSON 敏感字段名。
     * 输出：创建被拒绝，不留下绑定候选或历史。
     * 红线：token secret、密钥和 CVV 不得进入支付工具绑定当前态、历史、日志、导出或报表。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectSensitiveContextVariablesWithoutRouteCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setContextVariables(RAW_PAN_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setContextVariables(RAW_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建支付工具绑定时配置了倒置或空的生效窗口。
     * 输入：validTo 早于或等于 validFrom 的 ACTIVE 绑定关系。
     * 输出：创建被拒绝，不留下绑定候选或历史。
     * 红线：无效绑定窗口不得进入 route 候选池，也不得写账或污染审计证据。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectInvalidValidityWindowWithoutBinding() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setValidFrom(now)
                .setValidTo(now.minusSeconds(1))))
                .hasMessageContaining("支付工具绑定生效时间必须早于失效时间");

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setValidFrom(now)
                .setValidTo(now)))
                .hasMessageContaining("支付工具绑定生效时间必须早于失效时间");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支付工具、绑定角色和币种已经存在 ACTIVE 默认绑定后，再创建第二个默认绑定。
     * 输入：同一工具下两个不同资金主体绑定都声明 defaultBinding = true。
     * 输出：第二个绑定被拒绝，默认候选仍只有原绑定。
     * 红线：默认候选不唯一时不得给后续 route 留下随机选路空间，不写绑定当前态、历史或账本事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectDuplicateActiveDefaultCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(createSecondBindingRequest()
                .setDefaultBinding(Boolean.TRUE)))
                .hasMessageContaining("默认支付工具绑定不唯一");

        List<PaymentInstrumentBindingDTO> defaults = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setDefaultBinding(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        assertThat(defaults)
                .singleElement()
                .satisfies(binding -> {
                    assertThat(binding.getSn()).isEqualTo(BINDING_SN);
                    assertThat(binding.getSubjectId()).isEqualTo(FUNDING_ACCOUNT_ID);
                });
        assertThat(countRows("t_payment_instrument_binding", "sn", DUPLICATE_DEFAULT_BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", DUPLICATE_DEFAULT_BINDING_SN))
                .isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营提前配置未来生效的默认支付工具绑定，用于当前默认绑定到期后的计划切换。
     * 输入：两个 ACTIVE 默认绑定的 validFrom/validTo 首尾相接，不存在同一时刻重叠。
     * 输出：未来默认绑定创建成功；当前 route 候选仍只返回当前有效默认绑定。
     * 红线：默认候选唯一性只限制同一时刻的有效候选，不得阻断计划内换绑，也不得写账。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldAllowNonOverlappingDefaultCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime switchAt = now.plusHours(1);
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setValidFrom(now.minusDays(1))
                .setValidTo(switchAt));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long futureBindingId = paymentInstrumentService.createPaymentInstrumentBinding(createSecondBindingRequest()
                .setDefaultBinding(Boolean.TRUE)
                .setValidFrom(switchAt)
                .setValidTo(now.plusDays(1)));

        List<PaymentInstrumentBindingDTO> currentDefaults = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setDefaultBinding(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        PaymentInstrumentBindingDTO futureBinding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(DUPLICATE_DEFAULT_BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();

        assertThat(futureBindingId).isPositive();
        assertThat(currentDefaults)
                .extracting(PaymentInstrumentBindingDTO::getSn)
                .containsExactly(BINDING_SN);
        assertThat(futureBinding.getDefaultBinding()).isTrue();
        assertThat(futureBinding.getValidFrom()).isAfter(now);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", DUPLICATE_DEFAULT_BINDING_SN))
                .isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：两个外部请求并发把同一支付工具、绑定角色和币种创建为当前 ACTIVE 默认绑定。
     * 输入：两个不同绑定 SN、不同主体和不同 requestSn 同时提交，且绑定窗口重叠。
     * 输出：只有一个默认绑定创建成功，另一个被默认唯一性拒绝；默认和优先级 guard scope 各生成一行。
     * 红线：并发下不得同时留下两个当前默认候选，也不得写入账本事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldSerializeConcurrentDefaultCandidates() throws Exception {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BindingAttemptResult> first = executor.submit(concurrentBindingAttempt(startGate,
                    createBindingRequest()));
            Future<BindingAttemptResult> second = executor.submit(concurrentBindingAttempt(startGate,
                    createSecondBindingRequest()
                            .setSn(CONCURRENT_DEFAULT_BINDING_SN)
                            .setSubjectId(THIRD_FUNDING_ACCOUNT_ID)
                            .setRequestSn(CONCURRENT_DEFAULT_CREATE_REQUEST_SN)
                            .setDefaultBinding(Boolean.TRUE)));

            startGate.countDown();

            List<BindingAttemptResult> results = List.of(first.get(), second.get());
            List<PaymentInstrumentBindingDTO> defaults = paymentInstrumentService.queryPaymentInstrumentBindings(
                    new PaymentInstrumentBindingQuery()
                            .setTenantId(TENANT_ID)
                            .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                            .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                            .setCurrency(CurrencyIsoCode.USD)
                            .setDefaultBinding(Boolean.TRUE)
                            .setStatus(FundsAccountStatus.ACTIVE),
                    DefaultPageQueryOptions.defaults(10)).getRecords();

            assertThat(results).filteredOn(BindingAttemptResult::succeeded).hasSize(1);
            assertThat(results).filteredOn(result -> !result.succeeded())
                    .singleElement()
                    .satisfies(result -> assertThat(result.message()).contains("默认支付工具绑定不唯一"));
            assertThat(defaults).singleElement();
            assertThat(countRows("t_payment_instrument_binding_guard", "instrument_sn", PAYMENT_INSTRUMENT_SN))
                    .isEqualTo(2);
            assertLedgerFactsUnchanged(jdbcTemplate, before);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：两个外部请求并发把同一支付工具、绑定角色和币种创建为当前 ACTIVE 同优先级非默认绑定。
     * 输入：两个不同绑定 SN、不同主体和不同 requestSn 同时提交，且绑定窗口和 priority 重叠。
     * 输出：只有一个绑定创建成功，另一个被优先级唯一性拒绝；DB guard scope 只生成一行。
     * 红线：并发下不得同时留下两个当前同优先级候选，也不得写入账本事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldSerializeConcurrentPriorityCandidates() throws Exception {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BindingAttemptResult> first = executor.submit(concurrentBindingAttempt(startGate,
                    createBindingRequest().setDefaultBinding(Boolean.FALSE)));
            Future<BindingAttemptResult> second = executor.submit(concurrentBindingAttempt(startGate,
                    createPriorityConflictBindingRequest()));

            startGate.countDown();

            List<BindingAttemptResult> results = List.of(first.get(), second.get());
            List<PaymentInstrumentBindingDTO> bindings = paymentInstrumentService.queryPaymentInstrumentBindings(
                    new PaymentInstrumentBindingQuery()
                            .setTenantId(TENANT_ID)
                            .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                            .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                            .setCurrency(CurrencyIsoCode.USD)
                            .setDefaultBinding(Boolean.FALSE)
                            .setStatus(FundsAccountStatus.ACTIVE),
                    DefaultPageQueryOptions.defaults(10)).getRecords();

            assertThat(results).filteredOn(BindingAttemptResult::succeeded).hasSize(1);
            assertThat(results).filteredOn(result -> !result.succeeded())
                    .singleElement()
                    .satisfies(result -> assertThat(result.message()).contains("支付工具绑定优先级冲突"));
            assertThat(bindings).singleElement();
            assertThat(countRows("t_payment_instrument_binding_guard", "instrument_sn", PAYMENT_INSTRUMENT_SN))
                    .isOne();
            assertLedgerFactsUnchanged(jdbcTemplate, before);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 场景：同一支付工具存在一个 ACTIVE 默认绑定和一个非默认绑定，运营尝试把非默认绑定改为默认。
     * 输入：第二个绑定仅变更 defaultBinding = true，其他绑定维度与原默认绑定相同。
     * 输出：变更被拒绝，第二个绑定仍保持非默认和原版本。
     * 红线：当前态变更入口不得绕过默认候选唯一性，也不得追加伪成功的绑定历史。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectDuplicateActiveDefaultCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createSecondBindingRequest()
                .setDefaultBinding(Boolean.FALSE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(DUPLICATE_DEFAULT_BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setDefaultBinding(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("promote duplicate default")
                        .setRequestSn(DUPLICATE_DEFAULT_CHANGE_REQUEST_SN)))
                .hasMessageContaining("默认支付工具绑定不唯一");

        PaymentInstrumentBindingDTO secondBinding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(DUPLICATE_DEFAULT_BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(secondBinding.getDefaultBinding()).isFalse();
        assertThat(secondBinding.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(secondBinding.getVersion()).isEqualTo(1);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", DUPLICATE_DEFAULT_BINDING_SN))
                .isEqualTo(1);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支付工具、绑定角色和币种已经存在 ACTIVE 候选优先级后，再创建同优先级绑定。
     * 输入：两个不同资金主体绑定都处于 ACTIVE，且 priority 相同。
     * 输出：第二个绑定被拒绝，原候选保持唯一可排序。
     * 红线：支付工具绑定优先级冲突时不得给后续 route 留下随机选路空间，不写绑定当前态、历史或账本事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldRejectDuplicateActivePriorityCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setDefaultBinding(Boolean.FALSE));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.createPaymentInstrumentBinding(
                createPriorityConflictBindingRequest()))
                .hasMessageContaining("支付工具绑定优先级冲突");

        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isOne();
        assertThat(countRows("t_payment_instrument_binding", "sn", PRIORITY_CONFLICT_BINDING_SN)).isZero();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", PRIORITY_CONFLICT_BINDING_SN))
                .isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支付工具下已有两个 ACTIVE 非默认绑定，运营把第二个绑定改成已存在的优先级。
     * 输入：第二个绑定仅变更 priority = 10，其他绑定维度与已有候选相同。
     * 输出：变更被拒绝，第二个绑定保持原优先级和原版本。
     * 红线：当前态变更入口不得绕过绑定优先级唯一性，也不得追加伪成功的绑定历史。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectDuplicateActivePriorityCandidate() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setDefaultBinding(Boolean.FALSE));
        paymentInstrumentService.createPaymentInstrumentBinding(createPriorityConflictBindingRequest()
                .setPriority(20));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(PRIORITY_CONFLICT_BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(10)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("raise priority")
                        .setRequestSn(PRIORITY_CONFLICT_CHANGE_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定优先级冲突");

        PaymentInstrumentBindingDTO secondBinding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(PRIORITY_CONFLICT_BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(secondBinding.getPriority()).isEqualTo(20);
        assertThat(secondBinding.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(secondBinding.getVersion()).isEqualTo(1);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", PRIORITY_CONFLICT_BINDING_SN))
                .isEqualTo(1);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支付工具下存在多个 ACTIVE 非默认候选，后续 route 需要按确定顺序读取候选。
     * 输入：两个候选优先级分别为 20 和 10，创建顺序与优先级顺序相反。
     * 输出：查询结果按 priority 升序返回，再由稳定主键兜底。
     * 红线：候选查询不得依赖数据库自然顺序，避免后续 route 出现隐式随机选路。
     */
    @Test
    void testQueryPaymentInstrumentBindingsShouldReturnActiveCandidatesByPriority() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setPriority(20)
                .setDefaultBinding(Boolean.FALSE));
        paymentInstrumentService.createPaymentInstrumentBinding(createPriorityOrderBindingRequest());

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records)
                .extracting(PaymentInstrumentBindingDTO::getSn)
                .containsExactly(PRIORITY_ORDER_BINDING_SN, BINDING_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一支付工具下存在过期、当前有效和未来生效的 ACTIVE 非默认候选。
     * 输入：三条候选都为 ACTIVE，但 validFrom/validTo 覆盖过去、当前和未来窗口。
     * 输出：查询当前 ACTIVE 候选时只返回当前有效记录。
     * 红线：已过期或未生效的绑定不得进入 route 候选，避免后续交易使用错误资金来源。
     */
    @Test
    void testQueryPaymentInstrumentBindingsShouldExcludeInactiveValidityWindowCandidates() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        LocalDateTime now = LocalDateTime.now();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setPriority(20)
                .setDefaultBinding(Boolean.FALSE)
                .setValidFrom(now.minusDays(2))
                .setValidTo(now.minusDays(1)));
        paymentInstrumentService.createPaymentInstrumentBinding(createPriorityOrderBindingRequest()
                .setValidFrom(now.minusMinutes(1))
                .setValidTo(now.plusDays(1)));
        paymentInstrumentService.createPaymentInstrumentBinding(createPriorityConflictBindingRequest()
                .setSubjectId(THIRD_FUNDING_ACCOUNT_ID)
                .setPriority(30)
                .setValidFrom(now.plusDays(1))
                .setValidTo(now.plusDays(2)));

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records)
                .extracting(PaymentInstrumentBindingDTO::getSn)
                .containsExactly(PRIORITY_ORDER_BINDING_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具绑定仍为 ACTIVE，但支付工具本身已被暂停。
     * 输入：查询新交易可用的 ACTIVE 绑定候选。
     * 输出：不返回该绑定，避免 route 使用已停用工具。
     * 红线：支付工具状态不可用时不得继续生成资金路径，查询过滤本身不得改写账本事实。
     */
    @Test
    void testQueryPaymentInstrumentBindingsShouldExcludeUnavailableInstrumentCandidates() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        jdbcTemplate.update("UPDATE t_payment_instrument SET status = ? WHERE sn = ?",
                FundsAccountStatus.SUSPENDED.name(),
                PAYMENT_INSTRUMENT_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records).isEmpty();
        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isOne();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具绑定已是 ACTIVE，但支付工具自身还未到生效时间。
     * 输入：查询新交易可用的 ACTIVE 绑定候选。
     * 输出：不返回该绑定，避免 route 提前使用未生效工具。
     * 红线：支付工具有效期是工具可用性的一部分，不得只看绑定有效期。
     */
    @Test
    void testQueryPaymentInstrumentBindingsShouldExcludeNotYetEffectiveInstrumentCandidates() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest()
                .setValidFrom(now.plusHours(1))
                .setValidTo(now.plusDays(1)));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<PaymentInstrumentBindingDTO> records = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                        .setBindingRole(PaymentInstrumentBindingRole.FUNDING_SUBJECT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(10)).getRecords();

        assertThat(records).isEmpty();
        assertThat(countRows("t_payment_instrument_binding", "sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营变更支付工具绑定时缺少审计上下文。
     * 输入：已存在 ACTIVE 绑定，变更请求分别缺 operatorId 或 changeReason。
     * 输出：变更被拒绝，绑定当前态和历史证据保持不变。
     * 红线：支付工具绑定变更必须可追溯，缺操作者或原因时不得写当前态、历史或账本事实。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectMissingAuditContextWithoutHistoryMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId("   ")
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定变更 operatorId 不能为空");

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("   ")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定变更 changeReason 不能为空");

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(binding.getPriority()).isEqualTo(10);
        assertThat(binding.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(binding.getVersion()).isEqualTo(1);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营变更支付工具绑定时缺少幂等流水。
     * 输入：已存在 ACTIVE 绑定，变更请求 requestSn 为空。
     * 输出：变更被拒绝，绑定当前态和历史证据保持不变。
     * 红线：支付工具绑定变更必须可幂等、可回放、可对账追踪，缺流水时不得改写候选池。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectMissingRequestSnWithoutHistoryMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn("   ")))
                .hasMessageContaining("支付工具绑定变更 requestSn 不能为空");

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(binding.getPriority()).isEqualTo(10);
        assertThat(binding.getVersion()).isEqualTo(1);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营变更支付工具绑定时把当前态改成倒置或空的生效窗口。
     * 输入：已存在 ACTIVE 绑定，变更请求设置 validTo 早于或等于 validFrom。
     * 输出：变更被拒绝，绑定当前态和历史证据保持不变。
     * 红线：绑定变更入口不得绕过窗口校验，不得追加伪成功历史或污染 route 候选。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectInvalidValidityWindowWithoutHistoryMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setValidFrom(now)
                        .setValidTo(now.minusSeconds(1))
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("invalid window")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定生效时间必须早于失效时间");

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setValidFrom(now)
                        .setValidTo(now)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("invalid window")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定生效时间必须早于失效时间");

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(binding.getVersion()).isEqualTo(1);
        assertThat(binding.getValidFrom()).isNull();
        assertThat(binding.getValidTo()).isNull();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营变更支付工具绑定时把通道 token secret 放入扩展上下文。
     * 输入：contextVariables 中包含嵌套 secretKey 字段、伪装字段名中的 PAN 值、外部账户原文字段或坏 JSON 敏感字段名。
     * 输出：变更被拒绝，绑定当前态和历史证据保持不变。
     * 红线：当前态变更入口不得把 token secret、密钥或 CVV 写入绑定历史或 route 候选。
     */
    @Test
    void testChangePaymentInstrumentBindingShouldRejectSensitiveContextVariablesWithoutHistoryMutation() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                        .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                        .setContextVariables(RAW_PAN_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                        .setContextVariables(RAW_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(20)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                        .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive payment instrument fields");

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(binding.getPriority()).isEqualTo(10);
        assertThat(binding.getVersion()).isEqualTo(1);
        assertThat(binding.getContextVariables()).isNull();
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testChangePaymentInstrumentBindingShouldAppendHistoryWithoutOverwritingEvidence() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

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
                    assertThat(snapshotOf(history.getAfterSnapshot())).containsEntry("priority", 10);
                    assertThat(history.getOperatorId()).isEqualTo(OPERATOR_ID);
                    assertThat(history.getChangeReason()).isEqualTo("bind funding account");
                    assertThat(history.getRequestSn()).isEqualTo(CREATE_BINDING_REQUEST_SN);
                });
        assertThat(histories.get(1))
                .satisfies(history -> {
                    assertThat(snapshotOf(history.getBeforeSnapshot()))
                            .containsEntry("priority", 10)
                            .containsEntry("version", 1);
                    assertThat(snapshotOf(history.getAfterSnapshot()))
                            .containsEntry("priority", 20)
                            .containsEntry("defaultBinding", false)
                            .containsEntry("status", "SUSPENDED")
                            .containsEntry("version", 2);
                    assertThat(history.getOperatorId()).isEqualTo(OPERATOR_ID);
                    assertThat(history.getChangeReason()).isEqualTo("risk review");
                    assertThat(history.getRequestSn()).isEqualTo(CHANGE_BINDING_REQUEST_SN);
                });
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testChangePaymentInstrumentBindingShouldReturnExistingWhenRequestSnReplayed() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ChangePaymentInstrumentBindingRequest request = new ChangePaymentInstrumentBindingRequest()
                .setBindingSn(BINDING_SN)
                .setTenantId(TENANT_ID)
                .setPriority(20)
                .setDefaultBinding(Boolean.FALSE)
                .setStatus(FundsAccountStatus.SUSPENDED)
                .setOperatorId(OPERATOR_ID)
                .setChangeReason("risk review")
                .setRequestSn(CHANGE_BINDING_REQUEST_SN)
                .setContextVariables("{\"ticket\":\"PI-007\"}");
        Long firstChangedBindingId = paymentInstrumentService.changePaymentInstrumentBinding(request);
        Long replayedChangedBindingId = paymentInstrumentService.changePaymentInstrumentBinding(request);

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(replayedChangedBindingId).isEqualTo(firstChangedBindingId);
        assertThat(binding.getVersion()).isEqualTo(2);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isEqualTo(2);
        assertThat(countRows("t_payment_instrument_binding_history", "request_sn", CHANGE_BINDING_REQUEST_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testChangePaymentInstrumentBindingShouldRejectRequestSnReplayFieldDrift() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        paymentInstrumentService.changePaymentInstrumentBinding(new ChangePaymentInstrumentBindingRequest()
                .setBindingSn(BINDING_SN)
                .setTenantId(TENANT_ID)
                .setPriority(20)
                .setDefaultBinding(Boolean.FALSE)
                .setStatus(FundsAccountStatus.SUSPENDED)
                .setOperatorId(OPERATOR_ID)
                .setChangeReason("risk review")
                .setRequestSn(CHANGE_BINDING_REQUEST_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentService.changePaymentInstrumentBinding(
                new ChangePaymentInstrumentBindingRequest()
                        .setBindingSn(BINDING_SN)
                        .setTenantId(TENANT_ID)
                        .setPriority(30)
                        .setOperatorId(OPERATOR_ID)
                        .setChangeReason("risk review")
                        .setRequestSn(CHANGE_BINDING_REQUEST_SN)))
                .hasMessageContaining("支付工具绑定请求流水号重放字段不一致");

        PaymentInstrumentBindingDTO binding = paymentInstrumentService.queryPaymentInstrumentBindings(
                new PaymentInstrumentBindingQuery()
                        .setTenantId(TENANT_ID)
                        .setSn(BINDING_SN),
                DefaultPageQueryOptions.defaults(10)).getRecords().getFirst();
        assertThat(binding.getPriority()).isEqualTo(20);
        assertThat(binding.getVersion()).isEqualTo(2);
        assertThat(countRows("t_payment_instrument_binding_history", "binding_sn", BINDING_SN)).isEqualTo(2);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
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
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE binding_sn IN (?, ?, ?, ?, ?, ?)",
                BINDING_SN,
                LONG_SUBJECT_BINDING_SN,
                DUPLICATE_DEFAULT_BINDING_SN,
                CONCURRENT_DEFAULT_BINDING_SN,
                PRIORITY_CONFLICT_BINDING_SN,
                PRIORITY_ORDER_BINDING_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE sn IN (?, ?, ?, ?, ?, ?)",
                BINDING_SN,
                LONG_SUBJECT_BINDING_SN,
                DUPLICATE_DEFAULT_BINDING_SN,
                CONCURRENT_DEFAULT_BINDING_SN,
                PRIORITY_CONFLICT_BINDING_SN,
                PRIORITY_ORDER_BINDING_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_guard WHERE instrument_sn IN (?)",
                PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                PAYMENT_INSTRUMENT_SN,
                RAW_PAYMENT_INSTRUMENT_SN,
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN,
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_pan_value",
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_external_account",
                SENSITIVE_CONTEXT_PAYMENT_INSTRUMENT_SN + "_unquoted",
                INVALID_WINDOW_PAYMENT_INSTRUMENT_SN,
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

    private CreatePaymentInstrumentBindingRequest createSecondBindingRequest() {
        return createBindingRequest()
                .setSn(DUPLICATE_DEFAULT_BINDING_SN)
                .setSubjectId(SECOND_FUNDING_ACCOUNT_ID)
                .setPriority(20)
                .setRequestSn(DUPLICATE_DEFAULT_CREATE_REQUEST_SN);
    }

    private CreatePaymentInstrumentBindingRequest createPriorityConflictBindingRequest() {
        return createBindingRequest()
                .setSn(PRIORITY_CONFLICT_BINDING_SN)
                .setSubjectId(SECOND_FUNDING_ACCOUNT_ID)
                .setPriority(10)
                .setDefaultBinding(Boolean.FALSE)
                .setRequestSn(PRIORITY_CONFLICT_CREATE_REQUEST_SN);
    }

    private CreatePaymentInstrumentBindingRequest createPriorityOrderBindingRequest() {
        return createBindingRequest()
                .setSn(PRIORITY_ORDER_BINDING_SN)
                .setSubjectId(SECOND_FUNDING_ACCOUNT_ID)
                .setPriority(10)
                .setDefaultBinding(Boolean.FALSE)
                .setRequestSn(PRIORITY_ORDER_CREATE_REQUEST_SN);
    }

    private Callable<BindingAttemptResult> concurrentBindingAttempt(CountDownLatch startGate,
                                                                    CreatePaymentInstrumentBindingRequest request) {
        return () -> {
            startGate.await();
            try {
                paymentInstrumentService.createPaymentInstrumentBinding(request);
                return new BindingAttemptResult(true, null);
            } catch (RuntimeException ex) {
                return new BindingAttemptResult(false, ex.getMessage());
            }
        };
    }

    private void assertPaymentInstrumentToStringDoesNotExposeSensitiveIdentifiers(Object value) {
        assertThat(value.toString())
                .doesNotContain("instrumentNo")
                .doesNotContain("externalInstrumentId")
                .doesNotContain(MASKED_INSTRUMENT_NO)
                .doesNotContain(EXTERNAL_INSTRUMENT_ID);
    }

    private long countRows(String tableName, String columnName, String value) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value);
        return result;
    }

    private static JSONObject snapshotOf(String snapshot) {
        return JSON.parseObject(snapshot);
    }

    @Configuration
    @Import({
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            PaymentInstrumentBindingConcurrencyGuard.class
    })
    static class Config {
    }

    private record BindingAttemptResult(boolean succeeded, String message) {
    }
}
