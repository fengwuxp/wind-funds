package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.PaymentInstrument;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBindingHistory;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingState;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * PaymentInstrument 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentInstrumentConverter {

    PaymentInstrumentConverter INSTANCE = Mappers.getMapper(PaymentInstrumentConverter.class);

    /**
     * 将 CreatePaymentInstrumentRequest 转换为 PaymentInstrument。
     *
     * @param request 创建请求
     * @return PaymentInstrument 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    PaymentInstrument convertToPaymentInstrument(CreatePaymentInstrumentRequest request);

    /**
     * 将 CreatePaymentInstrumentBindingRequest 转换为 PaymentInstrumentBinding。
     *
     * @param request 创建请求
     * @return PaymentInstrumentBinding 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "version", ignore = true)
    PaymentInstrumentBinding convertToPaymentInstrumentBinding(CreatePaymentInstrumentBindingRequest request);

    /**
     * 将 PaymentInstrument 转换为 PaymentInstrumentDTO。
     *
     * @param data PaymentInstrument 实例
     * @return PaymentInstrumentDTO 实例
     */
    PaymentInstrumentDTO convertToPaymentInstrumentDTO(PaymentInstrument data);

    /**
     * 将 PaymentInstrumentBinding 转换为 PaymentInstrumentBindingDTO。
     *
     * @param data PaymentInstrumentBinding 实例
     * @return PaymentInstrumentBindingDTO 实例
     */
    PaymentInstrumentBindingDTO convertToPaymentInstrumentBindingDTO(PaymentInstrumentBinding data);

    /**
     * 将 PaymentInstrumentBindingHistory 转换为 PaymentInstrumentBindingHistoryDTO。
     *
     * @param data PaymentInstrumentBindingHistory 实例
     * @return PaymentInstrumentBindingHistoryDTO 实例
     */
    PaymentInstrumentBindingHistoryDTO convertToPaymentInstrumentBindingHistoryDTO(PaymentInstrumentBindingHistory data);

    /**
     * 在同名字段映射后补齐创建默认值。
     *
     * @param request 创建请求
     * @param entity 支付工具实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreatePaymentInstrumentRequest request, @MappingTarget PaymentInstrument entity) {
        entity.setState(request.getState() == null ? FundsAccountState.ACTIVE : request.getState());
    }

    /**
     * 在同名字段映射后补齐绑定创建默认值。
     *
     * @param request 创建请求
     * @param entity 支付工具绑定实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreatePaymentInstrumentBindingRequest request,
                                    @MappingTarget PaymentInstrumentBinding entity) {
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setDefaultBinding(Boolean.TRUE.equals(request.getDefaultBinding()));
        entity.setState(request.getState() == null ? PaymentInstrumentBindingState.ACTIVE : request.getState());
        entity.setVersion(1);
    }
}
