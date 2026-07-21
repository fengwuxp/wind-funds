package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.SpendControlScope;
import com.wind.funds.wallet.model.dto.SpendControlScopeDTO;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * SpendControlScope model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpendControlScopeConverter {

    SpendControlScopeConverter INSTANCE = Mappers.getMapper(SpendControlScopeConverter.class);

    /**
     * CreateSpendControlScopeRequest convert to SpendControlScope.
     *
     * @param request 创建请求
     * @return SpendControlScope 实例
     */
    SpendControlScope convertToSpendControlScope(CreateSpendControlScopeRequest request);

    /**
     * SpendControlScope convert to SpendControlScopeDTO.
     *
     * @param data SpendControlScope 实例
     * @return SpendControlScopeDTO 实例
     */
    SpendControlScopeDTO convertToSpendControlScopeDTO(SpendControlScope data);

    /**
     * Fill create defaults after same-name field mapping.
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
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }
}
