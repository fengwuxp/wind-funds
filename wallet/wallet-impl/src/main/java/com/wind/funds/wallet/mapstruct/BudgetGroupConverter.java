package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.BudgetGroup;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.model.dto.BudgetGroupDTO;
import com.wind.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * BudgetGroup model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetGroupConverter {

    BudgetGroupConverter INSTANCE = Mappers.getMapper(BudgetGroupConverter.class);

    /**
     * CreateBudgetGroupRequest convert to BudgetGroup.
     *
     * @param request 创建请求
     * @return BudgetGroup 实例
     */
    BudgetGroup convertToBudgetGroup(CreateBudgetGroupRequest request);

    /**
     * BudgetGroup convert to BudgetGroupDTO.
     *
     * @param data BudgetGroup 实例
     * @return BudgetGroupDTO 实例
     */
    @Mapping(target = "ledgerIds", ignore = true)
    BudgetGroupDTO convertToBudgetGroupDTO(BudgetGroup data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 预算组实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateBudgetGroupRequest request, @MappingTarget BudgetGroup entity) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        entity.setPeriodType(periodType);
        entity.setPeriodId(periodType == AccountBalancePeriodType.LIFETIME
                ? AccountBalancePeriodType.LIFETIME.name() : request.getPeriodId());
        entity.setLedgerProfileCode(request.getLedgerProfileCode() == null
                ? LedgerProfileCode.BUDGET_BASIC : request.getLedgerProfileCode());
        entity.setLedgerProfileVersion(1);
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }
}
