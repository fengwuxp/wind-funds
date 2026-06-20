package com.wind.funds.wallet.application.spend.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlActivityApplicationService;
import com.wind.funds.wallet.dal.entities.SpendControlActivity;
import com.wind.funds.wallet.dal.entities.table.SpendControlActivityNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendControlActivityMapper;
import com.wind.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 支出控制活动应用服务实现。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Service
@AllArgsConstructor
public class SpendControlActivityApplicationServiceImpl implements SpendControlActivityApplicationService {

    private static final String SENSITIVE_CONTEXT_MESSAGE =
            "contextVariables must not contain sensitive wallet fields";

    private final SpendControlActivityMapper spendControlActivityMapper;

    private final FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request) {
        validateIdempotencyBoundary(request);
        SpendControlActivity existing = findByActivitySn(request.getTenantId(), request.getActivitySn());
        if (existing != null) {
            AssertUtils.isTrue(Objects.equals(existing.getActivityDigest(), request.getActivityDigest()),
                    "控制活动流水已存在但摘要不一致，activitySn = {}",
                    request.getActivitySn());
            return toDTO(existing);
        }
        validateRecordRequest(request);
        assertReleaseAmountNotOverReserved(request);
        SpendControlActivity entity = toEntity(request);
        spendControlActivityMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录支出控制活动失败，activitySn = {}", request.getActivitySn());
        return toDTO(spendControlActivityMapper.selectOneById(entity.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendControlActivityDTO> queryActivities(@NonNull SpendControlActivityQuery query) {
        validateQuery(query);
        return spendControlActivityMapper.selectListByQuery(toQueryWrapper(query)).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull BudgetControlProjectionDTO getBudgetControlProjection(
            @NonNull BudgetControlProjectionQuery query) {
        validateProjectionQuery(query);
        List<SpendControlActivity> activities = spendControlActivityMapper.selectListByQuery(toProjectionWrapper(query));
        return toProjection(query, activities);
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
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(request.getSpendDecisionSn(), "Spend Rule 决策流水号不能为空");
        AssertUtils.notNull(request.getSpendDecisionResult(), "Spend Rule 决策结果不能为空");
        AssertUtils.hasText(request.getSpendDecisionDigest(), "Spend Rule 决策摘要不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
        assertNoSensitiveContextVariables(request.getContextVariables());
        assertTargetAccountSupported(request);
        if (request.getActivityType() == SpendControlActivityType.REJECTED_RECORDED) {
            AssertUtils.isTrue(request.getSpendDecisionResult() == SpendControlDecisionResult.REJECTED,
                    "拒绝控制活动必须对应拒绝决策，activitySn = {}",
                    request.getActivitySn());
            AssertUtils.hasText(request.getRejectReason(), "拒绝控制活动必须提供拒绝原因");
        }
        if (isBudgetActivity(request.getActivityType())) {
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

    private void validateQuery(SpendControlActivityQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        if (query.getTargetAccountId() != null) {
            assertSupportedTargetSubjectType(targetSubjectType(query.getTargetAccountId()));
        }
    }

    private void validateProjectionQuery(BudgetControlProjectionQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getBudgetGroupSn(), "预算组标识不能为空");
        AssertUtils.notNull(query.getCurrency(), "币种不能为空");
    }

    private SpendControlActivity findByActivitySn(Long tenantId, String activitySn) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.activitySn.eq(activitySn));
        return spendControlActivityMapper.selectOneByQuery(wrapper);
    }

    private QueryWrapper toQueryWrapper(SpendControlActivityQuery query) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.activitySn.eq(query.getActivitySn()))
                .and(ref.activityType.eq(query.getActivityType()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
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

    private QueryWrapper toProjectionWrapper(BudgetControlProjectionQuery query) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.budgetGroupSn.eq(query.getBudgetGroupSn()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.spendRuleId.eq(query.getSpendRuleId()))
                .and(ref.spendRuleVersion.eq(query.getSpendRuleVersion()));
        wrapper.orderBy(ref.id.asc());
        return wrapper;
    }

    private void assertReleaseAmountNotOverReserved(RecordSpendControlActivityRequest request) {
        if (!isReleaseActivity(request.getActivityType())) {
            return;
        }
        BudgetControlProjectionQuery query = new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setBudgetGroupSn(request.getBudgetGroupSn())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion());
        BudgetControlProjectionDTO projection = toProjection(query,
                spendControlActivityMapper.selectListByQuery(toProjectionWrapper(query)));
        AssertUtils.isTrue(projection.getRemainingControlAmount() >= request.getAmount(),
                "控制释放金额超过可释放占用金额，activitySn = {}, remainingControlAmount = {}, amount = {}",
                request.getActivitySn(),
                projection.getRemainingControlAmount(),
                request.getAmount());
    }

    private BudgetControlProjectionDTO toProjection(BudgetControlProjectionQuery query,
                                                    List<SpendControlActivity> activities) {
        List<SpendControlActivity> budgetActivities = activities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.RESERVED
                        || isReleaseActivity(activity.getActivityType()))
                .toList();
        long reservedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.RESERVED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long releasedAmount = budgetActivities.stream()
                .filter(activity -> isReleaseActivity(activity.getActivityType()))
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        SpendControlActivity lastActivity = budgetActivities.isEmpty() ? null : budgetActivities.getLast();
        return new BudgetControlProjectionDTO()
                .setTenantId(query.getTenantId())
                .setBudgetGroupSn(query.getBudgetGroupSn())
                .setCurrency(query.getCurrency())
                .setSpendRuleId(query.getSpendRuleId())
                .setSpendRuleVersion(query.getSpendRuleVersion())
                .setReservedAmount(reservedAmount)
                .setReleasedAmount(releasedAmount)
                .setRemainingControlAmount(reservedAmount - releasedAmount)
                .setLastActivitySn(lastActivity == null ? null : lastActivity.getActivitySn())
                .setLastActivityAt(lastActivity == null ? null : lastActivity.getGmtCreate());
    }

    private SpendControlActivity toEntity(RecordSpendControlActivityRequest request) {
        FundsSubjectType targetSubjectType = targetSubjectType(request.getTargetAccountId());
        SpendControlActivity result = new SpendControlActivity();
        result.setTenantId(request.getTenantId());
        result.setActivitySn(request.getActivitySn());
        result.setActivityType(request.getActivityType());
        result.setBusinessScene(request.getBusinessScene());
        result.setBusinessSn(request.getBusinessSn());
        result.setInstrumentSn(request.getInstrumentSn());
        result.setAction(request.getAction());
        result.setTargetSubjectId(request.getTargetAccountId().id());
        result.setTargetSubjectType(targetSubjectType);
        result.setAmount(request.getAmount());
        result.setCurrency(request.getCurrency());
        result.setSpendRuleId(request.getSpendRuleId());
        result.setSpendRuleVersion(request.getSpendRuleVersion());
        result.setSpendDecisionSn(request.getSpendDecisionSn());
        result.setSpendDecisionResult(request.getSpendDecisionResult());
        result.setSpendDecisionDigest(request.getSpendDecisionDigest());
        result.setBudgetGroupSn(request.getBudgetGroupSn());
        result.setRejectReason(request.getRejectReason());
        result.setActivityDigest(request.getActivityDigest());
        result.setDescription(request.getDescription());
        result.setContextVariables(request.getContextVariables());
        return result;
    }

    private SpendControlActivityDTO toDTO(SpendControlActivity entity) {
        return new SpendControlActivityDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setActivitySn(entity.getActivitySn())
                .setActivityType(entity.getActivityType())
                .setBusinessScene(entity.getBusinessScene())
                .setBusinessSn(entity.getBusinessSn())
                .setInstrumentSn(entity.getInstrumentSn())
                .setAction(entity.getAction())
                .setTargetAccountId(FundsAccountId.immutable(entity.getTargetSubjectId(),
                        entity.getTargetSubjectType()))
                .setAmount(entity.getAmount())
                .setCurrency(entity.getCurrency())
                .setSpendRuleId(entity.getSpendRuleId())
                .setSpendRuleVersion(entity.getSpendRuleVersion())
                .setSpendDecisionSn(entity.getSpendDecisionSn())
                .setSpendDecisionResult(entity.getSpendDecisionResult())
                .setSpendDecisionDigest(entity.getSpendDecisionDigest())
                .setBudgetGroupSn(entity.getBudgetGroupSn())
                .setRejectReason(entity.getRejectReason())
                .setActivityDigest(entity.getActivityDigest())
                .setDescription(entity.getDescription())
                .setContextVariables(entity.getContextVariables());
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
        AssertUtils.isFalse(
                PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(contextVariables),
                SENSITIVE_CONTEXT_MESSAGE);
        FundsBenefitSpecValidators.rejectInstructionContextVariables(contextVariables, "wallet");
    }

    private boolean isBudgetActivity(SpendControlActivityType type) {
        return type == SpendControlActivityType.RESERVED
                || type == SpendControlActivityType.RELEASED
                || type == SpendControlActivityType.EXPIRED
                || type == SpendControlActivityType.REVERSED;
    }

    private boolean isReleaseActivity(SpendControlActivityType type) {
        return type == SpendControlActivityType.RELEASED
                || type == SpendControlActivityType.EXPIRED
                || type == SpendControlActivityType.REVERSED;
    }
}
