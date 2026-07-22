package com.wind.funds.reconciliation.application.batch;

import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 对账批次应用服务。
 *
 * <p>职责：冻结对账范围，并为批次记录基准侧和核对侧的不可变来源成员集合。</p>
 *
 * <p>边界：本服务不采集或解析原始文件，不执行匹配规则，也不创建资金或账本事实。</p>
 */
@NullMarked
public interface ReconciliationBatchApplicationService {

    /**
     * 创建对账批次。
     *
     * @param request  对账范围和规则
     * @param operator 操作人
     * @return 对账批次；相同不可变事实重复提交时返回原批次
     */
    ReconciliationBatchDTO createBatch(CreateReconciliationBatchRequest request, WindOperator operator);

    /**
     * 记录并冻结一侧来源快照。
     *
     * <p>来源成员引用必须指向不可变事实。服务按规范化后的成员集合生成摘要；同一批次同一角色只能记录一次。</p>
     *
     * @param request  来源角色、类型、成员引用和证据引用
     * @param operator 操作人
     * @return 来源快照；相同不可变事实重复提交时返回原快照
     */
    ReconciliationSourceSnapshotDTO recordSourceSnapshot(RecordReconciliationSourceSnapshotRequest request,
                                                          WindOperator operator);
}
