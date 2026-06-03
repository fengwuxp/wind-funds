package com.wind.funds.reconciliation.service;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 出款单服务。
 *
 * <p>职责：承接清结算出款前后的应用服务能力。本轮只开放出款前准入检查，供调用方在创建或提交出款前确认是否允许继续。</p>
 *
 * <p>调用方：清结算编排、运营后台、自动出款任务或后续出款命令服务。</p>
 *
 * <p>边界：准入检查不创建出款单、不调用外部通道、不写账务事实；真正的出款生命周期、通道回执和账务处理由后续能力承载。</p>
 */
@NullMarked
public interface PayoutOrderService {

    /**
     * 检查出款前准入条件。
     *
     * @param request  出款前准入检查请求，核心身份、金额、幂等字段必须满足非空契约
     * @param operator 操作人，用于审计检查人
     * @return 出款前准入检查结果；永不返回 null，阻断原因列表为空表示准入通过
     */
    PayoutPreflightResultDTO checkPayoutPreflight(CheckPayoutPreflightRequest request, WindOperator operator);
}
