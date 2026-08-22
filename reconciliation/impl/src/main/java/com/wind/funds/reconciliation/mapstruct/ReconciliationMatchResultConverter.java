package com.wind.funds.reconciliation.mapstruct;

import com.wind.jackson.WindJson;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 对账逐笔匹配结果模型转换器。
 */
@Mapper
public interface ReconciliationMatchResultConverter {

    ReconciliationMatchResultConverter INSTANCE = Mappers.getMapper(ReconciliationMatchResultConverter.class);

    @Mapping(target = "createdTime", source = "gmtCreate")
    @Mapping(target = "referenceFactRef", expression = "java(nullableIdentity(source.getReferenceFactOwnerNamespace(), source.getReferenceFactIdentityValue()))")
    @Mapping(target = "comparisonFactRef", expression = "java(nullableIdentity(source.getComparisonFactOwnerNamespace(), source.getComparisonFactIdentityValue()))")
    @Mapping(target = "comparisonIdentity", expression = "java(identity(source.getComparisonOwnerNamespace(), source.getComparisonIdentityValue()))")
    @Mapping(target = "evidenceRefs", expression = "java(parseEvidenceRefs(source.getEvidenceRefs()))")
    ReconciliationMatchResultDTO toDTO(ReconciliationMatchResult source);

    default StableIdentity identity(String ownerNamespace, String value) {
        return new StableIdentity().setOwnerNamespace(ownerNamespace).setValue(value);
    }

    default StableIdentity nullableIdentity(String ownerNamespace, String value) {
        return StringUtils.hasText(ownerNamespace) && StringUtils.hasText(value) ? identity(ownerNamespace, value) : null;
    }

    default List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }
}
