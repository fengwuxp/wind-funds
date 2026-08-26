package com.wind.funds.wallet.mapstruct;

import com.wind.funds.wallet.dal.entities.AccountHierarchyRelation;
import com.wind.funds.wallet.model.dto.AccountHierarchyRelationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 账户层级关系模型转换器。
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountHierarchyRelationConverter {

    AccountHierarchyRelationConverter INSTANCE = Mappers.getMapper(AccountHierarchyRelationConverter.class);

    AccountHierarchyRelationDTO convertToAccountHierarchyRelationDTO(AccountHierarchyRelation data);
}
