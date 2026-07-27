package com.wind.funds.reconciliation.application.batch;

import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.AbortReconciliationBatchRequest;
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
     * 终止被确认无效的当前批次。
     *
     * <p>该操作只使批次证据失效，不删除来源、运行结果或差错事实，也不自动创建替代批次。
     * 替代批次由调用方以被终止批次作为 {@code previousBatchSn} 显式创建。</p>
     *
     * @param request  批次流水号和终止原因
     * @param operator 操作人
     * @return 已终止批次
     */
    ReconciliationBatchDTO abortBatch(AbortReconciliationBatchRequest request, WindOperator operator);

    /**
     * 记录并冻结一侧来源快照。
     *
     * <p>来源成员引用必须指向不可变事实，内容摘要由已完成验签、来源授权与归一化的可信适配器生成。
     * 服务只冻结引用与内容身份并生成集合摘要，不验证外部来源真实性；同一批次同一角色只能记录一次。
     * 生产环境必须通过宿主 IAM 限制本方法只能由可信适配器调用。</p>
     *
     * @param request  来源角色、类型、成员引用和证据引用
     * @param operator 操作人
     * @return 来源快照；相同不可变事实重复提交时返回原快照
     */
    ReconciliationSourceSnapshotDTO recordSourceSnapshot(RecordReconciliationSourceSnapshotRequest request,
                                                          WindOperator operator);
}
