package com.wind.funds.reconciliation.application.run;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;

/**
 * 对账运行结果应用服务。
 *
 * <p>职责：追加记录一次已完成对账运行的范围、规则、来源摘要、逐笔匹配结果和证据引用。</p>
 *
 * <p>边界：不采集外部文件、不执行匹配、不处理差错，也不创建资金或账本事实；
 * 状态和计数只由逐笔匹配结果派生。调用方必须是已完成来源验真、归一化和匹配的受控内部对账适配层；
 * 本服务只验证结构一致性，不独立证明来源内容和覆盖范围。</p>
 */
public interface ReconciliationRunResultApplicationService {

    /**
     * 记录一次不可变的已完成对账运行结果。
     *
     * <p>同一对账批次和准入对象重复提交相同事实时返回原结果；事实变化时拒绝覆盖。</p>
     *
     * @param request 对账运行结果
     * @param operator 操作人
     * @return 已保存的运行结果
     */
    ReconciliationRunResultDTO recordRunResult(RecordReconciliationRunResultRequest request,
                                                WindOperator operator);
}
