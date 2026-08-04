package com.wind.funds.governance.projection;

import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.funds.governance.enums.ProjectionReplayTaskStatus;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 持久交易投影重放任务。
 */
@Builder
public record FundsProjectionReplayTaskDTO(@NonNull String taskSn,
                                           @NonNull Long tenantId,
                                           @NonNull String requestSn,
                                           @NonNull String requestDigest,
                                           @NonNull String viewDomain,
                                           @NonNull ProjectionReplayMode mode,
                                           @NonNull FundsTransactionProjectionReplayRange replayRange,
                                           @NonNull ProjectionReplayTaskStatus status,
                                           @NonNull FundsTransactionProjectionCheckpoint checkpoint,
                                           long successCount,
                                           long failedCount,
                                           long skippedCount,
                                           long differenceCount,
                                           @NonNull String reason,
                                           @NonNull String auditRef,
                                           @Nullable String approvalRef,
                                           @Nullable String validatedShadowTaskSn,
                                           @NonNull String operatorId) {
}
