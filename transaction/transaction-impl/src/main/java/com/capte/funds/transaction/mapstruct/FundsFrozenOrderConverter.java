package com.capte.funds.transaction.mapstruct;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * FundsFrozenOrder model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundsFrozenOrderConverter {

    FundsFrozenOrderConverter INSTANCE = Mappers.getMapper(FundsFrozenOrderConverter.class);

    /**
     * CreateFundsFrozenOrderRequest convert to FundsFrozenOrder.
     *
     * @param request 创建请求
     * @return FundsFrozenOrder 实例
     */
    FundsFrozenOrder convertToFundsFrozenOrder(CreateFundsFrozenOrderRequest request);

    /**
     * FundsFrozenOrder convert to FundsFrozenOrderDTO.
     *
     * @param data FundsFrozenOrder 实例
     * @return FundsFrozenOrderDTO 实例
     */
    FundsFrozenOrderDTO convertToFundsFrozenOrderDTO(FundsFrozenOrder data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 资金冻结订单实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateFundsFrozenOrderRequest request, @MappingTarget FundsFrozenOrder entity) {
        entity.setReleasedAmount(0L);
        entity.setConsumedAmount(0L);
        entity.setStatus(request.getStatus() == null
                ? resolveInitialStatus(request)
                : request.getStatus());
    }

    private FundsFrozenOrderStatus resolveInitialStatus(CreateFundsFrozenOrderRequest request) {
        return hasText(request.getFreezeLedgerTransactionSn())
                ? FundsFrozenOrderStatus.FROZEN
                : FundsFrozenOrderStatus.CREATED;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
