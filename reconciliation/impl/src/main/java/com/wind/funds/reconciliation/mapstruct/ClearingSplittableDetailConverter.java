package com.wind.funds.reconciliation.mapstruct;

import com.wind.jackson.WindJson;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 可清分明细模型转换器。
 *
 * @author wuxp
 * @since 2026-07-21
 */
@Mapper
public interface ClearingSplittableDetailConverter {

    ClearingSplittableDetailConverter INSTANCE = Mappers.getMapper(ClearingSplittableDetailConverter.class);

    @Mapping(target = "createdTime", source = "gmtCreate")
    @Mapping(target = "reconciliationDecisionResult", source = "reconciliationDecisionResult")
    @Mapping(target = "reconciliationEvidenceRefs",
            expression = "java(parseEvidenceRefs(source.getReconciliationEvidenceRefs()))")
    ClearingSplittableDetailDTO toDTO(ClearingSplittableDetail source);

    default List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }
}
