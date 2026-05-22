package com.capte.funds.wallet.services.impl;

import com.alibaba.fastjson2.JSON;
import com.capte.funds.wallet.dal.entities.PaymentInstrument;
import com.capte.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.capte.funds.wallet.dal.entities.PaymentInstrumentBindingHistory;
import com.capte.funds.wallet.dal.entities.table.PaymentInstrumentBindingNameRefs;
import com.capte.funds.wallet.dal.entities.table.PaymentInstrumentBindingHistoryNameRefs;
import com.capte.funds.wallet.dal.entities.table.PaymentInstrumentNameRefs;
import com.capte.funds.wallet.dal.mapper.PaymentInstrumentBindingMapper;
import com.capte.funds.wallet.dal.mapper.PaymentInstrumentBindingHistoryMapper;
import com.capte.funds.wallet.dal.mapper.PaymentInstrumentMapper;
import com.capte.funds.wallet.mapstruct.PaymentInstrumentConverter;
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
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付工具服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

    private static final WindSequenceType PAYMENT_INSTRUMENT_BINDING_HISTORY_SEQUENCE_TYPE =
            WindSequenceType.immutable("PAYMENT_INSTRUMENT_BINDING_HISTORY", "PIBH", 6);

    private static final String DEFAULT_CREATE_OPERATOR_ID = "SYSTEM";

    private static final String DEFAULT_CREATE_REASON = "CREATE_PAYMENT_INSTRUMENT_BINDING";

    private final PaymentInstrumentMapper paymentInstrumentMapper;

    private final PaymentInstrumentBindingMapper paymentInstrumentBindingMapper;

    private final PaymentInstrumentBindingHistoryMapper paymentInstrumentBindingHistoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrument(@NonNull CreatePaymentInstrumentRequest request) {
        assertNoRawSensitiveInstrumentNo(request);
        PaymentInstrument entity = PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrument(request);
        paymentInstrumentMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具失败");
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request) {
        PaymentInstrument instrument = getInstrumentBySn(request.getTenantId(), request.getInstrumentSn());
        assertInstrumentCanBind(instrument, request);
        PaymentInstrumentBinding entity =
                PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBinding(request);
        assertNoDuplicateActiveDefaultBinding(entity);
        assertNoDuplicateActivePriorityBinding(entity);
        paymentInstrumentBindingMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具绑定失败");
        appendBindingHistory(null,
                entity,
                PaymentInstrumentBindingChangeType.CREATE,
                createOperatorId(request),
                createChangeReason(request),
                request.getRequestSn(),
                request.getValidFrom(),
                request.getContextVariables());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long changePaymentInstrumentBinding(@NonNull ChangePaymentInstrumentBindingRequest request) {
        PaymentInstrumentBinding entity = getBindingBySn(request.getTenantId(), request.getBindingSn());
        PaymentInstrumentBinding before = copyBinding(entity);
        PaymentInstrumentBinding after = copyBinding(before);
        applyBindingChanges(after, request);
        after.setVersion(before.getVersion() + 1);
        assertNoDuplicateActiveDefaultBinding(after);
        assertNoDuplicateActivePriorityBinding(after);
        AssertUtils.isTrue(paymentInstrumentBindingMapper.updateByQuery(
                        toBindingUpdateEntity(after, request),
                        versionMatchedBinding(before)) == 1,
                "支付工具绑定已变更，请重试，bindingSn = {}",
                request.getBindingSn());
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
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.bindingRole.eq(query.getBindingRole()))
                .and(ref.subjectId.eq(query.getSubjectId()))
                .and(ref.subjectType.eq(query.getSubjectType()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.defaultBinding.eq(query.getDefaultBinding()))
                .and(ref.status.eq(query.getStatus()));
        return MybatisQueryHelper.<PaymentInstrumentBinding, PaymentInstrumentBindingDTO>query(wrapper)
                .counter(paymentInstrumentBindingMapper::selectCountByQuery)
                .resultQueryFunc(paymentInstrumentBindingMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    public @NonNull WindPagination<PaymentInstrumentBindingHistoryDTO> queryPaymentInstrumentBindingHistories(
            @NonNull PaymentInstrumentBindingHistoryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        PaymentInstrumentBindingHistoryNameRefs ref =
                PaymentInstrumentBindingHistoryNameRefs.paymentInstrumentBindingHistory;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.bindingSn.eq(query.getBindingSn()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.changeType.eq(query.getChangeType()))
                .and(ref.version.eq(query.getVersion()))
                .and(ref.requestSn.eq(query.getRequestSn()));
        return MybatisQueryHelper.<PaymentInstrumentBindingHistory, PaymentInstrumentBindingHistoryDTO>query(wrapper)
                .counter(paymentInstrumentBindingHistoryMapper::selectCountByQuery)
                .resultQueryFunc(paymentInstrumentBindingHistoryMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private PaymentInstrumentDTO toDTO(PaymentInstrument entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentDTO(entity);
    }

    private PaymentInstrumentBindingDTO toDTO(PaymentInstrumentBinding entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingDTO(entity);
    }

    private PaymentInstrumentBindingHistoryDTO toDTO(PaymentInstrumentBindingHistory entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingHistoryDTO(entity);
    }

    private PaymentInstrumentBinding getBindingBySn(Long tenantId, String bindingSn) {
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(bindingSn));
        PaymentInstrumentBinding result = paymentInstrumentBindingMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "支付工具绑定不存在，bindingSn = {}", bindingSn);
        return result;
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

    private boolean supportsBindingRole(PaymentInstrumentDirection direction, PaymentInstrumentBindingRole bindingRole) {
        if (bindingRole == PaymentInstrumentBindingRole.RECEIVE_SUBJECT) {
            return direction == PaymentInstrumentDirection.RECEIVE || direction == PaymentInstrumentDirection.BOTH;
        }
        return direction == PaymentInstrumentDirection.PAYMENT || direction == PaymentInstrumentDirection.BOTH;
    }

    private void assertNoDuplicateActiveDefaultBinding(PaymentInstrumentBinding binding) {
        if (!Boolean.TRUE.equals(binding.getDefaultBinding()) || binding.getStatus() != FundsAccountStatus.ACTIVE) {
            return;
        }
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(binding.getTenantId()))
                .and(ref.instrumentSn.eq(binding.getInstrumentSn()))
                .and(ref.bindingRole.eq(binding.getBindingRole()))
                .and(ref.currency.eq(binding.getCurrency()))
                .and(ref.defaultBinding.eq(Boolean.TRUE))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = paymentInstrumentBindingMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> binding.getId() == null || !binding.getId().equals(existing.getId()));
        AssertUtils.isFalse(duplicated,
                "默认支付工具绑定不唯一，instrumentSn = {}, bindingRole = {}, currency = {}",
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency());
    }

    private void assertNoDuplicateActivePriorityBinding(PaymentInstrumentBinding binding) {
        if (binding.getStatus() != FundsAccountStatus.ACTIVE) {
            return;
        }
        int priority = binding.getPriority() == null ? 0 : binding.getPriority();
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(binding.getTenantId()))
                .and(ref.instrumentSn.eq(binding.getInstrumentSn()))
                .and(ref.bindingRole.eq(binding.getBindingRole()))
                .and(ref.currency.eq(binding.getCurrency()))
                .and(ref.priority.eq(priority))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = paymentInstrumentBindingMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> binding.getId() == null || !binding.getId().equals(existing.getId()));
        AssertUtils.isFalse(duplicated,
                "支付工具绑定优先级冲突，instrumentSn = {}, bindingRole = {}, currency = {}, priority = {}",
                binding.getInstrumentSn(),
                binding.getBindingRole(),
                binding.getCurrency(),
                priority);
    }

    private void applyBindingChanges(PaymentInstrumentBinding entity, ChangePaymentInstrumentBindingRequest request) {
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

    private PaymentInstrumentBinding toBindingUpdateEntity(PaymentInstrumentBinding after,
                                                           ChangePaymentInstrumentBindingRequest request) {
        PaymentInstrumentBinding entity = UpdateEntity.of(PaymentInstrumentBinding.class);
        UpdateWrapper<PaymentInstrumentBinding> updateWrapper = UpdateWrapper.of(entity);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.priority,
                after.getPriority(),
                request.getPriority() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.defaultBinding,
                after.getDefaultBinding(),
                request.getDefaultBinding() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.status,
                after.getStatus(),
                request.getStatus() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.validFrom,
                after.getValidFrom(),
                request.getValidFrom() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.validTo,
                after.getValidTo(),
                request.getValidTo() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.description,
                after.getDescription(),
                request.getDescription() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.contextVariables,
                after.getContextVariables(),
                request.getContextVariables() != null);
        updateWrapper.set(PaymentInstrumentBindingNameRefs.paymentInstrumentBinding.version, after.getVersion(), true);
        return entity;
    }

    private QueryWrapper versionMatchedBinding(PaymentInstrumentBinding before) {
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        return QueryWrapper.create()
                .where(ref.id.eq(before.getId()))
                .and(ref.tenantId.eq(before.getTenantId()))
                .and(ref.version.eq(before.getVersion()));
    }

    private void appendBindingHistory(PaymentInstrumentBinding before,
                                      PaymentInstrumentBinding after,
                                      PaymentInstrumentBindingChangeType changeType,
                                      String operatorId,
                                      String changeReason,
                                      String requestSn,
                                      LocalDateTime effectiveAt,
                                      String contextVariables) {
        PaymentInstrumentBindingHistory history = new PaymentInstrumentBindingHistory();
        history.setSn(TemporalSequenceFactory.hourNext(PAYMENT_INSTRUMENT_BINDING_HISTORY_SEQUENCE_TYPE));
        history.setTenantId(after.getTenantId());
        history.setBindingSn(after.getSn());
        history.setInstrumentSn(after.getInstrumentSn());
        history.setChangeType(changeType);
        history.setVersion(after.getVersion());
        history.setBeforeSnapshot(before == null ? null : snapshotBinding(before));
        history.setAfterSnapshot(snapshotBinding(after));
        history.setOperatorId(operatorId);
        history.setChangeReason(changeReason);
        history.setEffectiveAt(effectiveAt == null ? LocalDateTime.now() : effectiveAt);
        history.setRequestSn(requestSn);
        history.setContextVariables(contextVariables);
        paymentInstrumentBindingHistoryMapper.insertSelective(history);
        AssertUtils.notNull(history.getId(), "创建支付工具绑定历史失败");
    }

    private String snapshotBinding(PaymentInstrumentBinding binding) {
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

    private PaymentInstrumentBinding copyBinding(PaymentInstrumentBinding source) {
        PaymentInstrumentBinding result = new PaymentInstrumentBinding();
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
}
