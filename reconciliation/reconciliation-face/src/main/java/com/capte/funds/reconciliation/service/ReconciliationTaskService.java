package com.capte.funds.reconciliation.service;

import com.capte.funds.reconciliation.model.dto.ReconciliationTaskDTO;
import com.capte.funds.reconciliation.model.request.CreateReconciliationTaskRequest;
import org.jspecify.annotations.NonNull;

/**
 * 对账任务服务。
 *
 * <p>职责：承载对账任务控制面，创建任务、采集事实、执行匹配和关闭任务。</p>
 *
 * <p>边界：本服务不直接修改交易事实、账本分录、余额投影或交易投影；涉及资金修正时必须转入标准资金事实链路。</p>
 */
public interface ReconciliationTaskService {

    /**
     * 创建对账任务。
     *
     * @param request 创建请求
     * @return 对账任务
     */
    @NonNull
    ReconciliationTaskDTO createTask(@NonNull CreateReconciliationTaskRequest request);
}
