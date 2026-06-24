package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.service.SpendControlActivityDomainQueryService;
import com.wind.funds.wallet.service.SpendControlActivityService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 控制额度变动流水领域读服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendControlActivityDomainQueryServiceImpl implements SpendControlActivityDomainQueryService {

    private static final int CONTROL_ACTIVITY_QUERY_PAGE_SIZE = 500;

    private final SpendControlActivityService spendControlActivityService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendControlActivityDTO> queryActivities(@NonNull SpendControlActivityQuery query) {
        validateQuery(query);
        return queryActivitiesByPage(query);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull BudgetControlProjectionDTO getBudgetControlProjection(
            @NonNull BudgetControlProjectionQuery query) {
        validateProjectionQuery(query);
        return toProjection(query, queryBudgetProjectionActivities(query));
    }

    private void validateQuery(SpendControlActivityQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.isTrue(hasNarrowCondition(query), "控制额度变动流水查询必须至少提供一个过滤条件");
        if (query.getTargetAccountId() != null) {
            assertSupportedTargetSubjectType(targetSubjectType(query.getTargetAccountId()));
        }
    }

    private void validateProjectionQuery(BudgetControlProjectionQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getBudgetGroupSn(), "预算组标识不能为空");
        AssertUtils.notNull(query.getCurrency(), "币种不能为空");
        if (query.getTargetAccountId() != null) {
            assertSupportedTargetSubjectType(targetSubjectType(query.getTargetAccountId()));
        }
    }

    private List<SpendControlActivityDTO> queryBudgetProjectionActivities(BudgetControlProjectionQuery query) {
        return queryActivitiesByPage(new SpendControlActivityQuery()
                .setTenantId(query.getTenantId())
                .setBudgetGroupSn(query.getBudgetGroupSn())
                .setCurrency(query.getCurrency())
                .setSpendRuleId(query.getSpendRuleId())
                .setSpendRuleVersion(query.getSpendRuleVersion())
                .setTargetAccountId(query.getTargetAccountId()));
    }

    private List<SpendControlActivityDTO> queryActivitiesByPage(SpendControlActivityQuery query) {
        WindPagination<SpendControlActivityDTO> page = spendControlActivityService.querySpendControlActivities(
                query,
                DefaultPageQueryOptions.defaults(CONTROL_ACTIVITY_QUERY_PAGE_SIZE));
        AssertUtils.isTrue(page.getTotal() <= CONTROL_ACTIVITY_QUERY_PAGE_SIZE,
                "控制额度变动流水查询超过单次读取上限，tenantId = {}, total = {}",
                query.getTenantId(),
                page.getTotal());
        return page.getRecords();
    }

    private BudgetControlProjectionDTO toProjection(BudgetControlProjectionQuery query,
                                                    List<SpendControlActivityDTO> activities) {
        List<SpendControlActivityDTO> budgetActivities = activities.stream()
                .filter(activity -> activity.getActivityType().isBudgetProjectionActivity())
                .toList();
        long limitIncreasedAmount = sumByType(budgetActivities, SpendControlActivityType.LIMIT_INCREASED);
        long limitDecreasedAmount = sumByType(budgetActivities, SpendControlActivityType.LIMIT_DECREASED);
        long limitAmount = limitIncreasedAmount - limitDecreasedAmount;
        long reservedAmount = sumByType(budgetActivities, SpendControlActivityType.RESERVED);
        long grossConsumedAmount = sumByType(budgetActivities, SpendControlActivityType.CONSUMED);
        long refundCompensatedAmount = sumByType(budgetActivities, SpendControlActivityType.REFUND_COMPENSATED);
        long consumedAmount = grossConsumedAmount - refundCompensatedAmount;
        long releasedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType().isReleaseActivity())
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
        long remainingControlAmount = reservedAmount - consumedAmount - releasedAmount;
        long availableControlAmount = limitAmount - consumedAmount - remainingControlAmount;
        SpendControlActivityDTO lastActivity = budgetActivities.isEmpty() ? null : budgetActivities.getLast();
        return new BudgetControlProjectionDTO()
                .setTenantId(query.getTenantId())
                .setBudgetGroupSn(query.getBudgetGroupSn())
                .setCurrency(query.getCurrency())
                .setSpendRuleId(query.getSpendRuleId())
                .setSpendRuleVersion(query.getSpendRuleVersion())
                .setTargetAccountId(query.getTargetAccountId())
                .setLimitIncreasedAmount(limitIncreasedAmount)
                .setLimitDecreasedAmount(limitDecreasedAmount)
                .setLimitAmount(limitAmount)
                .setReservedAmount(reservedAmount)
                .setConsumedAmount(consumedAmount)
                .setReleasedAmount(releasedAmount)
                .setRemainingControlAmount(remainingControlAmount)
                .setAvailableControlAmount(availableControlAmount)
                .setLastActivitySn(lastActivity == null ? null : lastActivity.getActivitySn())
                .setLastActivityAt(lastActivity == null ? null : lastActivity.getGmtCreate());
    }

    private boolean hasNarrowCondition(SpendControlActivityQuery query) {
        return StringUtils.hasText(query.getActivitySn())
                || query.getActivityType() != null
                || StringUtils.hasText(query.getBusinessScene())
                || StringUtils.hasText(query.getBusinessSn())
                || StringUtils.hasText(query.getOriginalActivitySn())
                || StringUtils.hasText(query.getTransactionSn())
                || StringUtils.hasText(query.getInstrumentSn())
                || query.getTargetAccountId() != null
                || query.getCurrency() != null
                || StringUtils.hasText(query.getSpendRuleId())
                || StringUtils.hasText(query.getSpendRuleVersion())
                || StringUtils.hasText(query.getBudgetGroupSn());
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
}
