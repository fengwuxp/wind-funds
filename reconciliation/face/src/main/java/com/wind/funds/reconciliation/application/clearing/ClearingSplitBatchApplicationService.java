package com.wind.funds.reconciliation.application.clearing;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.model.dto.ClearingSplitBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplitResultSnapshotDTO;
import com.wind.funds.reconciliation.model.query.ClearingSplitBatchQuery;
import com.wind.funds.reconciliation.model.request.CancelClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingSplitBatchRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingSplitBatchRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 清分批次应用服务。
 *
 * <p>一个批次只承接同一账务主体、币种、业务线、清分周期和规则版本的明细。
 * 确认只冻结不可变清分结果快照，不创建资金交易、账本分录或余额变更。</p>
 */
@NullMarked
public interface ClearingSplitBatchApplicationService {

    ClearingSplitBatchDTO createBatch(CreateClearingSplitBatchRequest request, WindOperator operator);

    ClearingSplitBatchDTO submitBatch(SubmitClearingSplitBatchRequest request, WindOperator operator);

    ClearingSplitBatchDTO confirmBatch(ConfirmClearingSplitBatchRequest request, WindOperator operator);

    ClearingSplitBatchDTO cancelBatch(CancelClearingSplitBatchRequest request, WindOperator operator);

    ClearingSplitBatchDTO getBatch(Long tenantId, String splitBatchSn);

    /**
     * 分页查询清分批次，供宿主任务和运营台发现待处理对象。
     */
    WindPagination<ClearingSplitBatchDTO> queryBatches(
            ClearingSplitBatchQuery query,
            WindQuery<? extends QueryOrderField> options);

    List<ClearingSplitResultSnapshotDTO> getResultSnapshots(Long tenantId, String splitBatchSn);
}
