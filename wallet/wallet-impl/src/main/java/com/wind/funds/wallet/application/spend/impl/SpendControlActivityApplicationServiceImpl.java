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
import org.springframework.dao.DataIntegrityViolationException;
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
            assertSameActivity(request, existing);
            return toDTO(existing);
        }
        validateRecordRequest(request);
        assertReleaseAmountNotOverReserved(request);
        SpendControlActivity entity = toEntity(request);
        try {
            spendControlActivityMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentActivityAfterInsertConflict(request, exception);
        }
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
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
        assertNoSensitiveContextVariables(request.getContextVariables());
        assertTargetAccountSupported(request);
        if (isLimitAdjustmentActivity(request.getActivityType())) {
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
        if (query.getTargetAccountId() != null) {
            assertSupportedTargetSubjectType(targetSubjectType(query.getTargetAccountId()));
        }
    }

    private SpendControlActivity findByActivitySn(Long tenantId, String activitySn) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.activitySn.eq(activitySn));
        return spendControlActivityMapper.selectOneByQuery(wrapper);
    }

    private SpendControlActivityDTO readIdempotentActivityAfterInsertConflict(
            RecordSpendControlActivityRequest request,
            DataIntegrityViolationException exception) {
        SpendControlActivity existing = findByActivitySn(request.getTenantId(), request.getActivitySn());
        if (existing == null) {
            throw exception;
        }
        assertSameActivity(request, existing);
        return toDTO(existing);
    }

    private void assertSameActivity(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
        assertSameActivityDigest(request, existing);
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        assertSameActivityIdentity(request, existing);
        assertSameActivityTarget(request, existing);
        assertSameActivityAmountAndRule(request, existing);
        assertSameActivityDecision(request, existing);
        assertSameActivityAudit(request, existing);
    }

    private void assertSameActivityIdentity(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
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

    private void assertSameActivityTarget(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
        AssertUtils.isTrue(Objects.equals(existing.getTargetSubjectId(), request.getTargetAccountId().id())
                        && existing.getTargetSubjectType() == targetSubjectType(request.getTargetAccountId()),
                "控制活动流水已存在但目标账户不一致，activitySn = {}",
                request.getActivitySn());
    }

    private void assertSameActivityAmountAndRule(RecordSpendControlActivityRequest request,
            SpendControlActivity existing) {
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

    private void assertSameActivityDecision(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
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

    private void assertSameActivityAudit(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
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

    private void assertSameActivityDigest(RecordSpendControlActivityRequest request, SpendControlActivity existing) {
        AssertUtils.isTrue(Objects.equals(existing.getActivityDigest(), request.getActivityDigest()),
                "控制活动流水已存在但摘要不一致，activitySn = {}",
                request.getActivitySn());
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

    private QueryWrapper toProjectionWrapper(BudgetControlProjectionQuery query) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.budgetGroupSn.eq(query.getBudgetGroupSn()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.spendRuleId.eq(query.getSpendRuleId()))
                .and(ref.spendRuleVersion.eq(query.getSpendRuleVersion()));
        if (query.getTargetAccountId() != null) {
            wrapper.and(ref.targetSubjectId.eq(query.getTargetAccountId().id()))
                    .and(ref.targetSubjectType.eq(targetSubjectType(query.getTargetAccountId())));
        }
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
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId());
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
                .filter(activity -> isBudgetActivity(activity.getActivityType()))
                .toList();
        long limitIncreasedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.LIMIT_INCREASED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long limitDecreasedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.LIMIT_DECREASED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long limitAmount = limitIncreasedAmount - limitDecreasedAmount;
        long reservedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.RESERVED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long grossConsumedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.CONSUMED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long refundCompensatedAmount = budgetActivities.stream()
                .filter(activity -> activity.getActivityType() == SpendControlActivityType.REFUND_COMPENSATED)
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long consumedAmount = grossConsumedAmount - refundCompensatedAmount;
        long releasedAmount = budgetActivities.stream()
                .filter(activity -> isReleaseActivity(activity.getActivityType()))
                .mapToLong(SpendControlActivity::getAmount)
                .sum();
        long remainingControlAmount = reservedAmount - consumedAmount - releasedAmount;
        SpendControlActivity lastActivity = budgetActivities.isEmpty() ? null : budgetActivities.getLast();
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
                .setAvailableControlAmount(limitAmount - remainingControlAmount)
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
        result.setOriginalActivitySn(request.getOriginalActivitySn());
        result.setTransactionSn(request.getTransactionSn());
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
        result.setReasonCode(request.getReasonCode());
        result.setOperatorId(request.getOperatorId());
        result.setAuditReferenceSn(request.getAuditReferenceSn());
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
                .setOriginalActivitySn(entity.getOriginalActivitySn())
                .setTransactionSn(entity.getTransactionSn())
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
                .setReasonCode(entity.getReasonCode())
                .setOperatorId(entity.getOperatorId())
                .setAuditReferenceSn(entity.getAuditReferenceSn())
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
        return type == SpendControlActivityType.LIMIT_INCREASED
                || type == SpendControlActivityType.LIMIT_DECREASED
                || type == SpendControlActivityType.RESERVED
                || type == SpendControlActivityType.CONSUMED
                || type == SpendControlActivityType.REFUND_COMPENSATED
                || type == SpendControlActivityType.RELEASED
                || type == SpendControlActivityType.EXPIRED
                || type == SpendControlActivityType.REVERSED;
    }

    private boolean isLimitAdjustmentActivity(SpendControlActivityType type) {
        return type == SpendControlActivityType.LIMIT_INCREASED
                || type == SpendControlActivityType.LIMIT_DECREASED;
    }

    private boolean isReleaseActivity(SpendControlActivityType type) {
        return type == SpendControlActivityType.RELEASED
                || type == SpendControlActivityType.EXPIRED
                || type == SpendControlActivityType.REVERSED;
    }
}
