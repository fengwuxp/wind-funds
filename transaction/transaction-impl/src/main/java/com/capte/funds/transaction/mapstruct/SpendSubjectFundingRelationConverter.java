package com.capte.funds.transaction.mapstruct;

import com.capte.funds.transaction.dal.entities.SpendSubjectFundingRel;
import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * SpendSubjectFundingRelation model converter.
 *
 * @author Codex
 * @date 2026-05-08
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpendSubjectFundingRelationConverter {

    SpendSubjectFundingRelationConverter INSTANCE = Mappers.getMapper(SpendSubjectFundingRelationConverter.class);

    /**
     * CreateSpendSubjectFundingRelationRequest convert to SpendSubjectFundingRel.
     *
     * @param request 创建请求
     * @return SpendSubjectFundingRel 实例
     */
    SpendSubjectFundingRel convertToSpendSubjectFundingRel(CreateSpendSubjectFundingRelationRequest request);

    /**
     * SpendSubjectFundingRel convert to SpendSubjectFundingRelationDTO.
     *
     * @param data SpendSubjectFundingRel 实例
     * @return SpendSubjectFundingRelationDTO 实例
     */
    SpendSubjectFundingRelationDTO convertToSpendSubjectFundingRelationDTO(SpendSubjectFundingRel data);

    /**
     * Fill create defaults after same-name field mapping.
     *
     * @param request 创建请求
     * @param entity 支出主体资金关系实体
     */
    @AfterMapping
    default void fillCreateDefaults(CreateSpendSubjectFundingRelationRequest request,
                                    @MappingTarget SpendSubjectFundingRel entity) {
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setDefaultRelation(Boolean.TRUE.equals(request.getDefaultRelation()));
        entity.setStatus(request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus());
    }
}
