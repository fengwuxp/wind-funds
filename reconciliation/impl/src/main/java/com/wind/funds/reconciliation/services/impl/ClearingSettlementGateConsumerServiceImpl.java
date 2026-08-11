package com.wind.funds.reconciliation.services.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.model.dto.ClearingSettlementGateResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckClearingSettlementGateRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.service.ClearingSettlementGateConsumerService;
import com.wind.integration.core.context.TenantContextHolder;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 清算 / 结算对账准入消费服务实现。
 *
 * <p>职责：复用对账差错 gate，为清算或结算消费方返回可解释的时点检查结果。</p>
 *
 * <p>边界：本实现不直接访问差错 Mapper，不写清算、结算、交易或账本事实。</p>
 */
@NullMarked
@Service
@AllArgsConstructor
public class ClearingSettlementGateConsumerServiceImpl implements ClearingSettlementGateConsumerService {

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClearingSettlementGateResultDTO inspectGate(CheckClearingSettlementGateRequest request,
                                                       WindOperator operator) {
        validateRequest(request);
        AssertUtils.notNull(operator, "清算结算对账准入检查操作人不能为空");
        ReconciliationGateDecisionDTO decision = reconciliationGateApplicationService.inspectGate(
                toGateRequest(request), operator);
        return new ClearingSettlementGateResultDTO()
                .setPassed(decision.isPassed())
                .setDecisionResult(decision.getDecisionResult())
                .setGateObjectType(decision.getGateObjectType())
                .setGateObjectSn(decision.getGateObjectSn())
                .setReconciliationRunResultSn(decision.getReconciliationRunResultSn())
                .setReconciliationResultDigest(decision.getReconciliationResultDigest())
                .setBlockingDifferences(decision.getBlockingDifferences())
                .setResolvedDifferenceCount(decision.getResolvedDifferenceCount())
                .setEvidenceRefs(decision.getEvidenceRefs())
                .setExplanation(decision.getExplanation())
                .setCheckedAt(decision.getCheckedAt())
                .setCheckedBy(decision.getCheckedBy());
    }

    private void validateRequest(CheckClearingSettlementGateRequest request) {
        AssertUtils.notNull(request, "清算结算对账准入检查请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "清算结算对账准入检查租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "清算结算对账准入检查 tenantId 与当前租户不一致");
        AssertUtils.notNull(request.getGateObjectType(), "清算结算对账准入消费对象类型不能为空");
        AssertUtils.isTrue(isClearingOrSettlement(request.getGateObjectType()),
                "清算结算对账准入消费对象类型仅支持 CLEARING 或 SETTLEMENT");
        AssertUtils.hasText(request.getGateObjectSn(), "清算结算对账准入消费对象流水号不能为空");
        AssertUtils.hasText(request.getReconciliationRunResultSn(), "清算结算对账运行结果流水号不能为空");
    }

    private boolean isClearingOrSettlement(ReconciliationGateObjectType gateObjectType) {
        return gateObjectType == ReconciliationGateObjectType.CLEARING
                || gateObjectType == ReconciliationGateObjectType.SETTLEMENT;
    }

    private CheckReconciliationGateRequest toGateRequest(CheckClearingSettlementGateRequest request) {
        return new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setGateObjectType(request.getGateObjectType())
                .setGateObjectSn(request.getGateObjectSn())
                .setReconciliationRunResultSn(request.getReconciliationRunResultSn());
    }
}
