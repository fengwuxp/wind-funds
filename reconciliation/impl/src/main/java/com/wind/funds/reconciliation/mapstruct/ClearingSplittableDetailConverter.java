package com.wind.funds.reconciliation.mapstruct;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 可清分明细模型转换器。
 */
@Mapper
public interface ClearingSplittableDetailConverter {

    ClearingSplittableDetailConverter INSTANCE = Mappers.getMapper(ClearingSplittableDetailConverter.class);

    @Mapping(target = "createdTime", source = "gmtCreate")
    @Mapping(target = "reconciliationEvidenceRefs",
            expression = "java(parseEvidenceRefs(source.getReconciliationEvidenceRefs()))")
    ClearingSplittableDetailDTO toDTO(ClearingSplittableDetail source);

    default List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(JSON.parseArray(value, String.class)) : List.of();
    }
}
