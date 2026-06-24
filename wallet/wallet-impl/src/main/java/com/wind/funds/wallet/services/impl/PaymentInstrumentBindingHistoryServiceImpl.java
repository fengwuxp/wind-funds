package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBindingHistory;
import com.wind.funds.wallet.dal.entities.table.PaymentInstrumentBindingHistoryNameRefs;
import com.wind.funds.wallet.dal.mapper.PaymentInstrumentBindingHistoryMapper;
import com.wind.funds.wallet.mapstruct.PaymentInstrumentConverter;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.request.RecordPaymentInstrumentBindingHistoryRequest;
import com.wind.funds.wallet.service.PaymentInstrumentBindingHistoryService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付工具绑定历史基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentBindingHistoryServiceImpl implements PaymentInstrumentBindingHistoryService {

    private static final WindSequenceType PAYMENT_INSTRUMENT_BINDING_HISTORY_SEQUENCE_TYPE =
            WindSequenceType.immutable("PAYMENT_INSTRUMENT_BINDING_HISTORY", "PIBH", 6);

    private final PaymentInstrumentBindingHistoryMapper paymentInstrumentBindingHistoryMapper;

    @Override
    public @NonNull Long recordPaymentInstrumentBindingHistory(
            @NonNull RecordPaymentInstrumentBindingHistoryRequest request) {
        PaymentInstrumentBindingHistory entity = toEntity(request);
        paymentInstrumentBindingHistoryMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具绑定历史失败");
        return entity.getId();
    }

    @Override
    public @NonNull PaymentInstrumentBindingHistoryDTO getPaymentInstrumentBindingHistoryById(@NonNull Long id) {
        PaymentInstrumentBindingHistory result = paymentInstrumentBindingHistoryMapper.selectOneById(id);
        AssertUtils.notNull(result, "支付工具绑定历史不存在，id = {}", id);
        return toDTO(result);
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

    private PaymentInstrumentBindingHistory toEntity(RecordPaymentInstrumentBindingHistoryRequest request) {
        PaymentInstrumentBindingHistory result = new PaymentInstrumentBindingHistory();
        result.setSn(TemporalSequenceFactory.hourNext(PAYMENT_INSTRUMENT_BINDING_HISTORY_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setBindingSn(request.getBindingSn());
        result.setInstrumentSn(request.getInstrumentSn());
        result.setChangeType(request.getChangeType());
        result.setVersion(request.getVersion());
        result.setBeforeSnapshot(request.getBeforeSnapshot());
        result.setAfterSnapshot(request.getAfterSnapshot());
        result.setOperatorId(request.getOperatorId());
        result.setChangeReason(request.getChangeReason());
        result.setEffectiveAt(request.getEffectiveAt() == null ? LocalDateTime.now() : request.getEffectiveAt());
        result.setRequestSn(request.getRequestSn());
        result.setContextVariables(request.getContextVariables());
        return result;
    }

    private PaymentInstrumentBindingHistoryDTO toDTO(PaymentInstrumentBindingHistory entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingHistoryDTO(entity);
    }
}
