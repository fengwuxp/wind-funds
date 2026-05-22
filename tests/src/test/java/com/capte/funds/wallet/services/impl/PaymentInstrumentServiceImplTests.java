package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.wallet.dal.entities.PaymentInstrument;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

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

    private static final String DUPLICATE_DEFAULT_BINDING_SN = "pi_binding_service_default_duplicate";

    private static final String PRIORITY_CONFLICT_BINDING_SN = "pi_binding_service_priority_conflict";

    private static final String PRIORITY_ORDER_BINDING_SN = "pi_binding_service_priority_order";

    private static final String OWNER_ID = "owner_pi_service";

    private static final String FUNDING_ACCOUNT_ID = "funding_pi_binding_target";

    private static final String SECOND_FUNDING_ACCOUNT_ID = "funding_pi_binding_second";

    private static final String THIRD_FUNDING_ACCOUNT_ID = "funding_pi_binding_third";

    private static final String OPERATOR_ID = "ops_pi_service";

    private static final String CREATE_BINDING_REQUEST_SN = "req_pi_binding_create";

    private static final String CHANGE_BINDING_REQUEST_SN = "req_pi_binding_change";

    private static final String DUPLICATE_DEFAULT_CREATE_REQUEST_SN = "req_pi_binding_duplicate_default_create";

    private static final String DUPLICATE_DEFAULT_CHANGE_REQUEST_SN = "req_pi_binding_duplicate_default_change";

    private static final String PRIORITY_CONFLICT_CREATE_REQUEST_SN = "req_pi_binding_priority_conflict_create";

    private static final String PRIORITY_CONFLICT_CHANGE_REQUEST_SN = "req_pi_binding_priority_conflict_change";

    private static final String PRIORITY_ORDER_CREATE_REQUEST_SN = "req_pi_binding_priority_order_create";

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
                        .setInstrumentNo("4242424242424242")))
                .hasMessageContaining("instrumentNo must be masked or token reference");

        assertThat(countRows("t_payment_instrument", "sn", RAW_PAYMENT_INSTRUMENT_SN)).isZero();
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
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE binding_sn IN (?, ?, ?, ?)",
                BINDING_SN,
                DUPLICATE_DEFAULT_BINDING_SN,
                PRIORITY_CONFLICT_BINDING_SN,
                PRIORITY_ORDER_BINDING_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE sn IN (?, ?, ?, ?)",
                BINDING_SN,
                DUPLICATE_DEFAULT_BINDING_SN,
                PRIORITY_CONFLICT_BINDING_SN,
                PRIORITY_ORDER_BINDING_SN);
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

    @Configuration
    @Import(PaymentInstrumentServiceImpl.class)
    static class Config {
    }
}
