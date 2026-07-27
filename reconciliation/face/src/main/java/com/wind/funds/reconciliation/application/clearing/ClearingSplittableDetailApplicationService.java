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
 * <p>边界：本服务不创建清分批次、清算候选、资金交易、route、posting 或账本事实。
 * 返回的 {@code SPLIT_READY} 只表示基于已冻结来源版本形成清分候选，不是清分执行授权。
 * 最终清分命令必须在自身事务内重新核对来源交易版本、退款累计与对象级对账 Gate，
 * 任一事实变化都必须失败关闭。</p>
 */
@NullMarked
public interface ClearingSplittableDetailApplicationService {

    /**
     * 识别并记录单笔可清分明细。
     *
     * @param request  来源事实与已解析清分规则
     * @param operator 操作人
     * @return 可清分候选准入结果；重复请求返回同一结果。对账 Gate 阻断时返回无流水号的临时排除结论，
     * 不占用来源账本分录唯一键，待阻断解除后可重试
     */
    ClearingSplittableDetailDTO identifySplittableDetail(IdentifyClearingSplittableDetailRequest request,
                                                         WindOperator operator);
}
