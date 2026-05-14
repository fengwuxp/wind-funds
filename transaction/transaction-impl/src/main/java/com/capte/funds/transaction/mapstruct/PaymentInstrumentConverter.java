package com.capte.funds.transaction.mapstruct;

import com.capte.funds.transaction.dal.entities.PaymentInstrument;
import com.capte.funds.transaction.dal.entities.PaymentInstrumentBinding;
import com.capte.funds.transaction.model.dto.PaymentInstrumentBindingDTO;
import com.capte.funds.transaction.model.dto.PaymentInstrumentDTO;
import com.capte.funds.transaction.model.request.CreatePaymentInstrumentBindingRequest;
import com.capte.funds.transaction.model.request.CreatePaymentInstrumentRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * PaymentInstrument model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentInstrumentConverter {

    PaymentInstrumentConverter INSTANCE = Mappers.getMapper(PaymentInstrumentConverter.class);

    /**
     * CreatePaymentInstrumentRequest convert to PaymentInstrument.
     *
     * @param request 创建请求
     * @return PaymentInstrument 实例
     */
    PaymentInstrument convertToPaymentInstrument(CreatePaymentInstrumentRequest request);

    /**
     * CreatePaymentInstrumentBindingRequest convert to PaymentInstrumentBinding.
     *
     * @param request 创建请求
     * @return PaymentInstrumentBinding 实例
     */
    PaymentInstrumentBinding convertToPaymentInstrumentBinding(CreatePaymentInstrumentBindingRequest request);

    /**
     * PaymentInstrument convert to PaymentInstrumentDTO.
     *
     * @param data PaymentInstrument 实例
     * @return PaymentInstrumentDTO 实例
     */
    PaymentInstrumentDTO convertToPaymentInstrumentDTO(PaymentInstrument data);

    /**
     * PaymentInstrumentBinding convert to PaymentInstrumentBindingDTO.
     *
     * @param data PaymentInstrumentBinding 实例
     * @return PaymentInstrumentBindingDTO 实例
     */
    PaymentInstrumentBindingDTO convertToPaymentInstrumentBindingDTO(PaymentInstrumentBinding data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 支付工具实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreatePaymentInstrumentRequest request, @MappingTarget PaymentInstrument entity) {
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }

    /**
     * Fill binding create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 支付工具绑定实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreatePaymentInstrumentBindingRequest request,
                                    @MappingTarget PaymentInstrumentBinding entity) {
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setDefaultBinding(Boolean.TRUE.equals(request.getDefaultBinding()));
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }
}
