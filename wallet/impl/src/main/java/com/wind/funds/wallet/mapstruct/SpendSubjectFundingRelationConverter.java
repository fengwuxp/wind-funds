package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.SpendSubjectFundingRel;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * SpendSubjectFundingRelation 模型转换器。
 *
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpendSubjectFundingRelationConverter {

    SpendSubjectFundingRelationConverter INSTANCE = Mappers.getMapper(SpendSubjectFundingRelationConverter.class);

    /**
     * 将 CreateSpendSubjectFundingRelationRequest 转换为 SpendSubjectFundingRel。
     *
     * @param request 创建请求
     * @return SpendSubjectFundingRel 实例
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    @Mapping(target = "sn", ignore = true)
    SpendSubjectFundingRel convertToSpendSubjectFundingRel(CreateSpendSubjectFundingRelationRequest request);

    /**
     * 将 SpendSubjectFundingRel 转换为 SpendSubjectFundingRelationDTO。
     *
     * @param data SpendSubjectFundingRel 实例
     * @return SpendSubjectFundingRelationDTO 实例
     */
    SpendSubjectFundingRelationDTO convertToSpendSubjectFundingRelationDTO(SpendSubjectFundingRel data);
}
