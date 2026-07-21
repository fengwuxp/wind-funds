package com.wind.funds.reconciliation.application.clearing;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 可清分明细准入应用服务。
 *
 * <p>职责：基于调用方提供的稳定事实引用，校验资金交易、交易明细、账本交易、记账计划、
 * CLEARING 分录和清分前对账门禁，并幂等记录单笔准入结果。</p>
 *
 * <p>边界：本服务不创建清分批次、清算候选、资金交易、route、posting 或账本事实。</p>
 */
@NullMarked
public interface ClearingSplittableDetailApplicationService {

    /**
     * 识别并记录单笔可清分明细。
     *
     * @param request  来源事实与已解析清分规则
     * @param operator 操作人
     * @return 可清分准入结果；重复请求返回同一结果
     */
    ClearingSplittableDetailDTO identifySplittableDetail(IdentifyClearingSplittableDetailRequest request,
                                                         WindOperator operator);
}
