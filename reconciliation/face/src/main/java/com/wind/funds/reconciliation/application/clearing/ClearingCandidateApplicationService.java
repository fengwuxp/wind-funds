package com.wind.funds.reconciliation.application.clearing;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.model.dto.ClearingCandidateDTO;
import com.wind.funds.reconciliation.model.query.ClearingCandidateQuery;
import com.wind.funds.reconciliation.model.request.CreateClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ExcludeClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.LockClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ReleaseClearingCandidateLockRequest;
import com.wind.funds.reconciliation.model.request.RestoreClearingCandidateRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 清算候选应用服务。
 *
 * <p>只承接已确认清分结果快照的清算资格和批次占用；不创建清算资金交易、账本分录或余额变更。</p>
 */
@NullMarked
public interface ClearingCandidateApplicationService {

    ClearingCandidateDTO createCandidate(CreateClearingCandidateRequest request, WindOperator operator);

    ClearingCandidateDTO excludeCandidate(ExcludeClearingCandidateRequest request, WindOperator operator);

    ClearingCandidateDTO restoreCandidate(RestoreClearingCandidateRequest request, WindOperator operator);

    ClearingCandidateDTO lockCandidate(LockClearingCandidateRequest request, WindOperator operator);

    /**
     * 释放清算批次确定性撤回的候选锁定。
     *
     * <p>仅允许匹配当前锁定批次的 LOCKED 候选回到 READY；超时、网络断开或外部结果未知时不得调用此方法。</p>
     */
    ClearingCandidateDTO releaseCandidateLock(ReleaseClearingCandidateLockRequest request,
                                               WindOperator operator);

    ClearingCandidateDTO getCandidate(Long tenantId, String candidateSn);

    /**
     * 分页查询清算候选，供宿主账期任务和异常处置扫描。
     */
    WindPagination<ClearingCandidateDTO> queryCandidates(
            ClearingCandidateQuery query,
            WindQuery<? extends QueryOrderField> options);
}
