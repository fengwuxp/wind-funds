package com.wind.funds.reconciliation.mapstruct;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 对账运行结果模型转换器。
 */
@Mapper
public interface ReconciliationRunResultConverter {

    ReconciliationRunResultConverter INSTANCE = Mappers.getMapper(ReconciliationRunResultConverter.class);

    @Mapping(target = "createdTime", source = "gmtCreate")
    @Mapping(target = "internalSourceDigest", source = "internalSourceDigest")
    @Mapping(target = "externalSourceDigest", source = "externalSourceDigest")
    @Mapping(target = "evidenceRefs", expression = "java(parseEvidenceRefs(source.getEvidenceRefs()))")
    ReconciliationRunResultDTO toDTO(ReconciliationRunResult source);

    default List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(JSON.parseArray(value, String.class)) : List.of();
    }
}
