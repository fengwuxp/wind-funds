package com.wind.funds.reconciliation.application.run;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.AbstractPageQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.integration.operator.WindOperator;

/**
 * 对账运行结果应用服务。
 *
 * <p>职责：追加记录一次已完成对账运行的范围、规则、来源摘要、逐笔匹配结果和证据引用。</p>
 *
 * <p>边界：不采集外部文件、不执行匹配、不处理差错，也不创建资金或账本事实；
 * 状态和计数只由逐笔匹配结果派生。调用方必须是已完成来源验真、归一化和匹配的受控内部对账适配层；
 * 本服务从已冻结来源快照验证引用归属和完整覆盖，不验证来源业务内容本身。</p>
 */
public interface ReconciliationRunResultApplicationService {

    /**
     * 记录一次不可变的已完成对账运行结果。
     *
     * <p>同一对账批次重复提交相同事实时返回原结果；事实变化时拒绝覆盖。</p>
     *
     * @param request 对账运行结果
     * @param operator 操作人
     * @return 已保存的运行结果
     */
    ReconciliationRunResultDTO recordRunResult(RecordReconciliationRunResultRequest request,
                                                WindOperator operator);

    /**
     * 按流水号读取不可变运行结果。
     *
     * @param tenantId 租户 ID
     * @param runResultSn 运行结果流水号
     * @return 运行结果
     */
    ReconciliationRunResultDTO getRunResult(Long tenantId, String runResultSn);

    /**
     * 按固定内部主键升序分页读取指定运行的逐笔匹配结果。
     *
     * <p>仅支持页码分页，不接受游标分页或自定义排序。</p>
     *
     * @param tenantId 租户 ID
     * @param runResultSn 运行结果流水号
     * @param options 页码分页选项
     * @return 逐笔匹配结果分页
     */
    WindPagination<ReconciliationMatchResultDTO> queryMatchResults(
            Long tenantId,
            String runResultSn,
            AbstractPageQuery<? extends QueryOrderField> options);
}
