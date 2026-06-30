package com.wind.funds.wallet.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.funds.wallet.dal.entities.PaymentInstrument;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.wind.funds.wallet.dal.entities.table.PaymentInstrumentNameRefs;
import com.wind.funds.wallet.dal.mapper.PaymentInstrumentMapper;
import com.wind.funds.wallet.mapstruct.PaymentInstrumentConverter;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentQuery;
import com.wind.funds.wallet.model.request.ChangePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.RecordPaymentInstrumentBindingHistoryRequest;
import com.wind.funds.wallet.model.request.UpdatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.service.PaymentInstrumentBindingHistoryService;
import com.wind.funds.wallet.service.PaymentInstrumentBindingService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付工具服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

    private static final String DEFAULT_CREATE_OPERATOR_ID = "SYSTEM";

    private static final String DEFAULT_CREATE_REASON = "CREATE_PAYMENT_INSTRUMENT_BINDING";

    private final PaymentInstrumentMapper paymentInstrumentMapper;

    private final PaymentInstrumentBindingService paymentInstrumentBindingService;

    private final PaymentInstrumentBindingHistoryService paymentInstrumentBindingHistoryService;

    private final PaymentInstrumentBindingConcurrencyGuard paymentInstrumentBindingConcurrencyGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrument(@NonNull CreatePaymentInstrumentRequest request) {
        assertNoRawSensitiveInstrumentNo(request);
        assertNoSensitiveContextVariables(request);
        assertPaymentInstrumentValidityWindow(request.getValidFrom(), request.getValidTo());
        PaymentInstrument entity = PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrument(request);
        paymentInstrumentMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具失败");
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request) {
        assertBindingCreateRequestSnPresent(request);
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
        PaymentInstrumentBindingHistoryDTO replayed = findBindingHistoryByRequestSn(
                request.getTenantId(),
                request.getRequestSn());
        if (replayed != null) {
            assertReplayHistoryMatches(replayed, PaymentInstrumentBindingChangeType.CREATE, request.getSn());
            assertCreateReplayFieldsMatch(request, replayed);
            return paymentInstrumentBindingService.getPaymentInstrumentBinding(
                    request.getTenantId(),
                    replayed.getBindingSn()).getId();
        }
        PaymentInstrument instrument = getInstrumentBySn(request.getTenantId(), request.getInstrumentSn());
        assertInstrumentCanBind(instrument, request);
        assertFundingSubjectBindingTargetsFundingAccount(request);
        assertCreditSubjectBindingTargetsCreditAccount(request);
        assertBudgetSubjectBindingTargetsBudgetGroup(request);
        PaymentInstrumentBindingDTO binding = toBindingCandidate(request);
        assertBindingValidityWindow(binding);
        paymentInstrumentBindingConcurrencyGuard.lockActiveDefaultBindingScope(binding);
        assertNoDuplicateActiveDefaultBinding(binding);
        assertNoDuplicateActivePriorityBinding(binding);
        Long bindingId = paymentInstrumentBindingService.createPaymentInstrumentBinding(request);
        appendBindingHistory(null,
                binding,
                PaymentInstrumentBindingChangeType.CREATE,
                createOperatorId(request),
                createChangeReason(request),
                request.getRequestSn(),
                request.getValidFrom(),
                request.getContextVariables());
        return bindingId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long changePaymentInstrumentBinding(@NonNull ChangePaymentInstrumentBindingRequest request) {
        assertBindingChangeRequestSnPresent(request);
        assertBindingChangeAuditContextPresent(request);
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
        PaymentInstrumentBindingHistoryDTO replayed = findBindingHistoryByRequestSn(
                request.getTenantId(),
                request.getRequestSn());
        if (replayed != null) {
            assertReplayHistoryMatches(replayed, PaymentInstrumentBindingChangeType.UPDATE, request.getBindingSn());
            assertChangeReplayFieldsMatch(request, replayed);
            return paymentInstrumentBindingService.getPaymentInstrumentBinding(
                    request.getTenantId(),
                    replayed.getBindingSn()).getId();
        }
        PaymentInstrumentBindingDTO before = paymentInstrumentBindingService.getPaymentInstrumentBinding(
                request.getTenantId(),
                request.getBindingSn());
        PaymentInstrumentBindingDTO after = copyBinding(before);
        applyBindingChanges(after, request);
        after.setVersion(before.getVersion() + 1);
        assertBindingValidityWindow(after);
        paymentInstrumentBindingConcurrencyGuard.lockActiveDefaultBindingScope(after);
        assertNoDuplicateActiveDefaultBinding(after);
        assertNoDuplicateActivePriorityBinding(after);
        paymentInstrumentBindingService.updatePaymentInstrumentBinding(toUpdateRequest(before, after, request));
        appendBindingHistory(before,
                after,
                PaymentInstrumentBindingChangeType.UPDATE,
                request.getOperatorId(),
                request.getChangeReason(),
                request.getRequestSn(),
                request.getEffectiveAt(),
                request.getContextVariables());
        return after.getId();
    }

    private void assertBindingChangeAuditContextPresent(ChangePaymentInstrumentBindingRequest request) {
        AssertUtils.hasText(request.getOperatorId(), "支付工具绑定变更 operatorId 不能为空");
        AssertUtils.hasText(request.getChangeReason(), "支付工具绑定变更 changeReason 不能为空");
    }

    private void assertBindingCreateRequestSnPresent(CreatePaymentInstrumentBindingRequest request) {
        AssertUtils.hasText(request.getRequestSn(), "支付工具绑定创建 requestSn 不能为空");
    }

    private void assertBindingChangeRequestSnPresent(ChangePaymentInstrumentBindingRequest request) {
        AssertUtils.hasText(request.getRequestSn(), "支付工具绑定变更 requestSn 不能为空");
    }

    private @Nullable PaymentInstrumentBindingHistoryDTO findBindingHistoryByRequestSn(Long tenantId,
                                                                                      String requestSn) {
        if (!StringUtils.hasText(requestSn)) {
            return null;
        }
        return paymentInstrumentBindingHistoryService.queryPaymentInstrumentBindingHistories(
                        new PaymentInstrumentBindingHistoryQuery()
                                .setTenantId(tenantId)
                                .setRequestSn(requestSn),
                        DefaultPageQueryOptions.defaults(1))
                .getRecords()
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void assertReplayHistoryMatches(PaymentInstrumentBindingHistoryDTO history,
                                            PaymentInstrumentBindingChangeType expectedChangeType,
                                            String bindingSn) {
        AssertUtils.isTrue(history.getChangeType() == expectedChangeType
                        && Objects.equals(history.getBindingSn(), bindingSn),
                "支付工具绑定请求流水号已被其他变更使用，requestSn = {}",
                history.getRequestSn());
    }

    private void assertCreateReplayFieldsMatch(CreatePaymentInstrumentBindingRequest request,
                                               PaymentInstrumentBindingHistoryDTO history) {
        JSONObject after = JSON.parseObject(history.getAfterSnapshot());
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.instrumentSn, request.getInstrumentSn(),
                after.getString(PaymentInstrumentBinding.Fields.instrumentSn));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.bindingRole, request.getBindingRole().name(),
                after.getString(PaymentInstrumentBinding.Fields.bindingRole));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.subjectId, request.getSubjectId(),
                after.getString(PaymentInstrumentBinding.Fields.subjectId));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.subjectType, request.getSubjectType().name(),
                after.getString(PaymentInstrumentBinding.Fields.subjectType));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.currency, request.getCurrency().name(),
                after.getString(PaymentInstrumentBinding.Fields.currency));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.priority, request.getPriority(),
                after.getInteger(PaymentInstrumentBinding.Fields.priority));
        assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.defaultBinding, request.getDefaultBinding(),
                after.getBoolean(PaymentInstrumentBinding.Fields.defaultBinding));
    }

    private void assertChangeReplayFieldsMatch(ChangePaymentInstrumentBindingRequest request,
                                               PaymentInstrumentBindingHistoryDTO history) {
        JSONObject after = JSON.parseObject(history.getAfterSnapshot());
        if (request.getPriority() != null) {
            assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.priority, request.getPriority(),
                    after.getInteger(PaymentInstrumentBinding.Fields.priority));
        }
        if (request.getDefaultBinding() != null) {
            assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.defaultBinding,
                    request.getDefaultBinding(),
                    after.getBoolean(PaymentInstrumentBinding.Fields.defaultBinding));
        }
        if (request.getStatus() != null) {
            assertReplayFieldMatches(history, PaymentInstrumentBinding.Fields.status, request.getStatus().name(),
                    after.getString(PaymentInstrumentBinding.Fields.status));
        }
    }

    private void assertReplayFieldMatches(PaymentInstrumentBindingHistoryDTO history,
                                          String fieldName,
                                          Object requestValue,
                                          Object snapshotValue) {
        AssertUtils.isTrue(Objects.equals(requestValue, snapshotValue),
                "支付工具绑定请求流水号重放字段不一致，requestSn = {}, field = {}",
                history.getRequestSn(),
                fieldName);
    }

    @Override
    public @NonNull PaymentInstrumentDTO getPaymentInstrumentById(@NonNull Long id) {
        PaymentInstrument result = paymentInstrumentMapper.selectOneById(id);
        AssertUtils.notNull(result, "支付工具不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull PaymentInstrumentDTO getPaymentInstrumentBySn(@NonNull String sn) {
        PaymentInstrumentNameRefs ref = PaymentInstrumentNameRefs.paymentInstrument;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        PaymentInstrument result = paymentInstrumentMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "支付工具不存在，sn = {}", sn);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<PaymentInstrumentDTO> queryPaymentInstruments(
            @NonNull PaymentInstrumentQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        PaymentInstrumentNameRefs ref = PaymentInstrumentNameRefs.paymentInstrument;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.instrumentType.eq(query.getInstrumentType()))
                .and(ref.instrumentDirection.eq(query.getInstrumentDirection()))
                .and(ref.instrumentNo.eq(query.getInstrumentNo()))
                .and(ref.channelCode.eq(query.getChannelCode()))
                .and(ref.externalInstrumentId.eq(query.getExternalInstrumentId()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.status.eq(query.getStatus()));
        return MybatisQueryHelper.<PaymentInstrument, PaymentInstrumentDTO>query(wrapper)
                .counter(paymentInstrumentMapper::selectCountByQuery)
                .resultQueryFunc(paymentInstrumentMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    public @NonNull WindPagination<PaymentInstrumentBindingDTO> queryPaymentInstrumentBindings(
            @NonNull PaymentInstrumentBindingQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return paymentInstrumentBindingService.queryPaymentInstrumentBindings(query, options);
    }

    @Override
    public @NonNull WindPagination<PaymentInstrumentBindingHistoryDTO> queryPaymentInstrumentBindingHistories(
            @NonNull PaymentInstrumentBindingHistoryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return paymentInstrumentBindingHistoryService.queryPaymentInstrumentBindingHistories(query, options);
    }

    private PaymentInstrumentDTO toDTO(PaymentInstrument entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentDTO(entity);
    }

    private PaymentInstrument getInstrumentBySn(Long tenantId, String instrumentSn) {
        PaymentInstrumentNameRefs ref = PaymentInstrumentNameRefs.paymentInstrument;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(instrumentSn));
        PaymentInstrument result = paymentInstrumentMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "支付工具不存在，instrumentSn = {}", instrumentSn);
        return result;
    }

    private void assertInstrumentCanBind(PaymentInstrument instrument, CreatePaymentInstrumentBindingRequest request) {
        AssertUtils.isTrue(instrument.getStatus() == FundsAccountStatus.ACTIVE,
                "支付工具不可用于绑定，instrumentSn = {}",
                request.getInstrumentSn());
        AssertUtils.isTrue(instrument.getCurrency() == request.getCurrency(),
                "支付工具币种与绑定币种不一致，instrumentSn = {}",
                request.getInstrumentSn());
        AssertUtils.isTrue(supportsBindingRole(instrument.getInstrumentDirection(), request.getBindingRole()),
                "支付工具方向不支持绑定角色，instrumentSn = {}",
                request.getInstrumentSn());
    }

    private void assertFundingSubjectBindingTargetsFundingAccount(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.FUNDING_SUBJECT) {
            return;
        }
        AssertUtils.isTrue(request.getSubjectType() == FundsSubjectType.FUNDING_ACCOUNT,
                "真实资金主体绑定必须指向资金账户，bindingSn = {}, subjectType = {}",
                request.getSn(),
                request.getSubjectType());
    }

    private void assertCreditSubjectBindingTargetsCreditAccount(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.CREDIT_SUBJECT) {
            return;
        }
        AssertUtils.isTrue(request.getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "信用控制主体绑定必须指向信用账户，bindingSn = {}, subjectType = {}",
                request.getSn(),
                request.getSubjectType());
    }

    private void assertBudgetSubjectBindingTargetsBudgetGroup(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.BUDGET_SUBJECT) {
            return;
        }
        AssertUtils.isTrue(request.getSubjectType() == FundsSubjectType.BUDGET_GROUP,
                "预算控制范围绑定必须指向 BUDGET_GROUP 兼容类型，bindingSn = {}, subjectType = {}",
                request.getSn(),
                request.getSubjectType());
    }

    private void assertPaymentInstrumentValidityWindow(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom == null || validTo == null) {
            return;
        }
        AssertUtils.isTrue(validFrom.isBefore(validTo), "支付工具生效时间必须早于失效时间");
    }

    private void assertBindingValidityWindow(PaymentInstrumentBindingDTO binding) {
        if (binding.getValidFrom() == null || binding.getValidTo() == null) {
            return;
        }
        AssertUtils.isTrue(binding.getValidFrom().isBefore(binding.getValidTo()),
                "支付工具绑定生效时间必须早于失效时间");
    }

    private boolean supportsBindingRole(PaymentInstrumentDirection direction, PaymentInstrumentBindingRole bindingRole) {
        if (bindingRole == PaymentInstrumentBindingRole.RECEIVE_SUBJECT) {
            return direction == PaymentInstrumentDirection.RECEIVE || direction == PaymentInstrumentDirection.BOTH;
        }
        return direction == PaymentInstrumentDirection.PAYMENT || direction == PaymentInstrumentDirection.BOTH;
    }

    private void assertNoDuplicateActiveDefaultBinding(PaymentInstrumentBindingDTO binding) {
        boolean duplicated = paymentInstrumentBindingService.existsOverlappingActiveDefaultBinding(binding);
        AssertUtils.isFalse(duplicated,
                "默认支付工具绑定不唯一，instrumentSn = {}, bindingRole = {}, currency = {}",
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency());
    }

    private void assertNoDuplicateActivePriorityBinding(PaymentInstrumentBindingDTO binding) {
        int priority = binding.getPriority() == null ? 0 : binding.getPriority();
        boolean duplicated = paymentInstrumentBindingService.existsOverlappingActivePriorityBinding(binding);
        AssertUtils.isFalse(duplicated,
                "支付工具绑定优先级冲突，instrumentSn = {}, bindingRole = {}, currency = {}, priority = {}",
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency(),
                priority);
    }

    private void applyBindingChanges(PaymentInstrumentBindingDTO entity, ChangePaymentInstrumentBindingRequest request) {
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        if (request.getDefaultBinding() != null) {
            entity.setDefaultBinding(request.getDefaultBinding());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getValidFrom() != null) {
            entity.setValidFrom(request.getValidFrom());
        }
        if (request.getValidTo() != null) {
            entity.setValidTo(request.getValidTo());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getContextVariables() != null) {
            entity.setContextVariables(request.getContextVariables());
        }
    }

    private UpdatePaymentInstrumentBindingRequest toUpdateRequest(PaymentInstrumentBindingDTO before,
                                                                  PaymentInstrumentBindingDTO after,
                                                                  ChangePaymentInstrumentBindingRequest request) {
        return new UpdatePaymentInstrumentBindingRequest()
                .setId(before.getId())
                .setTenantId(before.getTenantId())
                .setBindingSn(before.getSn())
                .setExpectedVersion(before.getVersion())
                .setNextVersion(after.getVersion())
                .setPriority(request.getPriority() == null ? null : after.getPriority())
                .setDefaultBinding(request.getDefaultBinding() == null ? null : after.getDefaultBinding())
                .setStatus(request.getStatus() == null ? null : after.getStatus())
                .setValidFrom(request.getValidFrom() == null ? null : after.getValidFrom())
                .setValidTo(request.getValidTo() == null ? null : after.getValidTo())
                .setDescription(request.getDescription() == null ? null : after.getDescription())
                .setContextVariables(request.getContextVariables() == null ? null : after.getContextVariables());
    }

    private void appendBindingHistory(PaymentInstrumentBindingDTO before,
                                      PaymentInstrumentBindingDTO after,
                                      PaymentInstrumentBindingChangeType changeType,
                                      String operatorId,
                                      String changeReason,
                                      String requestSn,
                                      LocalDateTime effectiveAt,
                                      String contextVariables) {
        paymentInstrumentBindingHistoryService.recordPaymentInstrumentBindingHistory(
                new RecordPaymentInstrumentBindingHistoryRequest()
                        .setTenantId(after.getTenantId())
                        .setBindingSn(after.getSn())
                        .setInstrumentSn(after.getInstrumentSn())
                        .setChangeType(changeType)
                        .setVersion(after.getVersion())
                        .setBeforeSnapshot(before == null ? null : snapshotBinding(before))
                        .setAfterSnapshot(snapshotBinding(after))
                        .setOperatorId(operatorId)
                        .setChangeReason(changeReason)
                        .setEffectiveAt(effectiveAt)
                        .setRequestSn(requestSn)
                        .setContextVariables(contextVariables));
    }

    private String snapshotBinding(PaymentInstrumentBindingDTO binding) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(PaymentInstrumentBinding.Fields.sn, binding.getSn());
        values.put(PaymentInstrumentBinding.Fields.instrumentSn, binding.getInstrumentSn());
        values.put(PaymentInstrumentBinding.Fields.bindingRole, binding.getBindingRole());
        values.put(PaymentInstrumentBinding.Fields.subjectId, binding.getSubjectId());
        values.put(PaymentInstrumentBinding.Fields.subjectType, binding.getSubjectType());
        values.put(PaymentInstrumentBinding.Fields.currency, binding.getCurrency());
        values.put(PaymentInstrumentBinding.Fields.priority, binding.getPriority());
        values.put(PaymentInstrumentBinding.Fields.defaultBinding, binding.getDefaultBinding());
        values.put(PaymentInstrumentBinding.Fields.status, binding.getStatus());
        values.put(PaymentInstrumentBinding.Fields.version, binding.getVersion());
        values.put(PaymentInstrumentBinding.Fields.validFrom, binding.getValidFrom());
        values.put(PaymentInstrumentBinding.Fields.validTo, binding.getValidTo());
        return JSON.toJSONString(values);
    }

    private PaymentInstrumentBindingDTO copyBinding(PaymentInstrumentBindingDTO source) {
        PaymentInstrumentBindingDTO result = new PaymentInstrumentBindingDTO();
        result.setId(source.getId());
        result.setGmtCreate(source.getGmtCreate());
        result.setGmtModified(source.getGmtModified());
        result.setSn(source.getSn());
        result.setTenantId(source.getTenantId());
        result.setInstrumentSn(source.getInstrumentSn());
        result.setBindingRole(source.getBindingRole());
        result.setSubjectId(source.getSubjectId());
        result.setSubjectType(source.getSubjectType());
        result.setCurrency(source.getCurrency());
        result.setPriority(source.getPriority());
        result.setDefaultBinding(source.getDefaultBinding());
        result.setStatus(source.getStatus());
        result.setVersion(source.getVersion());
        result.setValidFrom(source.getValidFrom());
        result.setValidTo(source.getValidTo());
        result.setDescription(source.getDescription());
        result.setContextVariables(source.getContextVariables());
        return result;
    }

    private PaymentInstrumentBindingDTO toBindingCandidate(CreatePaymentInstrumentBindingRequest request) {
        PaymentInstrumentBinding binding =
                PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBinding(request);
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingDTO(binding);
    }

    private String createOperatorId(CreatePaymentInstrumentBindingRequest request) {
        if (StringUtils.hasText(request.getOperatorId())) {
            return request.getOperatorId();
        }
        return DEFAULT_CREATE_OPERATOR_ID;
    }

    private String createChangeReason(CreatePaymentInstrumentBindingRequest request) {
        if (StringUtils.hasText(request.getChangeReason())) {
            return request.getChangeReason();
        }
        return DEFAULT_CREATE_REASON;
    }

    private void assertNoRawSensitiveInstrumentNo(CreatePaymentInstrumentRequest request) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.isRawSensitiveInstrumentNo(
                        request.getInstrumentNo()),
                "instrumentNo must be masked or token reference");
    }

    private void assertNoSensitiveContextVariables(CreatePaymentInstrumentRequest request) {
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
    }

    private void assertNoSensitivePaymentInstrumentContextVariables(String contextVariables) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(contextVariables),
                "contextVariables must not contain sensitive payment instrument fields");
        FundsBenefitSpecValidators.rejectInstructionContextVariables(contextVariables, "paymentInstrument");
    }

}
