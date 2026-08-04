package com.wind.funds.governance.projection.impl;

import com.wind.funds.governance.enums.ProjectionCheckpointType;
import com.wind.funds.governance.projection.FundsTransactionProjectionCheckpoint;
import com.wind.funds.governance.projection.FundsTransactionProjectionFact;
import com.wind.funds.governance.projection.FundsTransactionProjectionFactBatch;
import com.wind.funds.governance.projection.FundsTransactionProjectionReplayRange;
import com.wind.funds.governance.projection.FundsTransactionProjectionReplaySource;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanBatch;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanCursor;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanQuery;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 从交易公共扫描契约读取持久事实的生产重放来源。
 */
@Component
@AllArgsConstructor
public class DefaultFundsTransactionProjectionReplaySource implements FundsTransactionProjectionReplaySource {

    private static final int INITIAL_BATCH_SIZE = 1;

    private final FundsTransactionProjectionExplainApplicationService projectionExplainApplicationService;

    @Override
    public @NonNull List<FundsTransactionProjectionFact> loadFacts(
            @NonNull FundsTransactionProjectionReplayRange range) {
        throw new IllegalStateException("生产投影重放来源必须使用 tenant 和 checkpoint 有界读取");
    }

    @Override
    public @NonNull FundsTransactionProjectionCheckpoint initializeCheckpoint(
            @NonNull Long tenantId,
            @NonNull String viewDomain,
            @NonNull FundsTransactionProjectionReplayRange range) {
        FundsTransactionProjectionScanCursor cursor = projectionExplainApplicationService.initializeScanCursor(
                scanQuery(tenantId, range, null, INITIAL_BATCH_SIZE));
        return checkpoint(cursor);
    }

    @Override
    public @NonNull FundsTransactionProjectionFactBatch loadFactBatch(
            @NonNull Long tenantId,
            @NonNull String viewDomain,
            @NonNull FundsTransactionProjectionReplayRange range,
            @NonNull FundsTransactionProjectionCheckpoint checkpoint,
            int maxBatchSize) {
        FundsTransactionProjectionScanCursor cursor = FundsTransactionProjectionScanCursor.parse(
                checkpoint.checkpointSn());
        FundsTransactionProjectionScanBatch batch = projectionExplainApplicationService.scan(
                scanQuery(tenantId, range, cursor, maxBatchSize));
        return FundsTransactionProjectionFactBatch.builder()
                .facts(batch.facts().stream().map(fact -> toFact(viewDomain, fact)).toList())
                .nextCheckpoint(checkpoint(batch.nextCursor()))
                .hasMore(batch.hasMore())
                .build();
    }

    private FundsTransactionProjectionScanQuery scanQuery(Long tenantId,
                                                          FundsTransactionProjectionReplayRange range,
                                                          FundsTransactionProjectionScanCursor cursor,
                                                          int maxBatchSize) {
        return FundsTransactionProjectionScanQuery.builder()
                .tenantId(tenantId)
                .eventTypes(Set.of())
                .sourceSn(range.sourceSn())
                .ownerType(range.ownerType())
                .ownerId(range.ownerId())
                .startTime(range.startTime())
                .endTime(range.endTime())
                .cursor(cursor)
                .maxBatchSize(maxBatchSize)
                .build();
    }

    private FundsTransactionProjectionCheckpoint checkpoint(FundsTransactionProjectionScanCursor cursor) {
        return FundsTransactionProjectionCheckpoint.builder()
                .type(ProjectionCheckpointType.TRANSACTION_PROJECTION)
                .checkpointSn(cursor.checkpointValue())
                .build();
    }

    private FundsTransactionProjectionFact toFact(String viewDomain,
                                                  FundsTransactionProjectionExplanation explanation) {
        return FundsTransactionProjectionFact.builder()
                .viewDomain(viewDomain)
                .ownerType(explanation.ownerType())
                .ownerId(explanation.ownerId())
                .sourceSn(explanation.fundsTransactionSn())
                .displayType(explanation.eventType().name())
                .displayStatus(explanation.displayStatus())
                .amount(explanation.amount())
                .currency(explanation.currency().name())
                .occurredTime(explanation.occurredTime())
                .payload(explanation.payload())
                .build();
    }
}
