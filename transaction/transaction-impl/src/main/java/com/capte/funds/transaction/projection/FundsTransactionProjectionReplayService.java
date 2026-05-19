package com.capte.funds.transaction.projection;

import com.wind.common.exception.AssertUtils;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 资金交易投影重放服务。
 *
 * <p>该服务只负责重建只读交易投影，不重新入账、不补写交易事实、不修改余额投影。</p>
 */
public class FundsTransactionProjectionReplayService {

    private final FundsTransactionProjectionReplaySource replaySource;

    private final FundsTransactionProjectionWriter projectionWriter;

    public FundsTransactionProjectionReplayService(@NonNull FundsTransactionProjectionReplaySource replaySource,
                                                   @NonNull FundsTransactionProjectionWriter projectionWriter) {
        this.replaySource = replaySource;
        this.projectionWriter = projectionWriter;
    }

    public @NonNull FundsTransactionProjectionReplayResult replay(@NonNull FundsTransactionProjectionReplayRequest request) {
        assertRequestValid(request);
        List<FundsTransactionProjectionFact> facts = replaySource.loadFacts(request.replayRange());
        List<FundsTransactionProjectionRow> rebuiltRows = facts.stream()
                .map(this::rebuildProjectionRow)
                .toList();
        List<FundsTransactionProjectionDifference> differences = projectionWriter.compare(request.viewDomain(), rebuiltRows);
        if (request.mode() == FundsTransactionProjectionReplayMode.REBUILD_SHADOW) {
            projectionWriter.upsertShadow(request.taskSn(), rebuiltRows);
        } else if (request.mode() == FundsTransactionProjectionReplayMode.REBUILD_APPLY) {
            projectionWriter.upsertOfficial(request.taskSn(), rebuiltRows);
        }
        return FundsTransactionProjectionReplayResult.builder()
                .taskSn(request.taskSn())
                .mode(request.mode())
                .viewDomain(request.viewDomain())
                .range(request.replayRange())
                .loadedFactCount(facts.size())
                .rebuiltRowCount(rebuiltRows.size())
                .differences(differences)
                .checkpoint(request.checkpoint())
                .build();
    }

    private void assertRequestValid(FundsTransactionProjectionReplayRequest request) {
        AssertUtils.hasText(request.taskSn(), "交易投影重放任务号不能为空");
        AssertUtils.notNull(request.mode(), "交易投影重放模式不能为空");
        AssertUtils.hasText(request.viewDomain(), "交易投影视图域不能为空");
        AssertUtils.notNull(request.replayRange(), "交易投影重放范围不能为空");
        AssertUtils.isTrue(request.replayRange().isBounded(), "交易投影重放必须指定单笔、主体、时间窗口或批次范围");
        AssertUtils.notNull(request.checkpoint(), "交易投影重放 checkpoint 不能为空");
        AssertUtils.isTrue(request.checkpoint().type() == FundsTransactionProjectionCheckpointType.TRANSACTION_PROJECTION,
                "交易投影重放不得复用余额水位、归档 Manifest 或报表 checkpoint");
    }

    private FundsTransactionProjectionRow rebuildProjectionRow(FundsTransactionProjectionFact fact) {
        return FundsTransactionProjectionRow.builder()
                .projectionSn("TP-" + fact.sourceSn())
                .viewDomain(fact.viewDomain())
                .ownerType(fact.ownerType())
                .ownerId(fact.ownerId())
                .sourceSn(fact.sourceSn())
                .displayType(fact.displayType())
                .displayStatus(fact.displayStatus())
                .amount(fact.amount())
                .currency(fact.currency())
                .occurredTime(fact.occurredTime())
                .payload(Map.copyOf(fact.payload()))
                .build();
    }

    public interface FundsTransactionProjectionReplaySource {

        @NonNull
        List<FundsTransactionProjectionFact> loadFacts(@NonNull FundsTransactionProjectionReplayRange range);
    }

    public interface FundsTransactionProjectionWriter {

        @NonNull
        List<FundsTransactionProjectionDifference> compare(@NonNull String viewDomain,
                                                           @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

        void upsertShadow(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

        void upsertOfficial(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);
    }

    public enum FundsTransactionProjectionReplayMode {

        /**
         * 只生成差异报告，不写正式或影子投影。
         */
        VERIFY_ONLY,

        /**
         * 写影子投影，用于灰度核对。
         */
        REBUILD_SHADOW,

        /**
         * 覆盖正式只读投影。
         */
        REBUILD_APPLY
    }

    public enum FundsTransactionProjectionCheckpointType {

        TRANSACTION_PROJECTION,

        BALANCE_WATERMARK,

        ARCHIVE_MANIFEST,

        REPORT_METRIC
    }

    @Builder
    public record FundsTransactionProjectionReplayRequest(@NonNull String taskSn,
                                                          @NonNull FundsTransactionProjectionReplayMode mode,
                                                          @NonNull String viewDomain,
                                                          @NonNull FundsTransactionProjectionReplayRange replayRange,
                                                          @NonNull FundsTransactionProjectionCheckpoint checkpoint) {
    }

    @Builder
    public record FundsTransactionProjectionReplayRange(@Nullable String sourceSn,
                                                        @Nullable String ownerType,
                                                        @Nullable String ownerId,
                                                        @Nullable LocalDateTime startTime,
                                                        @Nullable LocalDateTime endTime,
                                                        @Nullable String batchType,
                                                        @Nullable String batchSn) {

        private boolean isBounded() {
            return hasText(sourceSn)
                    || (hasText(ownerType) && hasText(ownerId))
                    || (startTime != null && endTime != null && startTime.isBefore(endTime))
                    || (hasText(batchType) && hasText(batchSn));
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    @Builder
    public record FundsTransactionProjectionCheckpoint(@NonNull FundsTransactionProjectionCheckpointType type,
                                                       @NonNull String checkpointSn) {
    }

    @Builder
    public record FundsTransactionProjectionFact(@NonNull String viewDomain,
                                                 @NonNull String ownerType,
                                                 @NonNull String ownerId,
                                                 @NonNull String sourceSn,
                                                 @NonNull String displayType,
                                                 @NonNull String displayStatus,
                                                 long amount,
                                                 @NonNull String currency,
                                                 @NonNull LocalDateTime occurredTime,
                                                 @NonNull Map<String, Object> payload) {
    }

    @Builder
    public record FundsTransactionProjectionRow(@NonNull String projectionSn,
                                                @NonNull String viewDomain,
                                                @NonNull String ownerType,
                                                @NonNull String ownerId,
                                                @NonNull String sourceSn,
                                                @NonNull String displayType,
                                                @NonNull String displayStatus,
                                                long amount,
                                                @NonNull String currency,
                                                @NonNull LocalDateTime occurredTime,
                                                @NonNull Map<String, Object> payload) {
    }

    @Builder
    public record FundsTransactionProjectionDifference(@NonNull String sourceSn,
                                                       @NonNull String fieldName,
                                                       @Nullable Object expectedValue,
                                                       @Nullable Object actualValue) {
    }

    @Builder
    public record FundsTransactionProjectionReplayResult(@NonNull String taskSn,
                                                         @NonNull FundsTransactionProjectionReplayMode mode,
                                                         @NonNull String viewDomain,
                                                         @NonNull FundsTransactionProjectionReplayRange range,
                                                         int loadedFactCount,
                                                         int rebuiltRowCount,
                                                         @NonNull List<FundsTransactionProjectionDifference> differences,
                                                         @NonNull FundsTransactionProjectionCheckpoint checkpoint) {
    }
}
