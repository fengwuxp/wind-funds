package com.wind.funds.reconciliation.service;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ClearingSettlementGateResultDTO;
import com.wind.funds.reconciliation.model.request.CheckClearingSettlementGateRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 清算 / 结算对账准入消费服务。
 *
 * <p>职责：面向清算编排、结算编排、运营后台或自动任务，消费对账差错 gate，判断目标清算或结算对象是否允许继续。</p>
 *
 * <p>边界：本服务只做只读准入检查，不创建清算候选、确认清算批次、锁定结算单，也不写交易、route、posting、LedgerEntry 或余额投影。</p>
 */
@NullMarked
public interface ClearingSettlementGateConsumerService {

    /**
     * 检查清算或结算对象的对账准入状态。
     *
     * @param request  清算 / 结算准入检查请求，必须包含租户、对象类型、对象流水、币种、金额和幂等键
     * @param operator 操作人，用于审计检查人
     * @return 清算 / 结算准入检查结果；永不返回 null，阻断差错列表为空表示没有阻断项
     */
    ClearingSettlementGateResultDTO checkGate(CheckClearingSettlementGateRequest request, WindOperator operator);
}
