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
 * <p>职责：从已冻结的两侧归一事实执行严格相等比较，并保存不可变运行结果。</p>
 *
 * <p>边界：不采集或解释原始载体，不创建资金或账本事实，不接受调用方的匹配断言。</p>
 */
public interface ReconciliationRunResultApplicationService {

    /**
     * 执行一次不可变严格精确对账运行。
     *
     * <p>同一对账批次重复提交相同事实时返回原结果；事实变化时拒绝覆盖。</p>
     *
     * @param request 对账运行结果
     * @param operator 操作人
     * @return 已保存的运行结果
     */
    ReconciliationRunResultDTO executeStrictExact(RecordReconciliationRunResultRequest request,
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
