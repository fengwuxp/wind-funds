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
 * {@code CLEARING -> AVAILABLE}。清分归类、退款决策和结算出款不属于本服务。</p>
 */
@NullMarked
public interface ClearingBatchApplicationService {

    ClearingBatchDTO createBatch(CreateClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO replaceDraftCandidates(ReplaceClearingBatchCandidatesRequest request,
                                             WindOperator operator);

    ClearingBatchDTO submitBatch(SubmitClearingBatchRequest request, WindOperator operator);

    ClearingBatchDTO returnToDraft(ReturnClearingBatchToDraftRequest request, WindOperator operator);

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
