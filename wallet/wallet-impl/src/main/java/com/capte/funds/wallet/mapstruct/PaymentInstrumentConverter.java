package com.capte.funds.wallet.mapstruct;

import com.capte.funds.wallet.dal.entities.PaymentInstrument;
import com.capte.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.capte.funds.wallet.dal.entities.PaymentInstrumentBindingHistory;
import com.capte.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.capte.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.capte.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.enums.FundsAccountStatus;
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
     * PaymentInstrumentBindingHistory convert to PaymentInstrumentBindingHistoryDTO.
     *
     * @param data PaymentInstrumentBindingHistory 实例
     * @return PaymentInstrumentBindingHistoryDTO 实例
     */
    PaymentInstrumentBindingHistoryDTO convertToPaymentInstrumentBindingHistoryDTO(PaymentInstrumentBindingHistory data);

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
        entity.setVersion(1);
    }
}
