package com.wind.funds.governance.projection.impl;

import com.wind.funds.governance.dal.entities.FundsTransactionProjection;
import com.wind.funds.governance.dal.mapper.FundsTransactionProjectionMapper;
import com.wind.funds.governance.projection.FundsTransactionProjectionDifference;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionRow;
import com.wind.funds.governance.projection.internal.FundsTransactionProjectionWriter;
import com.wind.jackson.WindJson;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 正式与影子交易投影的生产写入器。
 */
@Component
@AllArgsConstructor
public class DefaultFundsTransactionProjectionWriter implements FundsTransactionProjectionWriter {

    private static final String OFFICIAL_SCOPE = "OFFICIAL";

    private static final String SHADOW_SCOPE = "SHADOW";

    private static final String OFFICIAL_SCOPE_REF = "OFFICIAL";

    private final FundsTransactionProjectionMapper projectionMapper;

    @Override
    public @NonNull List<FundsTransactionProjectionDifference> compare(
            @NonNull Long tenantId,
            @NonNull String viewDomain,
            @NonNull List<FundsTransactionProjectionRow> rebuiltRows) {
        List<FundsTransactionProjectionDifference> result = new ArrayList<>();
        for (FundsTransactionProjectionRow row : rebuiltRows) {
            FundsTransactionProjection current = projectionMapper.selectProjection(tenantId, viewDomain,
                    OFFICIAL_SCOPE, OFFICIAL_SCOPE_REF, row.projectionSn());
            if (current == null) {
                result.add(difference(row.sourceSn(), "projection", canonical(row.payload()), null));
                continue;
            }
            addDifference(result, row.sourceSn(), "ownerType", row.ownerType(), current.getOwnerType());
            addDifference(result, row.sourceSn(), "ownerId", row.ownerId(), current.getOwnerId());
            addDifference(result, row.sourceSn(), "sourceSn", row.sourceSn(), current.getSourceSn());
            addDifference(result, row.sourceSn(), "displayType", row.displayType(), current.getDisplayType());
            addDifference(result, row.sourceSn(), "displayStatus", row.displayStatus(), current.getDisplayStatus());
            addDifference(result, row.sourceSn(), "amount", row.amount(), current.getAmount());
            addDifference(result, row.sourceSn(), "currency", row.currency(), current.getCurrency());
            addDifference(result, row.sourceSn(), "occurredTime", row.occurredTime(), current.getOccurredTime());
            addDifference(result, row.sourceSn(), "payload", canonical(row.payload()), current.getPayloadJson());
        }
        return List.copyOf(result);
    }

    @Override
    public void upsertShadow(@NonNull Long tenantId,
                             @NonNull String taskSn,
                             @NonNull List<FundsTransactionProjectionRow> rebuiltRows) {
        upsert(tenantId, SHADOW_SCOPE, taskSn, taskSn, rebuiltRows);
    }

    @Override
    public void upsertOfficial(@NonNull Long tenantId,
                               @NonNull String taskSn,
                               @NonNull List<FundsTransactionProjectionRow> rebuiltRows) {
        upsert(tenantId, OFFICIAL_SCOPE, OFFICIAL_SCOPE_REF, taskSn, rebuiltRows);
    }

    private void upsert(Long tenantId,
                        String projectionScope,
                        String scopeRef,
                        String taskSn,
                        List<FundsTransactionProjectionRow> rows) {
        for (FundsTransactionProjectionRow row : rows) {
            FundsTransactionProjection entity = projectionMapper.selectProjection(tenantId, row.viewDomain(),
                    projectionScope, scopeRef, row.projectionSn());
            if (entity == null) {
                entity = new FundsTransactionProjection();
                entity.setTenantId(tenantId);
                entity.setViewDomain(row.viewDomain());
                entity.setProjectionScope(projectionScope);
                entity.setScopeRef(scopeRef);
                entity.setProjectionSn(row.projectionSn());
                copy(row, taskSn, entity);
                projectionMapper.insertSelective(entity);
            } else {
                copy(row, taskSn, entity);
                projectionMapper.update(entity);
            }
        }
    }

    private void copy(FundsTransactionProjectionRow row,
                      String taskSn,
                      FundsTransactionProjection entity) {
        entity.setOwnerType(row.ownerType());
        entity.setOwnerId(row.ownerId());
        entity.setSourceSn(row.sourceSn());
        entity.setDisplayType(row.displayType());
        entity.setDisplayStatus(row.displayStatus());
        entity.setAmount(row.amount());
        entity.setCurrency(row.currency());
        entity.setOccurredTime(row.occurredTime());
        entity.setPayloadJson(canonical(row.payload()));
        entity.setReplayTaskSn(taskSn);
    }

    private void addDifference(List<FundsTransactionProjectionDifference> differences,
                               String sourceSn,
                               String fieldName,
                               Object expectedValue,
                               Object actualValue) {
        if (!Objects.equals(expectedValue, actualValue)) {
            differences.add(difference(sourceSn, fieldName, expectedValue, actualValue));
        }
    }

    private FundsTransactionProjectionDifference difference(String sourceSn,
                                                            String fieldName,
                                                            Object expectedValue,
                                                            Object actualValue) {
        return FundsTransactionProjectionDifference.builder()
                .sourceSn(sourceSn)
                .fieldName(fieldName)
                .expectedValue(expectedValue)
                .actualValue(actualValue)
                .build();
    }

    private String canonical(Object value) {
        return WindJson.getJsonMapper()
                .writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(value);
    }
}
