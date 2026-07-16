package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.wind.funds.wallet.dal.entities.table.PaymentInstrumentBindingNameRefs;
import com.wind.funds.wallet.dal.entities.table.PaymentInstrumentNameRefs;
import com.wind.funds.wallet.dal.mapper.PaymentInstrumentBindingMapper;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingState;
import com.wind.funds.wallet.mapstruct.PaymentInstrumentConverter;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.UpdatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.service.PaymentInstrumentBindingService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付工具绑定基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentBindingServiceImpl implements PaymentInstrumentBindingService {

    private static final WindSequenceType PAYMENT_INSTRUMENT_BINDING_SEQUENCE_TYPE =
            WindSequenceType.immutable("PAYMENT_INSTRUMENT_BINDING", "PIB", 6);

    private final PaymentInstrumentBindingMapper paymentInstrumentBindingMapper;

    @Override
    public @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request) {
        PaymentInstrumentBinding entity =
                PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBinding(request);
        entity.setSn(TemporalSequenceFactory.hourNext(PAYMENT_INSTRUMENT_BINDING_SEQUENCE_TYPE));
        paymentInstrumentBindingMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具绑定失败");
        return entity.getId();
    }

    @Override
    public @NonNull PaymentInstrumentBindingDTO getPaymentInstrumentBindingById(@NonNull Long id) {
        PaymentInstrumentBinding result = paymentInstrumentBindingMapper.selectOneById(id);
        AssertUtils.notNull(result, "支付工具绑定不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull PaymentInstrumentBindingDTO getPaymentInstrumentBinding(@NonNull Long tenantId,
                                                                            @NonNull String bindingSn) {
        AssertUtils.notNull(tenantId, "租户 ID 不能为空");
        AssertUtils.hasText(bindingSn, "支付工具绑定号不能为空");
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        PaymentInstrumentBinding result = paymentInstrumentBindingMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(bindingSn)));
        AssertUtils.notNull(result, "支付工具绑定不存在，bindingSn = {}", bindingSn);
        return toDTO(result);
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
                .and(ref.state.eq(query.getState()));
        applyCurrentEffectiveWindow(wrapper, ref, query.getState());
        applyActiveInstrumentWindow(wrapper, query);
        wrapper.orderBy(ref.priority.asc(), ref.id.asc());
        return MybatisQueryHelper.<PaymentInstrumentBinding, PaymentInstrumentBindingDTO>query(wrapper)
                .counter(paymentInstrumentBindingMapper::selectCountByQuery)
                .resultQueryFunc(paymentInstrumentBindingMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    @Override
    public @NonNull Long updatePaymentInstrumentBinding(@NonNull UpdatePaymentInstrumentBindingRequest request) {
        AssertUtils.notNull(request.getId(), "支付工具绑定主键不能为空");
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getBindingSn(), "支付工具绑定号不能为空");
        AssertUtils.notNull(request.getExpectedVersion(), "支付工具绑定期望版本不能为空");
        AssertUtils.notNull(request.getNextVersion(), "支付工具绑定更新后版本不能为空");
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        PaymentInstrumentBinding entity = UpdateEntity.of(PaymentInstrumentBinding.class);
        UpdateWrapper<PaymentInstrumentBinding> updateWrapper = UpdateWrapper.of(entity);
        updateWrapper.set(ref.priority, request.getPriority(), request.getPriority() != null);
        updateWrapper.set(ref.defaultBinding, request.getDefaultBinding(), request.getDefaultBinding() != null);
        updateWrapper.set(ref.state, request.getState(), request.getState() != null);
        updateWrapper.set(ref.validFrom, request.getValidFrom(), request.getValidFrom() != null);
        updateWrapper.set(ref.validTo, request.getValidTo(), request.getValidTo() != null);
        updateWrapper.set(ref.description, request.getDescription(), request.getDescription() != null);
        updateWrapper.set(ref.contextVariables, request.getContextVariables(), request.getContextVariables() != null);
        updateWrapper.set(ref.version, request.getNextVersion(), true);
        AssertUtils.isTrue(paymentInstrumentBindingMapper.updateByQuery(entity, QueryWrapper.create()
                        .where(ref.id.eq(request.getId()))
                        .and(ref.tenantId.eq(request.getTenantId()))
                        .and(ref.sn.eq(request.getBindingSn()))
                        .and(ref.version.eq(request.getExpectedVersion()))) == 1,
                "支付工具绑定已变更，请重试，bindingSn = {}",
                request.getBindingSn());
        return request.getId();
    }

    @Override
    public void deletePaymentInstrumentBinding(@NonNull Long tenantId,
                                               @NonNull String bindingSn,
                                               @NonNull Integer expectedVersion) {
        AssertUtils.notNull(tenantId, "租户 ID 不能为空");
        AssertUtils.hasText(bindingSn, "支付工具绑定号不能为空");
        AssertUtils.notNull(expectedVersion, "支付工具绑定期望版本不能为空");
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        AssertUtils.isTrue(paymentInstrumentBindingMapper.deleteByQuery(QueryWrapper.create()
                        .where(ref.tenantId.eq(tenantId))
                        .and(ref.sn.eq(bindingSn))
                        .and(ref.version.eq(expectedVersion))) == 1,
                "支付工具绑定已变更，请重试，bindingSn = {}",
                bindingSn);
    }

    @Override
    public boolean existsOverlappingActiveDefaultBinding(@NonNull PaymentInstrumentBindingDTO binding) {
        if (!Boolean.TRUE.equals(binding.getDefaultBinding())
                || binding.getState() != PaymentInstrumentBindingState.ACTIVE) {
            return false;
        }
        PaymentInstrumentBindingNameRefs ref = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(binding.getTenantId()))
                .and(ref.instrumentSn.eq(binding.getInstrumentSn()))
                .and(ref.bindingRole.eq(binding.getBindingRole()))
                .and(ref.currency.eq(binding.getCurrency()))
                .and(ref.defaultBinding.eq(Boolean.TRUE))
                .and(ref.state.eq(PaymentInstrumentBindingState.ACTIVE));
        return paymentInstrumentBindingMapper.selectListByQuery(wrapper).stream()
                .filter(existing -> binding.getId() == null || !binding.getId().equals(existing.getId()))
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        binding.getValidFrom(),
                        binding.getValidTo()));
    }

    @Override
    public boolean existsOverlappingActivePriorityBinding(@NonNull PaymentInstrumentBindingDTO binding) {
        if (binding.getState() != PaymentInstrumentBindingState.ACTIVE) {
            return false;
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
                .and(ref.state.eq(PaymentInstrumentBindingState.ACTIVE));
        return paymentInstrumentBindingMapper.selectListByQuery(wrapper).stream()
                .filter(existing -> binding.getId() == null || !binding.getId().equals(existing.getId()))
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        binding.getValidFrom(),
                        binding.getValidTo()));
    }

    private void applyCurrentEffectiveWindow(QueryWrapper wrapper,
                                             PaymentInstrumentBindingNameRefs ref,
                                             PaymentInstrumentBindingState state) {
        if (state != PaymentInstrumentBindingState.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(ref.validFrom.isNull().or(ref.validFrom.le(now)))
                .and(ref.validTo.isNull().or(ref.validTo.gt(now)));
    }

    private void applyActiveInstrumentWindow(QueryWrapper wrapper, PaymentInstrumentBindingQuery query) {
        if (query.getState() != PaymentInstrumentBindingState.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        PaymentInstrumentNameRefs instrumentRef = PaymentInstrumentNameRefs.paymentInstrument;
        QueryWrapper activeInstrument = QueryWrapper.create()
                .select(instrumentRef.sn)
                .from(instrumentRef)
                .where(instrumentRef.tenantId.eq(query.getTenantId()))
                .and(instrumentRef.status.eq(FundsAccountStatus.ACTIVE))
                .and(instrumentRef.validFrom.isNull().or(instrumentRef.validFrom.le(now)))
                .and(instrumentRef.validTo.isNull().or(instrumentRef.validTo.gt(now)));
        PaymentInstrumentBindingNameRefs bindingRef = PaymentInstrumentBindingNameRefs.paymentInstrumentBinding;
        wrapper.in(bindingRef.instrumentSn.getName(), activeInstrument);
    }

    private boolean validityWindowsOverlap(LocalDateTime leftFrom,
                                           LocalDateTime leftTo,
                                           LocalDateTime rightFrom,
                                           LocalDateTime rightTo) {
        boolean leftEndsAfterRightStarts = leftTo == null || rightFrom == null || leftTo.isAfter(rightFrom);
        boolean rightEndsAfterLeftStarts = rightTo == null || leftFrom == null || rightTo.isAfter(leftFrom);
        return leftEndsAfterRightStarts && rightEndsAfterLeftStarts;
    }

    private PaymentInstrumentBindingDTO toDTO(PaymentInstrumentBinding entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingDTO(entity);
    }
}
