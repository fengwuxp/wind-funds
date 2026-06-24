package com.wind.funds.wallet.mapstruct;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.dal.entities.SpendControlActivity;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;

/**
 * SpendControlActivity model converter.
 *
 * @author Codex
 * @date 2026-06-23
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpendControlActivityConverter {

    SpendControlActivityConverter INSTANCE = Mappers.getMapper(SpendControlActivityConverter.class);

    /**
     * RecordSpendControlActivityRequest convert to SpendControlActivity.
     *
     * @param request 记录请求
     * @return SpendControlActivity 实例
     */
    @Mapping(target = "targetSubjectId", ignore = true)
    @Mapping(target = "targetSubjectType", ignore = true)
    SpendControlActivity convertToSpendControlActivity(RecordSpendControlActivityRequest request);

    /**
     * SpendControlActivity convert to SpendControlActivityDTO.
     *
     * @param data SpendControlActivity 实例
     * @return SpendControlActivityDTO 实例
     */
    @Mapping(target = "targetAccountId", ignore = true)
    SpendControlActivityDTO convertToSpendControlActivityDTO(SpendControlActivity data);

    /**
     * Fill target subject fields after same-name field mapping.
     *
     * @param request 记录请求
     * @param entity 控制额度变动流水实体
     */
    @AfterMapping
    default void fillTargetSubject(RecordSpendControlActivityRequest request,
                                   @MappingTarget SpendControlActivity entity) {
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
    default void fillTargetAccountId(SpendControlActivity data,
                                     @MappingTarget SpendControlActivityDTO dto) {
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
        AssertUtils.isTrue(matched, "控制活动目标账户类型非法，targetAccountId = {}", accountId);
        return FundsSubjectType.valueOf(accountId.type());
    }
}
