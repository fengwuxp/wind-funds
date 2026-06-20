package com.wind.funds.wallet.application.spend.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.wallet.application.spend.SpendControlActivityApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlTransactionConsumptionApplicationService;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 交易结果消费支出控制活动应用服务实现。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Service
@AllArgsConstructor
public class SpendControlTransactionConsumptionApplicationServiceImpl
        implements SpendControlTransactionConsumptionApplicationService {

    private final SpendControlActivityApplicationService spendControlActivityApplicationService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO consume(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlActivityDTO originalActivity = getOriginalReservedActivity(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertClosedTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() != DefaultFundsTransactionType.REFUND,
                "控制消费不能使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlActivityMatchesOriginalActivity(request, originalActivity, "控制消费");
        assertControlActivityMatchesTransaction(request, transaction, "控制消费");
        assertEnoughRemainingControlAmount(request, originalActivity, "控制消费金额超过原占用剩余额度");
        return spendControlActivityApplicationService.recordActivity(
                toRecordRequest(request, originalActivity, SpendControlActivityType.CONSUMED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO release(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlActivityDTO originalActivity = getOriginalReservedActivity(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertReleasableTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() != DefaultFundsTransactionType.REFUND,
                "控制释放不能使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlActivityMatchesOriginalActivity(request, originalActivity, "控制释放");
        assertControlActivityMatchesTransaction(request, transaction, "控制释放");
        assertEnoughRemainingControlAmount(request, originalActivity, "控制释放金额超过原占用剩余额度");
        return spendControlActivityApplicationService.recordActivity(
                toRecordRequest(request, originalActivity, SpendControlActivityType.RELEASED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlActivityDTO refund(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlActivityDTO originalActivity = getOriginalReservedActivity(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertClosedTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() == DefaultFundsTransactionType.REFUND,
                "退款控制补偿必须使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlActivityMatchesOriginalActivity(request, originalActivity, "退款控制补偿");
        assertControlActivityMatchesTransaction(request, transaction, "退款控制补偿");
        assertRefundReferencesConsumedTransaction(request, transaction);
        assertRefundDoesNotExceedNetConsumedAmount(request, originalActivity);
        return spendControlActivityApplicationService.recordActivity(
                toRecordRequest(request, originalActivity, SpendControlActivityType.REFUND_COMPENSATED));
    }

    private void validateTransactionControlRequest(SpendControlTransactionConsumptionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getActivitySn(), "控制活动流水号不能为空");
        AssertUtils.hasText(request.getOriginalActivitySn(), "原控制活动流水号不能为空");
        AssertUtils.hasText(request.getTransactionSn(), "资金交易流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制活动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getActivityDigest(), "控制活动摘要不能为空");
    }

    private SpendControlActivityDTO getOriginalReservedActivity(SpendControlTransactionConsumptionRequest request) {
        List<SpendControlActivityDTO> activities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(request.getTenantId())
                        .setActivitySn(request.getOriginalActivitySn()));
        AssertUtils.isTrue(activities.size() == 1,
                "原控制活动不存在，originalActivitySn = {}", request.getOriginalActivitySn());
        SpendControlActivityDTO originalActivity = activities.getFirst();
        AssertUtils.isTrue(originalActivity.getActivityType() == SpendControlActivityType.RESERVED,
                "原控制活动必须是 RESERVED，originalActivitySn = {}", request.getOriginalActivitySn());
        return originalActivity;
    }

    private FundsTransactionDTO getExistingFundsTransaction(SpendControlTransactionConsumptionRequest request) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(request.getTransactionSn())
                .orElse(null);
        AssertUtils.notNull(transaction, "资金交易不存在，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), request.getTenantId()),
                "资金交易租户不一致，transactionSn = {}", request.getTransactionSn());
        return transaction;
    }

    private void assertClosedTransaction(SpendControlTransactionConsumptionRequest request,
                                         FundsTransactionDTO transaction) {
        AssertUtils.isTrue(transaction.getStatus() == FundsTransactionStatus.CLOSED,
                "资金交易必须已关闭，transactionSn = {}", request.getTransactionSn());
    }

    private void assertReleasableTransaction(SpendControlTransactionConsumptionRequest request,
                                             FundsTransactionDTO transaction) {
        AssertUtils.isTrue(transaction.getStatus() == FundsTransactionStatus.FAILED
                        || transaction.getStatus() == FundsTransactionStatus.REJECTED
                        || transaction.getStatus() == FundsTransactionStatus.EXPIRED,
                "控制释放必须使用失败、拒绝或过期交易事实，transactionSn = {}", request.getTransactionSn());
    }

    private void assertControlActivityMatchesOriginalActivity(SpendControlTransactionConsumptionRequest request,
                                                              SpendControlActivityDTO originalActivity,
                                                              String actionName) {
        AssertUtils.isTrue(Objects.equals(originalActivity.getTargetAccountId(), request.getTargetAccountId()),
                "{}目标账户不一致，activitySn = {}", actionName, request.getActivitySn());
        AssertUtils.isTrue(originalActivity.getCurrency() == request.getCurrency(),
                "{}币种不一致，activitySn = {}", actionName, request.getActivitySn());
        AssertUtils.isTrue(originalActivity.getAmount() >= request.getAmount(),
                "{}金额超过原占用金额，activitySn = {}", actionName, request.getActivitySn());
    }

    private void assertControlActivityMatchesTransaction(SpendControlTransactionConsumptionRequest request,
                                                         FundsTransactionDTO transaction,
                                                         String actionName) {
        AssertUtils.isTrue(transaction.getCurrency() == request.getCurrency(),
                "资金交易币种不一致，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(transaction.getAmount() >= request.getAmount(),
                "{}金额超过资金交易金额，transactionSn = {}", actionName, request.getTransactionSn());
    }

    private void assertEnoughRemainingControlAmount(SpendControlTransactionConsumptionRequest request,
                                                    SpendControlActivityDTO originalActivity,
                                                    String message) {
        ControlActivityUsage usage = controlActivityUsage(request, originalActivity);
        AssertUtils.isTrue(usage.remainingAmount() >= request.getAmount(),
                "{}, activitySn = {}, remainingControlAmount = {}, amount = {}",
                message,
                request.getActivitySn(),
                usage.remainingAmount(),
                request.getAmount());
    }

    private void assertRefundDoesNotExceedNetConsumedAmount(SpendControlTransactionConsumptionRequest request,
                                                            SpendControlActivityDTO originalActivity) {
        ControlActivityUsage usage = controlActivityUsage(request, originalActivity);
        AssertUtils.isTrue(usage.netConsumedAmount() >= request.getAmount(),
                "退款控制补偿金额超过已消费控制金额，activitySn = {}, netConsumedAmount = {}, amount = {}",
                request.getActivitySn(),
                usage.netConsumedAmount(),
                request.getAmount());
    }

    private void assertRefundReferencesConsumedTransaction(SpendControlTransactionConsumptionRequest request,
                                                          FundsTransactionDTO transaction) {
        AssertUtils.hasText(transaction.getReferenceTransactionSn(),
                "退款交易必须引用原消费交易，transactionSn = {}", request.getTransactionSn());
        List<SpendControlActivityDTO> consumedActivities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalActivitySn(request.getOriginalActivitySn())
                        .setActivityType(SpendControlActivityType.CONSUMED)
                        .setTransactionSn(transaction.getReferenceTransactionSn()));
        AssertUtils.isFalse(consumedActivities.isEmpty(),
                "退款交易未关联已消费控制活动，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                transaction.getReferenceTransactionSn());
    }

    private ControlActivityUsage controlActivityUsage(SpendControlTransactionConsumptionRequest request,
                                                      SpendControlActivityDTO originalActivity) {
        List<SpendControlActivityDTO> linkedActivities = spendControlActivityApplicationService.queryActivities(
                new SpendControlActivityQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalActivitySn(request.getOriginalActivitySn()));
        List<SpendControlActivityDTO> effectiveActivities = linkedActivities.stream()
                .filter(activity -> !Objects.equals(activity.getActivitySn(), request.getActivitySn()))
                .toList();
        long grossConsumedAmount = sumAmount(effectiveActivities, SpendControlActivityType.CONSUMED);
        long refundCompensatedAmount = sumAmount(effectiveActivities, SpendControlActivityType.REFUND_COMPENSATED);
        long releasedAmount = effectiveActivities.stream()
                .filter(activity -> isReleaseActivity(activity.getActivityType()))
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
        return new ControlActivityUsage(originalActivity.getAmount(),
                grossConsumedAmount,
                refundCompensatedAmount,
                releasedAmount);
    }

    private long sumAmount(List<SpendControlActivityDTO> activities, SpendControlActivityType activityType) {
        return activities.stream()
                .filter(activity -> activity.getActivityType() == activityType)
                .mapToLong(SpendControlActivityDTO::getAmount)
                .sum();
    }

    private boolean isReleaseActivity(SpendControlActivityType activityType) {
        return activityType == SpendControlActivityType.RELEASED
                || activityType == SpendControlActivityType.EXPIRED
                || activityType == SpendControlActivityType.REVERSED;
    }

    private RecordSpendControlActivityRequest toRecordRequest(SpendControlTransactionConsumptionRequest request,
                                                              SpendControlActivityDTO originalActivity,
                                                              SpendControlActivityType activityType) {
        return new RecordSpendControlActivityRequest()
                .setTenantId(request.getTenantId())
                .setActivitySn(request.getActivitySn())
                .setActivityType(activityType)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setOriginalActivitySn(request.getOriginalActivitySn())
                .setTransactionSn(request.getTransactionSn())
                .setInstrumentSn(originalActivity.getInstrumentSn())
                .setAction(originalActivity.getAction())
                .setTargetAccountId(request.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(originalActivity.getSpendRuleId())
                .setSpendRuleVersion(originalActivity.getSpendRuleVersion())
                .setSpendDecisionSn(originalActivity.getSpendDecisionSn())
                .setSpendDecisionResult(originalActivity.getSpendDecisionResult())
                .setSpendDecisionDigest(originalActivity.getSpendDecisionDigest())
                .setBudgetGroupSn(originalActivity.getBudgetGroupSn())
                .setActivityDigest(request.getActivityDigest())
                .setDescription(request.getDescription())
                .setContextVariables(request.getContextVariables());
    }

    private record ControlActivityUsage(long originalReservedAmount,
                                        long grossConsumedAmount,
                                        long refundCompensatedAmount,
                                        long releasedAmount) {

        private long netConsumedAmount() {
            return grossConsumedAmount - refundCompensatedAmount;
        }

        private long remainingAmount() {
            return originalReservedAmount - netConsumedAmount() - releasedAmount;
        }
    }
}
