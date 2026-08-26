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
 * SpendControlMovement 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpendControlMovementConverter {

    SpendControlMovementConverter INSTANCE = Mappers.getMapper(SpendControlMovementConverter.class);

    /**
     * 将 RecordSpendControlMovementRequest 转换为 SpendControlMovement。
     *
     * @param request 记录请求
     * @return SpendControlMovement 实例
     */
    @Mapping(target = "targetSubjectId", ignore = true)
    @Mapping(target = "targetSubjectType", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    SpendControlMovement convertToSpendControlMovement(RecordSpendControlMovementRequest request);

    /**
     * 将 SpendControlMovement 转换为 SpendControlMovementDTO。
     *
     * @param data SpendControlMovement 实例
     * @return SpendControlMovementDTO 实例
     */
    @Mapping(target = "targetAccountId", ignore = true)
    SpendControlMovementDTO convertToSpendControlMovementDTO(SpendControlMovement data);

    /**
     * 在同名字段映射后补齐目标主体字段。
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
     * 在同名字段映射后补齐账户标识。
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
     * 通过显式校验解析 FundsSubjectType。
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
