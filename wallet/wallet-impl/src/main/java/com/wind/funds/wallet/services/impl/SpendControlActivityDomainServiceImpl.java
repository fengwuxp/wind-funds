package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import com.wind.funds.wallet.service.SpendControlActivityDomainService;
import com.wind.funds.wallet.service.SpendControlActivityService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 控制额度变动流水领域写服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendControlActivityDomainServiceImpl implements SpendControlActivityDomainService {

    private static final int CONTROL_ACTIVITY_QUERY_PAGE_SIZE = 500;

    private final SpendControlActivityService spendControlActivityService;

    private final FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request) {
        validateIdempotencyBoundary(request);
        SpendControlActivityDTO existing = spendControlActivityService.findSpendControlActivity(
                request.getTenantId(),
                request.getActivitySn());
        if (existing != null) {
            assertSameActivity(request, existing);
            return existing;
        }
        validateRecordRequest(request);
        assertReleaseAmountNotOverReserved(request);
        try {
            Long id = spendControlActivityService.createSpendControlActivity(request);
            return spendControlActivityService.getSpendControlActivityById(id);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentActivityAfterInsertConflict(request, exception);
        }
    }

    private void validateIdempotencyBoundary(RecordSpendControlActivityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getActivitySn(), "控制活动流水号不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
    }

    private void validateRecordRequest(RecordSpendControlActivityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getActivitySn(), "控制活动流水号不能为空");
        AssertUtils.notNull(request.getActivityType(), "控制活动类型不能为空");
        AssertUtils.isFalse(request.getActivityType().isDecisionRecordCompatibilityActivity(),
                "Spend Rule 准入决策应记录为决策记录，不应写入控制额度变动流水，activitySn = {}",
                request.getActivitySn());
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
        assertNoSensitiveContextVariables(request.getContextVariables());
        assertTargetAccountSupported(request);
        if (request.getActivityType().isLimitAdjustmentActivity()) {
            AssertUtils.hasText(request.getReasonCode(), "预算控制额度调整原因码不能为空");
            AssertUtils.hasText(request.getOperatorId(), "预算控制额度调整操作者不能为空");
            AssertUtils.hasText(request.getAuditReferenceSn(), "预算控制额度调整审计引用不能为空");
        } else {
            AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
            AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
            AssertUtils.hasText(request.getSpendDecisionSn(), "Spend Rule 决策流水号不能为空");
            AssertUtils.notNull(request.getSpendDecisionResult(), "Spend Rule 决策结果不能为空");
            AssertUtils.hasText(request.getSpendDecisionDigest(), "Spend Rule 决策摘要不能为空");
        }
        if (request.getActivityType().isBudgetProjectionActivity()) {
            AssertUtils.hasText(request.getBudgetGroupSn(), "预算控制活动必须提供预算组标识");
        }
    }

    private void assertTargetAccountSupported(RecordSpendControlActivityRequest request) {
        FundsSubjectType subjectType = targetSubjectType(request.getTargetAccountId());
        assertSupportedTargetSubjectType(subjectType);
        fundsAccountCapabilityApplicationService.resolveFundsAccountCapability(
                new ResolveFundsAccountCapabilityRequest()
                        .setTenantId(request.getTenantId())
                        .setAccountId(request.getTargetAccountId())
                        .setCurrency(request.getCurrency()));
    }

    private SpendControlActivityDTO readIdempotentActivityAfterInsertConflict(
            RecordSpendControlActivityRequest request,
            DataIntegrityViolationException exception) {
        SpendControlActivityDTO existing = spendControlActivityService.findSpendControlActivity(
                request.getTenantId(),
                request.getActivitySn());
        if (existing == null) {
            throw exception;
        }
        assertSameActivity(request, existing);
        return existing;
    }

    private void assertSameActivity(RecordSpendControlActivityRequest request, SpendControlActivityDTO existing) {
        assertSameActivityDigest(request, existing);
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        assertSameActivityIdentity(request, existing);
        assertSameActivityTarget(request, existing);
        assertSameActivityAmountAndRule(request, existing);
        assertSameActivityDecision(request, existing);
        assertSameActivityAudit(request, existing);
    }

    private void assertSameActivityIdentity(RecordSpendControlActivityRequest request,
                                            SpendControlActivityDTO existing) {
        AssertUtils.isTrue(existing.getActivityType() == request.getActivityType(),
                "控制活动流水已存在但类型不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getBusinessScene(), request.getBusinessScene()),
                "控制活动流水已存在但业务场景不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getBusinessSn(), request.getBusinessSn()),
                "控制活动流水已存在但业务流水不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getOriginalActivitySn(), request.getOriginalActivitySn()),
                "控制活动流水已存在但原控制活动流水不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getTransactionSn(), request.getTransactionSn()),
                "控制活动流水已存在但资金交易流水不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getInstrumentSn(), request.getInstrumentSn()),
                "控制活动流水已存在但支付工具号不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(existing.getAction() == request.getAction(),
                "控制活动流水已存在但支付工具动作不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityTarget(RecordSpendControlActivityRequest request,
                                          SpendControlActivityDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getTargetAccountId(), request.getTargetAccountId()),
                "控制活动流水已存在但目标账户不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityAmountAndRule(RecordSpendControlActivityRequest request,
                                                 SpendControlActivityDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getAmount(), request.getAmount()),
                "控制活动流水已存在但控制金额不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(existing.getCurrency() == request.getCurrency(),
                "控制活动流水已存在但币种不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendRuleId(), request.getSpendRuleId()),
                "控制活动流水已存在但 Spend Rule 标识不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendRuleVersion(), request.getSpendRuleVersion()),
                "控制活动流水已存在但 Spend Rule 版本不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getBudgetGroupSn(), request.getBudgetGroupSn()),
                "控制活动流水已存在但预算组标识不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityDecision(RecordSpendControlActivityRequest request,
                                            SpendControlActivityDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getSpendDecisionSn(), request.getSpendDecisionSn()),
                "控制活动流水已存在但决策流水不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(existing.getSpendDecisionResult() == request.getSpendDecisionResult(),
                "控制活动流水已存在但决策结果不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getSpendDecisionDigest(), request.getSpendDecisionDigest()),
                "控制活动流水已存在但决策摘要不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityAudit(RecordSpendControlActivityRequest request,
                                         SpendControlActivityDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getReasonCode(), request.getReasonCode()),
                "控制活动流水已存在但调整原因码不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getOperatorId(), request.getOperatorId()),
                "控制活动流水已存在但操作者不一致，activitySn = {}",
                request.getActivitySn());
        AssertUtils.isTrue(Objects.equals(existing.getAuditReferenceSn(), request.getAuditReferenceSn()),
                "控制活动流水已存在但审计引用不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityDigest(RecordSpendControlActivityRequest request,
                                          SpendControlActivityDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getActivityDigest(), request.getActivityDigest()),
                "控制活动流水已存在但摘要不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertReleaseAmountNotOverReserved(RecordSpendControlActivityRequest request) {
        if (!request.getActivityType().isReleaseActivity()) {
            return;
        }
        long remainingControlAmount = getRemainingControlAmount(new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setBudgetGroupSn(request.getBudgetGroupSn())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId()));
        AssertUtils.isTrue(remainingControlAmount >= request.getAmount(),
                "控制释放金额超过可释放占用金额，activitySn = {}, remainingControlAmount = {}, amount = {}",
                request.getActivitySn(),
                remainingControlAmount,
                request.getAmount());
    }

    private long getRemainingControlAmount(BudgetControlProjectionQuery query) {
        List<SpendControlActivityDTO> budgetActivities = queryBudgetProjectionActivities(query).stream()
                .filter(activity -> activity.getActivityType().isBudgetProjectionActivity())
                .toList();
        long reservedAmount = sumByType(budgetActivities, SpendControlActivityType.RESERVED);
        long grossConsumedAmount = sumByType(budgetActivities, SpendControlActivityType.CONSUMED);
        long refundCompensatedAmount = sumByType(budgetActivities, SpendControlActivityType.REFUND_COMPENSATED);
        long consumedAmount = grossConsumedAmount - refundCompensatedAmount;
        long releasedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType().isReleaseActivity())
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
        return reservedAmount - consumedAmount - releasedAmount;
    }

    private List<SpendControlActivityDTO> queryBudgetProjectionActivities(BudgetControlProjectionQuery query) {
        WindPagination<SpendControlActivityDTO> page = spendControlActivityService.querySpendControlActivities(
                new SpendControlActivityQuery()
                        .setTenantId(query.getTenantId())
                        .setBudgetGroupSn(query.getBudgetGroupSn())
                        .setCurrency(query.getCurrency())
                        .setSpendRuleId(query.getSpendRuleId())
                        .setSpendRuleVersion(query.getSpendRuleVersion())
                        .setTargetAccountId(query.getTargetAccountId()),
                DefaultPageQueryOptions.defaults(CONTROL_ACTIVITY_QUERY_PAGE_SIZE));
        AssertUtils.isTrue(page.getTotal() <= CONTROL_ACTIVITY_QUERY_PAGE_SIZE,
                "预算控制活动超过单次投影计算上限，tenantId = {}, budgetGroupSn = {}, total = {}",
                query.getTenantId(),
                query.getBudgetGroupSn(),
                page.getTotal());
        return page.getRecords();
    }

    private long sumByType(List<SpendControlActivityDTO> activities, SpendControlActivityType activityType) {
        return activities.stream()
                .filter(activity -> activity.getActivityType() == activityType)
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
    }

    private FundsSubjectType targetSubjectType(FundsAccountId accountId) {
        boolean matched = Arrays.stream(FundsSubjectType.values())
                .anyMatch(type -> type.name().equals(accountId.type()));
        AssertUtils.isTrue(matched, "控制活动目标账户类型非法，targetAccountId = {}", accountId);
        return FundsSubjectType.valueOf(accountId.type());
    }

    private void assertSupportedTargetSubjectType(FundsSubjectType subjectType) {
        AssertUtils.isTrue(subjectType == FundsSubjectType.FUNDING_ACCOUNT
                        || subjectType == FundsSubjectType.CREDIT_ACCOUNT,
                "控制活动目标只能是资金账户或信用账户，targetSubjectType = {}",
                subjectType);
    }

    private void assertNoSensitiveContextVariables(String contextVariables) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(contextVariables);
    }
}
