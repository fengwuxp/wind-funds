package com.wind.funds.governance.projection;

import com.wind.funds.governance.enums.ProjectionReplayMode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 创建持久交易投影重放任务的请求。
 */
@Builder
public record CreateFundsProjectionReplayTaskRequest(@NonNull String requestSn,
                                                     @NonNull String requestDigest,
                                                     @NonNull Long tenantId,
                                                     @NonNull String viewDomain,
                                                     @NonNull ProjectionReplayMode mode,
                                                     @NonNull FundsTransactionProjectionReplayRange replayRange,
                                                     @NonNull String reason,
                                                     @NonNull String auditRef,
                                                     @Nullable String approvalRef,
                                                     @Nullable String validatedShadowTaskSn) {
}
