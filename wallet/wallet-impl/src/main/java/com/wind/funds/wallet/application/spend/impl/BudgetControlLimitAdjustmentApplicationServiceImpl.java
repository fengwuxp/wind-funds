package com.wind.funds.wallet.application.spend.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.application.spend.BudgetControlLimitAdjustmentApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlActivityApplicationService;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.model.dto.BudgetControlLimitAdjustmentResultDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.AdjustBudgetControlLimitRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import lombok.AllArgsConstructor;
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
@Service
@AllArgsConstructor
public class BudgetControlLimitAdjustmentApplicationServiceImpl
        implements BudgetControlLimitAdjustmentApplicationService {

    private final SpendControlActivityApplicationService spendControlActivityApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull BudgetControlLimitAdjustmentResultDTO adjustLimit(
            @NonNull AdjustBudgetControlLimitRequest request) {
        validateRequest(request);
        RecordSpendControlActivityRequest recordRequest = toRecordActivityRequest(request);
        BudgetControlProjectionQuery projectionQuery = toProjectionQuery(request);
        if (queryExistingActivity(request).isEmpty()) {
            BudgetControlProjectionDTO beforeProjection = spendControlActivityApplicationService
                    .getBudgetControlProjection(projectionQuery);
            assertDecreaseNotBreakOccupiedControl(request, beforeProjection);
        }
        SpendControlActivityDTO activity = spendControlActivityApplicationService.recordActivity(
                recordRequest);
        BudgetControlProjectionDTO projection = spendControlActivityApplicationService.getBudgetControlProjection(
                projectionQuery);
        return toResult(request, activity, projection);
    }

    private void validateRequest(AdjustBudgetControlLimitRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getActivitySn(), "预算控制额度调整活动流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.hasText(request.getBudgetGroupSn(), "预算组标识不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "预算控制额度目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "调整金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "调整金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getIncrease(), "预算控制额度调整方向不能为空");
        AssertUtils.hasText(request.getReasonCode(), "预算控制额度调整原因码不能为空");
        AssertUtils.hasText(request.getOperatorId(), "预算控制额度调整操作者不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "预算控制额度调整审计引用不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
    }

    private RecordSpendControlActivityRequest toRecordActivityRequest(AdjustBudgetControlLimitRequest request) {
        return new RecordSpendControlActivityRequest()
                .setTenantId(request.getTenantId())
                .setActivitySn(request.getActivitySn())
                .setActivityType(Boolean.TRUE.equals(request.getIncrease())
                        ? SpendControlActivityType.LIMIT_INCREASED
                        : SpendControlActivityType.LIMIT_DECREASED)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setTargetAccountId(request.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setBudgetGroupSn(request.getBudgetGroupSn())
                .setReasonCode(request.getReasonCode())
                .setOperatorId(request.getOperatorId())
                .setAuditReferenceSn(request.getAuditReferenceSn())
                .setActivityDigest(request.getActivityDigest())
                .setDescription(request.getDescription())
                .setContextVariables(request.getContextVariables());
    }

    private BudgetControlProjectionQuery toProjectionQuery(AdjustBudgetControlLimitRequest request) {
        return new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setBudgetGroupSn(request.getBudgetGroupSn())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId());
    }

    private List<SpendControlActivityDTO> queryExistingActivity(AdjustBudgetControlLimitRequest request) {
        return spendControlActivityApplicationService.queryActivities(new SpendControlActivityQuery()
                .setTenantId(request.getTenantId())
                .setActivitySn(request.getActivitySn()));
    }

    private void assertDecreaseNotBreakOccupiedControl(AdjustBudgetControlLimitRequest request,
                                                       BudgetControlProjectionDTO beforeProjection) {
        if (Boolean.TRUE.equals(request.getIncrease())) {
            return;
        }
        long proposedLimitAmount = beforeProjection.getLimitAmount() - request.getAmount();
        AssertUtils.isTrue(proposedLimitAmount >= beforeProjection.getRemainingControlAmount(),
                "预算控制额度调减不能低于已占用控制金额，activitySn = {}, limitAmount = {}, occupiedAmount = {}",
                request.getActivitySn(),
                proposedLimitAmount,
                beforeProjection.getRemainingControlAmount());
    }

    private BudgetControlLimitAdjustmentResultDTO toResult(AdjustBudgetControlLimitRequest request,
                                                           SpendControlActivityDTO activity,
                                                           BudgetControlProjectionDTO projection) {
        return new BudgetControlLimitAdjustmentResultDTO()
                .setActivityId(activity.getId())
                .setActivitySn(activity.getActivitySn())
                .setActivityType(activity.getActivityType())
                .setTenantId(activity.getTenantId())
                .setBusinessScene(activity.getBusinessScene())
                .setBusinessSn(activity.getBusinessSn())
                .setBudgetGroupSn(activity.getBudgetGroupSn())
                .setTargetAccountId(activity.getTargetAccountId())
                .setAmount(activity.getAmount())
                .setCurrency(activity.getCurrency())
                .setSpendRuleId(activity.getSpendRuleId())
                .setSpendRuleVersion(activity.getSpendRuleVersion())
                .setIncrease(request.getIncrease())
                .setReasonCode(activity.getReasonCode())
                .setOperatorId(activity.getOperatorId())
                .setAuditReferenceSn(activity.getAuditReferenceSn())
                .setActivityDigest(activity.getActivityDigest())
                .setProjection(projection);
    }
}
