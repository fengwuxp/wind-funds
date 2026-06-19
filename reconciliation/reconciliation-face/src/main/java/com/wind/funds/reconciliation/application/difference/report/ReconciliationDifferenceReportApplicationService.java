package com.wind.funds.reconciliation.application.difference.report;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceReportDTO;
import com.wind.funds.reconciliation.model.request.GetReconciliationDifferenceReportRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 对账差异报告应用服务。
 *
 * <p>职责：按单笔对账差错生成只读解释视图，向运营、财务、风控、研发和测试说明差错来源、阻断对象、处理链路、重跑结果和准入 gate 摘要。</p>
 *
 * <p>边界：本服务只读取并聚合对账差错与准入决策，不创建清算、结算、出款、交易、route、posting、LedgerEntry 或余额投影事实。</p>
 */
@NullMarked
public interface ReconciliationDifferenceReportApplicationService {

    /**
     * 查询单笔对账差异报告。
     *
     * @param request  报告查询请求，按租户和差错流水号稳定定位
     * @param operator 查询操作人，用于审计和解释当前查询主体
     * @return 对账差异报告
     */
    ReconciliationDifferenceReportDTO getReport(GetReconciliationDifferenceReportRequest request,
                                                WindOperator operator);
}
