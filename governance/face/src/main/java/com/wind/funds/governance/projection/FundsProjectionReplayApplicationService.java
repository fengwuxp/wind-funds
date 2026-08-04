package com.wind.funds.governance.projection;

import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 持久交易投影重放应用服务。
 */
public interface FundsProjectionReplayApplicationService {

    @NonNull
    FundsProjectionReplayTaskDTO createTask(@NonNull CreateFundsProjectionReplayTaskRequest request,
                                            @NonNull WindOperator operator);

    @NonNull
    FundsTransactionProjectionReplayResult runTask(@NonNull RunFundsProjectionReplayTaskRequest request,
                                                   @NonNull WindOperator operator);

    @NonNull
    FundsProjectionReplayTaskDTO getTask(@NonNull Long tenantId,
                                         @NonNull String taskSn,
                                         @NonNull WindOperator operator);

    @NonNull
    List<FundsProjectionReplayTaskDTO> queryBacklog(@NonNull Long tenantId,
                                                   int maxSize,
                                                   @NonNull WindOperator operator);
}
