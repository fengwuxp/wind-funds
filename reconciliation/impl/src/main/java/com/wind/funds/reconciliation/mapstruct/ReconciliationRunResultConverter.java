package com.wind.funds.reconciliation.mapstruct;

import com.wind.jackson.WindJson;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
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
    @Mapping(target = "evidenceRefs", expression = "java(parseEvidenceRefs(source.getEvidenceRefs()))")
    @Mapping(target = "scopeIdentity", expression = "java(identity(source.getScopeOwnerNamespace(), source.getScopeIdentityValue()))")
    @Mapping(target = "pairIdentity", expression = "java(identity(source.getPairOwnerNamespace(), source.getPairIdentityValue()))")
    @Mapping(target = "comparisonRuleRef", expression = "java(rule(source.getRuleNamespace(), source.getRuleIdentity(), source.getRuleVersion()))")
    ReconciliationRunResultDTO toDTO(ReconciliationRunResult source);

    default List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    default StableIdentity identity(String ownerNamespace, String value) {
        return new StableIdentity().setOwnerNamespace(ownerNamespace).setValue(value);
    }

    default ComparisonRuleRef rule(String namespace, String identity, String version) {
        return new ComparisonRuleRef().setNamespace(namespace).setIdentity(identity).setVersion(version);
    }
}
