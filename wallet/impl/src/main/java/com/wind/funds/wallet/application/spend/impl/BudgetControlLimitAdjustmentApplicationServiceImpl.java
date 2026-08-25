package com.wind.funds.wallet.application.spend.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.application.spend.BudgetControlLimitAdjustmentApplicationService;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.model.dto.BudgetControlLimitAdjustmentResultDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.AdjustBudgetControlLimitRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预算控制额度调整应用服务实现。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Slf4j
@Service
@AllArgsConstructor
public class BudgetControlLimitAdjustmentApplicationServiceImpl
        implements BudgetControlLimitAdjustmentApplicationService {

    private final SpendControlMovementService spendControlMovementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull BudgetControlLimitAdjustmentResultDTO adjustLimit(
            @NonNull AdjustBudgetControlLimitRequest request,
            @NonNull WindOperator operator) {
        validateRequest(request);
        RecordSpendControlMovementRequest recordRequest = toRecordMovementRequest(request, operator);
        BudgetControlProjectionQuery projectionQuery = toProjectionQuery(request);
        if (queryExistingMovement(request).isEmpty()) {
            BudgetControlProjectionDTO beforeProjection = spendControlMovementService
                    .getBudgetControlProjection(projectionQuery);
            assertDecreaseNotBreakOccupiedControl(request, beforeProjection);
        }
        SpendControlMovementDTO activity = spendControlMovementService.recordMovement(
                recordRequest);
        BudgetControlProjectionDTO projection = spendControlMovementService.getBudgetControlProjection(
                projectionQuery);
        BudgetControlLimitAdjustmentResultDTO result = toResult(request, activity, projection);
        log.info("预算控制额度调整完成，等待事务提交，tenantId={}, movementSn={}, businessScene={}, businessSn={}, "
                        + "controlScopeId={}, periodId={}, increase={}, amount={}, currency={}",
                request.getTenantId(), request.getMovementSn(), request.getBusinessScene(), request.getBusinessSn(),
                controlScopeId(request), request.getPeriodId(), request.getIncrease(), request.getAmount(),
                request.getCurrency());
        return result;
    }

    private void validateRequest(AdjustBudgetControlLimitRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "预算控制额度调整 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getMovementSn(), "预算控制额度调整变动流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.hasText(controlScopeId(request), "支出控制范围标识不能为空");
        AssertUtils.hasText(request.getPeriodId(), "预算控制周期标识不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "预算控制额度目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "调整金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "调整金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getIncrease(), "预算控制额度调整方向不能为空");
        AssertUtils.hasText(request.getReasonCode(), "预算控制额度调整原因码不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "预算控制额度调整审计引用不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getMovementDigest(), "控制额度变动摘要");
    }

    private RecordSpendControlMovementRequest toRecordMovementRequest(AdjustBudgetControlLimitRequest request,
                                                                      WindOperator operator) {
        return new RecordSpendControlMovementRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn())
                .setMovementType(Boolean.TRUE.equals(request.getIncrease())
                        ? SpendControlMovementType.LIMIT_INCREASED
                        : SpendControlMovementType.LIMIT_DECREASED)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setTargetAccountId(request.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setControlScopeId(controlScopeId(request))
                .setPeriodId(request.getPeriodId())
                .setReasonCode(request.getReasonCode())
                .setOperatorId(operatorId(operator))
                .setAuditReferenceSn(request.getAuditReferenceSn())
                .setMovementDigest(request.getMovementDigest())
                .setDescription(request.getDescription())
                .setContextVariables(request.getContextVariables());
    }

    private String operatorId(WindOperator operator) {
        AssertUtils.notNull(operator.getOperatorId(), "预算控制额度调整操作者不能为空");
        return operator.getOperatorAsText();
    }

    private BudgetControlProjectionQuery toProjectionQuery(AdjustBudgetControlLimitRequest request) {
        return new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setControlScopeId(controlScopeId(request))
                .setPeriodId(request.getPeriodId())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId());
    }

    private List<SpendControlMovementDTO> queryExistingMovement(AdjustBudgetControlLimitRequest request) {
        return spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn()));
    }

    private void assertDecreaseNotBreakOccupiedControl(AdjustBudgetControlLimitRequest request,
                                                       BudgetControlProjectionDTO beforeProjection) {
        if (Boolean.TRUE.equals(request.getIncrease())) {
            return;
        }
        long committedControlAmount = beforeProjection.getConsumedAmount()
                + beforeProjection.getRemainingControlAmount();
        long proposedLimitAmount = beforeProjection.getLimitAmount() - request.getAmount();
        AssertUtils.isTrue(proposedLimitAmount >= committedControlAmount,
                "预算控制额度调减不能低于已使用或已占用控制金额，movementSn = {}, "
                        + "limitAmount = {}, committedControlAmount = {}",
                request.getMovementSn(),
                proposedLimitAmount,
                committedControlAmount);
    }

    private BudgetControlLimitAdjustmentResultDTO toResult(AdjustBudgetControlLimitRequest request,
                                                           SpendControlMovementDTO activity,
                                                           BudgetControlProjectionDTO projection) {
        return new BudgetControlLimitAdjustmentResultDTO()
                .setMovementId(activity.getId())
                .setMovementSn(activity.getMovementSn())
                .setMovementType(activity.getMovementType())
                .setTenantId(activity.getTenantId())
                .setBusinessScene(activity.getBusinessScene())
                .setBusinessSn(activity.getBusinessSn())
                .setControlScopeId(activity.getControlScopeId())
                .setPeriodId(activity.getPeriodId())
                .setTargetAccountId(activity.getTargetAccountId())
                .setAmount(activity.getAmount())
                .setCurrency(activity.getCurrency())
                .setSpendRuleId(activity.getSpendRuleId())
                .setSpendRuleVersion(activity.getSpendRuleVersion())
                .setIncrease(request.getIncrease())
                .setReasonCode(activity.getReasonCode())
                .setOperatorId(activity.getOperatorId())
                .setAuditReferenceSn(activity.getAuditReferenceSn())
                .setMovementDigest(activity.getMovementDigest())
                .setProjection(projection);
    }

    private String controlScopeId(AdjustBudgetControlLimitRequest request) {
        return request.getControlScopeId();
    }
}
