package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.PaymentInstrument;
import com.capte.funds.transaction.dal.entities.PaymentInstrumentBinding;
import com.capte.funds.transaction.dal.entities.table.PaymentInstrumentBindingNameRefs;
import com.capte.funds.transaction.dal.entities.table.PaymentInstrumentNameRefs;
import com.capte.funds.transaction.dal.mapper.PaymentInstrumentBindingMapper;
import com.capte.funds.transaction.dal.mapper.PaymentInstrumentMapper;
import com.capte.funds.transaction.mapstruct.PaymentInstrumentConverter;
import com.capte.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.capte.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.capte.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.capte.funds.wallet.model.query.PaymentInstrumentQuery;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.capte.funds.wallet.service.PaymentInstrumentService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付工具服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentServiceImpl implements PaymentInstrumentService {

    private final PaymentInstrumentMapper paymentInstrumentMapper;

    private final PaymentInstrumentBindingMapper paymentInstrumentBindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrument(@NonNull CreatePaymentInstrumentRequest request) {
        PaymentInstrument entity = PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrument(request);
        paymentInstrumentMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具失败");
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request) {
        PaymentInstrumentBinding entity =
                PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBinding(request);
        paymentInstrumentBindingMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支付工具绑定失败");
        return entity.getId();
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

    private PaymentInstrumentDTO toDTO(PaymentInstrument entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentDTO(entity);
    }

    private PaymentInstrumentBindingDTO toDTO(PaymentInstrumentBinding entity) {
        return PaymentInstrumentConverter.INSTANCE.convertToPaymentInstrumentBindingDTO(entity);
    }
}
