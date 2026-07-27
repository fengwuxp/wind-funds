package com.wind.funds.reconciliation.mapstruct;

import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 对账逐笔匹配结果模型转换器。
 */
@Mapper
public interface ReconciliationMatchResultConverter {

    ReconciliationMatchResultConverter INSTANCE = Mappers.getMapper(ReconciliationMatchResultConverter.class);

    @Mapping(target = "createdTime", source = "gmtCreate")
    ReconciliationMatchResultDTO toDTO(ReconciliationMatchResult source);
}
