package com.wind.funds.reconciliation.service;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 出款证据预检服务。
 *
 * <p>职责：承接清结算出款前的证据预检能力。本轮只检查调用方提供的引用、外部规则证据、审批证据和对象级对账 Gate。</p>
 *
 * <p>调用方：清结算编排、运营后台、自动出款任务或后续出款命令服务。</p>
 *
 * <p>边界：预检结果不是出款提交授权，不验证结算锁定、账户真实状态、通道状态/额度、cutoff、名单、余额、准备金或幂等冲突；
 * 真实提交命令必须基于权威事实重新执行完整门禁。</p>
 */
@NullMarked
public interface PayoutPreflightService {

    /**
     * 检查出款前准入条件。
     *
     * @param request  出款前证据预检请求；结算单号和对账运行结果用于确定本次检查对象
     * @param operator 检查人；访问与操作审计由 Web/宿主层统一记录
     * @return 出款前证据预检结果；永不返回 null，阻断原因列表为空只表示当前预检项通过
     */
    PayoutPreflightResultDTO checkPayoutPreflight(CheckPayoutPreflightRequest request, WindOperator operator);
}
