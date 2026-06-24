package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.dal.entities.SpendControlActivity;
import com.wind.funds.wallet.dal.entities.table.SpendControlActivityNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendControlActivityMapper;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.mapstruct.SpendControlActivityConverter;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.service.SpendControlActivityService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 控制额度变动流水服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendControlActivityServiceImpl implements SpendControlActivityService {

    private static final int CONTROL_ACTIVITY_QUERY_PAGE_SIZE = 500;

    private final SpendControlActivityMapper spendControlActivityMapper;

    private final FundsAccountQueryService fundsAccountQueryService;

    private @NonNull Long insertSpendControlActivity(@NonNull RecordSpendControlActivityRequest request) {
        SpendControlActivity entity = SpendControlActivityConverter.INSTANCE.convertToSpendControlActivity(request);
        spendControlActivityMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录控制额度变动流水失败，activitySn = {}", request.getActivitySn());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request) {
        validateIdempotencyBoundary(request);
        SpendControlActivityDTO existing = findSpendControlActivity(request.getTenantId(), request.getActivitySn());
        if (existing != null) {
            assertSameActivity(request, existing);
            return existing;
        }
        validateRecordRequest(request);
        assertReleaseAmountNotOverReserved(request);
        try {
            Long id = insertSpendControlActivity(request);
            return getSpendControlActivityById(id);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentActivityAfterInsertConflict(request, exception);
        }
    }

    @Override
    public @NonNull SpendControlActivityDTO getSpendControlActivityById(@NonNull Long id) {
        SpendControlActivity result = spendControlActivityMapper.selectOneById(id);
        AssertUtils.notNull(result, "控制额度变动流水不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @Nullable SpendControlActivityDTO findSpendControlActivity(@NonNull Long tenantId,
                                                                      @NonNull String activitySn) {
        SpendControlActivity entity = findActivityEntity(tenantId, activitySn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendControlActivityDTO> querySpendControlActivities(
            @NonNull SpendControlActivityQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return MybatisQueryHelper.<SpendControlActivity, SpendControlActivityDTO>query(toQueryWrapper(query, options))
                .counter(spendControlActivityMapper::selectCountByQuery)
                .resultQueryFunc(spendControlActivityMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

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

    private void validateIdempotencyBoundary(RecordSpendControlActivityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getActivitySn(), "控制活动流水号不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getActivityDigest(), "控制活动摘要");
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
        SpendRuleDigestValidator.assertSha256Digest(request.getActivityDigest(), "控制活动摘要");
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
            SpendRuleDigestValidator.assertSha256Digest(request.getSpendDecisionDigest(), "Spend Rule 决策摘要");
        }
        if (request.getActivityType().isBudgetProjectionActivity()) {
            AssertUtils.hasText(request.getBudgetGroupSn(), "预算控制活动必须提供预算组标识");
        }
    }

    private void assertTargetAccountSupported(RecordSpendControlActivityRequest request) {
        assertSupportedTargetSubjectType(targetSubjectType(request.getTargetAccountId()));
        FundsAccount account = fundsAccountQueryService.getAccount(request.getTargetAccountId());
        AssertUtils.isTrue(Objects.equals(account.getTenantId(), request.getTenantId()),
                "控制活动目标账户租户不匹配，accountId = {}，tenantId = {}",
                request.getTargetAccountId(),
                request.getTenantId());
        AssertUtils.isTrue(account.getCurrency() == request.getCurrency(),
                "控制活动目标账户币种不匹配，accountId = {}，currency = {}",
                request.getTargetAccountId(),
                request.getCurrency());
    }

    private SpendControlActivityDTO readIdempotentActivityAfterInsertConflict(
            RecordSpendControlActivityRequest request,
            DataIntegrityViolationException exception) {
        SpendControlActivityDTO existing = findSpendControlActivity(request.getTenantId(), request.getActivitySn());
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
        long remainingControlAmount = remainingControlAmount(queryBudgetProjectionActivities(
                new BudgetControlProjectionQuery()
                        .setTenantId(request.getTenantId())
                        .setBudgetGroupSn(request.getBudgetGroupSn())
                        .setCurrency(request.getCurrency())
                        .setSpendRuleId(request.getSpendRuleId())
                        .setSpendRuleVersion(request.getSpendRuleVersion())
                        .setTargetAccountId(request.getTargetAccountId())));
        AssertUtils.isTrue(remainingControlAmount >= request.getAmount(),
                "控制释放金额超过可释放占用金额，activitySn = {}, remainingControlAmount = {}, amount = {}",
                request.getActivitySn(),
                remainingControlAmount,
                request.getAmount());
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
        WindPagination<SpendControlActivityDTO> page = querySpendControlActivities(
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
        List<SpendControlActivityDTO> budgetActivities = budgetProjectionActivities(activities);
        long limitIncreasedAmount = sumByType(budgetActivities, SpendControlActivityType.LIMIT_INCREASED);
        long limitDecreasedAmount = sumByType(budgetActivities, SpendControlActivityType.LIMIT_DECREASED);
        long limitAmount = limitIncreasedAmount - limitDecreasedAmount;
        long reservedAmount = sumByType(budgetActivities, SpendControlActivityType.RESERVED);
        long consumedAmount = consumedAmount(budgetActivities);
        long releasedAmount = releasedAmount(budgetActivities);
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

    private List<SpendControlActivityDTO> budgetProjectionActivities(List<SpendControlActivityDTO> activities) {
        return activities.stream()
                .filter(activity -> activity.getActivityType().isBudgetProjectionActivity())
                .toList();
    }

    private long remainingControlAmount(List<SpendControlActivityDTO> activities) {
        List<SpendControlActivityDTO> budgetActivities = budgetProjectionActivities(activities);
        return sumByType(budgetActivities, SpendControlActivityType.RESERVED)
                - consumedAmount(budgetActivities)
                - releasedAmount(budgetActivities);
    }

    private long consumedAmount(List<SpendControlActivityDTO> activities) {
        long grossConsumedAmount = sumByType(activities, SpendControlActivityType.CONSUMED);
        long refundCompensatedAmount = sumByType(activities, SpendControlActivityType.REFUND_COMPENSATED);
        return grossConsumedAmount - refundCompensatedAmount;
    }

    private long releasedAmount(List<SpendControlActivityDTO> activities) {
        return activities.stream()
                .filter(activity -> activity.getActivityType().isReleaseActivity())
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
    }

    private long sumByType(List<SpendControlActivityDTO> activities, SpendControlActivityType activityType) {
        return activities.stream()
                .filter(activity -> activity.getActivityType() == activityType)
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
    }

    private void assertNoSensitiveContextVariables(String contextVariables) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(contextVariables);
    }

    private SpendControlActivity findActivityEntity(Long tenantId, String activitySn) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.activitySn.eq(activitySn));
        return spendControlActivityMapper.selectOneByQuery(wrapper);
    }

    private QueryWrapper toQueryWrapper(SpendControlActivityQuery query,
                                        WindQuery<? extends QueryOrderField> options) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.activitySn.eq(query.getActivitySn()))
                .and(ref.activityType.eq(query.getActivityType()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
                .and(ref.originalActivitySn.eq(query.getOriginalActivitySn()))
                .and(ref.transactionSn.eq(query.getTransactionSn()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.spendRuleId.eq(query.getSpendRuleId()))
                .and(ref.spendRuleVersion.eq(query.getSpendRuleVersion()))
                .and(ref.budgetGroupSn.eq(query.getBudgetGroupSn()));
        if (query.getTargetAccountId() != null) {
            wrapper.and(ref.targetSubjectId.eq(query.getTargetAccountId().id()))
                    .and(ref.targetSubjectType.eq(targetSubjectType(query.getTargetAccountId())));
        }
        wrapper.orderBy(ref.id.asc());
        return wrapper;
    }

    private SpendControlActivityDTO toDTO(SpendControlActivity entity) {
        return SpendControlActivityConverter.INSTANCE.convertToSpendControlActivityDTO(entity);
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
