package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.SpendControlScope;
import com.wind.funds.wallet.model.dto.SpendControlScopeDTO;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * SpendControlScope 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpendControlScopeConverter {

    SpendControlScopeConverter INSTANCE = Mappers.getMapper(SpendControlScopeConverter.class);

    /**
     * 将 CreateSpendControlScopeRequest 转换为 SpendControlScope。
     *
     * @param request 创建请求
     * @return SpendControlScope 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    SpendControlScope convertToSpendControlScope(CreateSpendControlScopeRequest request);

    /**
     * 将 SpendControlScope 转换为 SpendControlScopeDTO。
     *
     * @param data SpendControlScope 实例
     * @return SpendControlScopeDTO 实例
     */
    SpendControlScopeDTO convertToSpendControlScopeDTO(SpendControlScope data);

    /**
     * 在同名字段映射后补齐创建默认值。
     *
     * @param request 创建请求
     * @param entity 支出控制范围实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateSpendControlScopeRequest request, @MappingTarget SpendControlScope entity) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        entity.setPeriodType(periodType);
        entity.setPeriodId(periodType == AccountBalancePeriodType.LIFETIME
                ? AccountBalancePeriodType.LIFETIME.name() : request.getPeriodId());
        entity.setState(request.getState() == null ? FundsAccountState.ACTIVE : request.getState());
    }
}
