package com.wind.funds.wallet.mapstruct;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.dal.entities.SpendControlMovement;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;

/**
 * SpendControlMovement model converter.
 *
 * @author Codex
 * @date 2026-06-23
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpendControlMovementConverter {

    SpendControlMovementConverter INSTANCE = Mappers.getMapper(SpendControlMovementConverter.class);

    /**
     * RecordSpendControlMovementRequest convert to SpendControlMovement.
     *
     * @param request 记录请求
     * @return SpendControlMovement 实例
     */
    @Mapping(target = "targetSubjectId", ignore = true)
    @Mapping(target = "targetSubjectType", ignore = true)
    SpendControlMovement convertToSpendControlMovement(RecordSpendControlMovementRequest request);

    /**
     * SpendControlMovement convert to SpendControlMovementDTO.
     *
     * @param data SpendControlMovement 实例
     * @return SpendControlMovementDTO 实例
     */
    @Mapping(target = "targetAccountId", ignore = true)
    SpendControlMovementDTO convertToSpendControlMovementDTO(SpendControlMovement data);

    /**
     * Fill target subject fields after same-name field mapping.
     *
     * @param request 记录请求
     * @param entity 控制额度变动流水实体
     */
    @AfterMapping
    default void fillTargetSubject(RecordSpendControlMovementRequest request,
                                   @MappingTarget SpendControlMovement entity) {
        FundsAccountId targetAccountId = request.getTargetAccountId();
        if (targetAccountId == null) {
            return;
        }
        entity.setTargetSubjectId(targetAccountId.id());
        entity.setTargetSubjectType(resolveSubjectType(targetAccountId));
    }

    /**
     * Fill account id after same-name field mapping.
     *
     * @param data 控制额度变动流水实体
     * @param dto 控制额度变动流水 DTO
     */
    @AfterMapping
    default void fillTargetAccountId(SpendControlMovement data,
                                     @MappingTarget SpendControlMovementDTO dto) {
        if (data.getTargetSubjectId() == null || data.getTargetSubjectType() == null) {
            return;
        }
        dto.setTargetAccountId(FundsAccountId.immutable(data.getTargetSubjectId(), data.getTargetSubjectType()));
    }

    /**
     * Resolve FundsSubjectType with explicit validation.
     *
     * @param accountId 账户标识
     * @return 资金主体类型
     */
    default FundsSubjectType resolveSubjectType(FundsAccountId accountId) {
        boolean matched = Arrays.stream(FundsSubjectType.values())
                .anyMatch(type -> type.name().equals(accountId.type()));
        AssertUtils.isTrue(matched, "控制额度变动目标账户类型非法，targetAccountId = {}", accountId);
        return FundsSubjectType.valueOf(accountId.type());
    }
}
