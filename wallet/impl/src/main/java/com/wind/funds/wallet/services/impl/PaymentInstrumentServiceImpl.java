package com.wind.funds.wallet.services.impl;

import com.wind.jackson.WindJson;
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
import com.wind.funds.wallet.model.request.UnbindPaymentInstrumentBindingRequest;
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
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
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
        assertBindingCreateIdentityPresent(request);
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
        assertFundingSubjectBindingTargetsFundingAccount(request);
        assertCreditSubjectBindingTargetsCreditAccount(request);
        assertBudgetSubjectBindingNotSupported(request);
        PaymentInstrumentBindingDTO binding = toBindingCandidate(request);
        assertBindingValidityWindow(binding);
        PaymentInstrumentBindingDTO existing = findBindingByBusinessKey(request);
        if (existing != null) {
            assertSameBinding(request, existing);
            return existing.getId();
        }
        PaymentInstrument instrument = getInstrumentBySn(request.getTenantId(), request.getInstrumentSn());
        assertInstrumentCanBind(instrument, request);
        assertNoDuplicateActiveDefaultBinding(binding);
        assertNoDuplicateActivePriorityBinding(binding);
        Long bindingId = paymentInstrumentBindingService.createPaymentInstrumentBinding(request);
        PaymentInstrumentBindingDTO created = paymentInstrumentBindingService.getPaymentInstrumentBindingById(bindingId);
        appendBindingHistory(null,
                created,
                PaymentInstrumentBindingChangeType.CREATE,
                createOperatorId(request),
                createChangeReason(request),
                request.getValidFrom(),
                request.getContextVariables());
        return bindingId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long changePaymentInstrumentBinding(@NonNull ChangePaymentInstrumentBindingRequest request) {
        assertBindingIdentityPresent(request.getTenantId(), request.getBindingSn());
        assertBindingChangeAuditContextPresent(request);
        assertEffectiveAtNotFuture(request.getEffectiveAt());
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
        PaymentInstrumentBindingDTO before = paymentInstrumentBindingService.getPaymentInstrumentBinding(
                request.getTenantId(),
                request.getBindingSn());
        PaymentInstrumentBindingDTO after = copyBinding(before);
        applyBindingChanges(after, request);
        if (sameBindingState(before, after)) {
            return before.getId();
        }
        after.setVersion(before.getVersion() + 1);
        assertBindingValidityWindow(after);
        assertNoDuplicateActiveDefaultBinding(after);
        assertNoDuplicateActivePriorityBinding(after);
        paymentInstrumentBindingService.updatePaymentInstrumentBinding(toUpdateRequest(before, after, request));
        appendBindingHistory(before,
                after,
                PaymentInstrumentBindingChangeType.UPDATE,
                request.getOperatorId(),
                request.getChangeReason(),
                request.getEffectiveAt(),
                request.getContextVariables());
        return after.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindPaymentInstrumentBinding(@NonNull UnbindPaymentInstrumentBindingRequest request) {
        assertBindingIdentityPresent(request.getTenantId(), request.getBindingSn());
        assertBindingUnbindAuditContextPresent(request);
        assertEffectiveAtNotFuture(request.getEffectiveAt());
        assertNoSensitivePaymentInstrumentContextVariables(request.getContextVariables());
        PaymentInstrumentBindingDTO binding = findBindingBySn(request.getTenantId(), request.getBindingSn());
        if (binding == null) {
            assertBindingAlreadyUnbound(request.getTenantId(), request.getBindingSn());
            return;
        }
        paymentInstrumentBindingService.deletePaymentInstrumentBinding(
                request.getTenantId(),
                request.getBindingSn(),
                binding.getVersion());
        appendBindingHistory(binding,
                null,
                PaymentInstrumentBindingChangeType.UNBIND,
                request.getOperatorId(),
                request.getChangeReason(),
                request.getEffectiveAt(),
                request.getContextVariables());
    }

    private void assertBindingChangeAuditContextPresent(ChangePaymentInstrumentBindingRequest request) {
        AssertUtils.hasText(request.getOperatorId(), "支付工具绑定变更 operatorId 不能为空");
        AssertUtils.hasText(request.getChangeReason(), "支付工具绑定变更 changeReason 不能为空");
    }

    private void assertBindingCreateIdentityPresent(CreatePaymentInstrumentBindingRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getBindingRole(), "支付工具绑定角色不能为空");
        AssertUtils.hasText(request.getSubjectId(), "支付工具绑定主体 ID 不能为空");
        AssertUtils.notNull(request.getSubjectType(), "支付工具绑定主体类型不能为空");
        AssertUtils.notNull(request.getCurrency(), "支付工具绑定币种不能为空");
    }

    private void assertBindingIdentityPresent(Long tenantId, String bindingSn) {
        AssertUtils.notNull(tenantId, "租户 ID 不能为空");
        AssertUtils.hasText(bindingSn, "支付工具绑定号不能为空");
    }

    private void assertEffectiveAtNotFuture(LocalDateTime effectiveAt) {
        if (effectiveAt == null) {
            return;
        }
        AssertUtils.isFalse(effectiveAt.isAfter(LocalDateTime.now()),
                "支付工具绑定变更 effectiveAt 不能晚于当前时间");
    }

    private void assertBindingUnbindAuditContextPresent(UnbindPaymentInstrumentBindingRequest request) {
        AssertUtils.hasText(request.getOperatorId(), "支付工具解绑 operatorId 不能为空");
        AssertUtils.hasText(request.getChangeReason(), "支付工具解绑 changeReason 不能为空");
    }

    private @Nullable PaymentInstrumentBindingDTO findBindingByBusinessKey(
            CreatePaymentInstrumentBindingRequest request) {
        return paymentInstrumentBindingService.queryPaymentInstrumentBindings(
                        new PaymentInstrumentBindingQuery()
                                .setTenantId(request.getTenantId())
                                .setInstrumentSn(request.getInstrumentSn())
                                .setBindingRole(request.getBindingRole())
                                .setSubjectType(request.getSubjectType())
                                .setSubjectId(request.getSubjectId())
                                .setCurrency(request.getCurrency()),
                        DefaultPageQueryOptions.defaults(2))
                .getRecords()
                .stream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable PaymentInstrumentBindingDTO findBindingBySn(Long tenantId, String bindingSn) {
        return paymentInstrumentBindingService.queryPaymentInstrumentBindings(
                        new PaymentInstrumentBindingQuery()
                                .setTenantId(tenantId)
                                .setSn(bindingSn),
                        DefaultPageQueryOptions.defaults(1))
                .getRecords()
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void assertSameBinding(CreatePaymentInstrumentBindingRequest request,
                                   PaymentInstrumentBindingDTO existing) {
        AssertUtils.isTrue(sameBindingState(toBindingCandidate(request), existing),
                "支付工具绑定已存在但内容不一致，instrumentSn = {}, bindingRole = {}, subjectType = {}, subjectId = {}, currency = {}",
                request.getInstrumentSn(),
                request.getBindingRole(),
                request.getSubjectType(),
                request.getSubjectId(),
                request.getCurrency());
    }

    private boolean sameBindingState(PaymentInstrumentBindingDTO left, PaymentInstrumentBindingDTO right) {
        return Objects.equals(left.getPriority(), right.getPriority())
                && Objects.equals(left.getDefaultBinding(), right.getDefaultBinding())
                && left.getState() == right.getState()
                && Objects.equals(left.getValidFrom(), right.getValidFrom())
                && Objects.equals(left.getValidTo(), right.getValidTo())
                && Objects.equals(left.getDescription(), right.getDescription())
                && Objects.equals(left.getContextVariables(), right.getContextVariables());
    }

    private void assertBindingAlreadyUnbound(Long tenantId, String bindingSn) {
        AssertUtils.isTrue(isBindingAlreadyUnbound(tenantId, bindingSn),
                "支付工具绑定不存在，bindingSn = {}",
                bindingSn);
    }

    private boolean isBindingAlreadyUnbound(Long tenantId, String bindingSn) {
        return !paymentInstrumentBindingHistoryService.queryPaymentInstrumentBindingHistories(
                        new PaymentInstrumentBindingHistoryQuery()
                                .setTenantId(tenantId)
                                .setBindingSn(bindingSn)
                                .setChangeType(PaymentInstrumentBindingChangeType.UNBIND),
                        DefaultPageQueryOptions.defaults(1))
                .getRecords()
                .isEmpty();
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
                .and(ref.flowDirection.eq(query.getFlowDirection()))
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
        AssertUtils.isTrue(supportsBindingRole(instrument.getFlowDirection(), request.getBindingRole()),
                "支付工具资金流向不支持绑定角色，instrumentSn = {}",
                request.getInstrumentSn());
    }

    private void assertFundingSubjectBindingTargetsFundingAccount(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.FUNDING_SUBJECT) {
            return;
        }
        AssertUtils.isTrue(request.getSubjectType() == FundsSubjectType.FUNDING_ACCOUNT,
                "真实资金主体绑定必须指向资金账户，instrumentSn = {}, subjectType = {}",
                request.getInstrumentSn(),
                request.getSubjectType());
    }

    private void assertCreditSubjectBindingTargetsCreditAccount(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.CREDIT_SUBJECT) {
            return;
        }
        AssertUtils.isTrue(request.getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "信用控制主体绑定必须指向信用账户，instrumentSn = {}, subjectType = {}",
                request.getInstrumentSn(),
                request.getSubjectType());
    }

    private void assertBudgetSubjectBindingNotSupported(CreatePaymentInstrumentBindingRequest request) {
        if (request.getBindingRole() != PaymentInstrumentBindingRole.BUDGET_SUBJECT) {
            return;
        }
        AssertUtils.isFalse(request.getBindingRole() == PaymentInstrumentBindingRole.BUDGET_SUBJECT,
                "支出控制范围不通过支付工具资金主体绑定维护，请使用 Spend Rule / Spend Control 控制范围，instrumentSn = {}",
                request.getInstrumentSn());
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

    private boolean supportsBindingRole(PaymentInstrumentFlowDirection direction, PaymentInstrumentBindingRole bindingRole) {
        if (bindingRole == PaymentInstrumentBindingRole.RECEIVE_SUBJECT) {
            return direction == PaymentInstrumentFlowDirection.INBOUND || direction == PaymentInstrumentFlowDirection.BIDIRECTIONAL;
        }
        return direction == PaymentInstrumentFlowDirection.OUTBOUND || direction == PaymentInstrumentFlowDirection.BIDIRECTIONAL;
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
        if (request.getState() != null) {
            entity.setState(request.getState());
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
                .setState(request.getState() == null ? null : after.getState())
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
                                      LocalDateTime effectiveAt,
                                      String contextVariables) {
        PaymentInstrumentBindingDTO binding = after == null ? before : after;
        int version = after == null ? before.getVersion() + 1 : after.getVersion();
        paymentInstrumentBindingHistoryService.recordPaymentInstrumentBindingHistory(
                new RecordPaymentInstrumentBindingHistoryRequest()
                        .setTenantId(binding.getTenantId())
                        .setBindingSn(binding.getSn())
                        .setInstrumentSn(binding.getInstrumentSn())
                        .setChangeType(changeType)
                        .setVersion(version)
                        .setBeforeSnapshot(before == null ? null : snapshotBinding(before))
                        .setAfterSnapshot(after == null ? null : snapshotBinding(after))
                        .setOperatorId(operatorId)
                        .setChangeReason(changeReason)
                        .setEffectiveAt(effectiveAt)
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
        values.put(PaymentInstrumentBinding.Fields.state, binding.getState());
        values.put(PaymentInstrumentBinding.Fields.version, binding.getVersion());
        values.put(PaymentInstrumentBinding.Fields.validFrom, binding.getValidFrom());
        values.put(PaymentInstrumentBinding.Fields.validTo, binding.getValidTo());
        values.put(PaymentInstrumentBinding.Fields.description, binding.getDescription());
        values.put(PaymentInstrumentBinding.Fields.contextVariables, binding.getContextVariables());
        return WindJson.toJsonString(values);
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
        result.setState(source.getState());
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
