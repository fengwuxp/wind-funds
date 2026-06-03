package com.wind.funds.transaction.mapstruct;

import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.wind.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

/**
 * 资金冻结单模型转换器。
 *
 * <p>职责：在冻结单创建请求、持久化实体和 DTO 之间做字段映射，并补齐冻结单创建默认值。</p>
 *
 * <p>边界：只做同名字段映射和冻结单初始状态默认值处理，不查询数据库、不写账本、不推进资金交易状态。</p>
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FundsFrozenOrderConverter {

    FundsFrozenOrderConverter INSTANCE = Mappers.getMapper(FundsFrozenOrderConverter.class);

    /**
     * 将冻结单创建请求转换为持久化实体。
     *
     * @param request 创建请求
     * @return 资金冻结单实体
     */
    FundsFrozenOrder convertToFundsFrozenOrder(CreateFundsFrozenOrderRequest request);

    /**
     * 将冻结单实体转换为查询 DTO。
     *
     * @param data 资金冻结单实体
     * @return 资金冻结单 DTO
     */
    FundsFrozenOrderDTO convertToFundsFrozenOrderDTO(FundsFrozenOrder data);

    /**
     * 在同名字段映射后补齐冻结单创建默认值。
     *
     * @param request 创建请求
     * @param entity 资金冻结订单实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateFundsFrozenOrderRequest request, @MappingTarget FundsFrozenOrder entity) {
        entity.setReleasedAmount(0L);
        entity.setStatus(request.getStatus() == null
                ? resolveInitialStatus(request)
                : request.getStatus());
    }

    private FundsFrozenOrderStatus resolveInitialStatus(CreateFundsFrozenOrderRequest request) {
        return StringUtils.hasText(request.getFreezeLedgerTransactionSn())
                ? FundsFrozenOrderStatus.FROZEN
                : FundsFrozenOrderStatus.CREATED;
    }
}
