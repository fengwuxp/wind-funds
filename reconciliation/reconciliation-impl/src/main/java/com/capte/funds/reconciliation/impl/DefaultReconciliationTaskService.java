package com.capte.funds.reconciliation.impl;

import com.capte.funds.reconciliation.enums.ReconciliationTaskStatus;
import com.capte.funds.reconciliation.model.dto.ReconciliationTaskDTO;
import com.capte.funds.reconciliation.model.request.CreateReconciliationTaskRequest;
import com.capte.funds.reconciliation.service.ReconciliationTaskService;
import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * 默认对账任务服务。
 *
 * <p>当前实现只落对账任务控制面边界，后续由独立批次补齐持久化、数据采集、匹配和差错闭环。</p>
 */
@Service
public class DefaultReconciliationTaskService implements ReconciliationTaskService {

    @Override
    public @NonNull ReconciliationTaskDTO createTask(@NonNull CreateReconciliationTaskRequest request) {
        AssertUtils.hasText(request.getRequestSn(), "对账任务请求号不能为空");
        AssertUtils.hasText(request.getRequestDigest(), "对账任务请求摘要不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账任务租户不能为空");
        AssertUtils.hasText(request.getTaskType(), "对账任务类型不能为空");
        AssertUtils.notNull(request.getWindowStart(), "对账任务开始时间不能为空");
        AssertUtils.notNull(request.getWindowEnd(), "对账任务结束时间不能为空");
        AssertUtils.isTrue(request.getWindowStart().isBefore(request.getWindowEnd()),
                "对账任务时间窗口必须有界");
        return new ReconciliationTaskDTO()
                .setTaskSn(request.getRequestSn())
                .setTenantId(request.getTenantId())
                .setTaskType(request.getTaskType())
                .setRuleVersion(request.getRuleVersion())
                .setWindowStart(request.getWindowStart())
                .setWindowEnd(request.getWindowEnd())
                .setStatus(ReconciliationTaskStatus.CREATED);
    }
}
