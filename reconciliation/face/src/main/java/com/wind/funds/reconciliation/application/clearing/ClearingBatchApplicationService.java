package com.wind.funds.reconciliation.application.clearing;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.model.dto.ClearingBatchDTO;
import com.wind.funds.reconciliation.model.query.ClearingBatchQuery;
import com.wind.funds.reconciliation.model.request.CancelClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.ReplaceClearingBatchCandidatesRequest;
import com.wind.funds.reconciliation.model.request.ReturnClearingBatchToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 清算批次应用服务。
 *
 * <p>负责锁定同一账务主体和币种的清算候选，并在最终准入通过后原子执行
 * {@code CLEARING -> AVAILABLE}。这是宿主发起内部清算的公共用例入口；交易层清算资金原语
 * 只服务本用例编排，不是宿主直接调用能力。清分归类、退款决策和结算出款不属于本服务。</p>
 */
@NullMarked
public interface ClearingBatchApplicationService {

    ClearingBatchDTO createBatch(CreateClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO replaceDraftCandidates(ReplaceClearingBatchCandidatesRequest request,
                                             WindOperator operator);

    ClearingBatchDTO submitBatch(SubmitClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO returnToDraft(ReturnClearingBatchToDraftRequest request, WindOperator operator);

    /**
     * 复核并原子确认清算批次，是宿主触发 {@code CLEARING -> AVAILABLE} 的唯一支持入口。
     *
     * @param request  清算批次确认请求
     * @param operator 操作人
     * @return 确认后的清算批次
     */
    ClearingBatchDTO confirmBatch(ConfirmClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO cancelBatch(CancelClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO getBatch(Long tenantId, String clearingBatchSn);

    /**
     * 分页查询清算批次，供宿主任务和运营台发现待复核或结果未知对象。
     */
    WindPagination<ClearingBatchDTO> queryBatches(
            ClearingBatchQuery query,
            WindQuery<? extends QueryOrderField> options);
}
