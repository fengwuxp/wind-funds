package com.wind.funds.governance.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 运行一批持久投影重放任务的请求。
 */
@Builder
public record RunFundsProjectionReplayTaskRequest(@NonNull Long tenantId,
                                                  @NonNull String taskSn,
                                                  int maxBatchSize) {
}
