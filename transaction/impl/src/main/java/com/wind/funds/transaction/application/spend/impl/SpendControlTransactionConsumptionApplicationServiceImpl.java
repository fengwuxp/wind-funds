package com.wind.funds.transaction.application.spend.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.application.spend.SpendControlBusinessConfirmedRefundCompensationRequest;
import com.wind.funds.transaction.application.spend.SpendControlTransactionConsumptionRequest;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentCapabilityApplicationService;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Transaction Provider 内部交易结果消费控制额度变动编排服务。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Slf4j
@Service
@AllArgsConstructor
public class SpendControlTransactionConsumptionApplicationServiceImpl {

    private static final String SHA256_PREFIX = "sha256:";

    private static final String REFUND_COMPENSATION_DIGEST_DOMAIN = "wallet.spend-control.refund-compensation";

    private final SpendControlMovementService spendControlMovementService;

    private final PaymentInstrumentCapabilityApplicationService paymentInstrumentCapabilityApplicationService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO consume(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlMovementDTO originalMovement = getOriginalReservedMovement(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertConsumableTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() != DefaultFundsTransactionType.REFUND,
                "控制消费不能使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlMovementMatchesOriginalMovement(request, originalMovement, "控制消费");
        assertControlMovementMatchesTransaction(request, transaction, "控制消费");
        assertTransactionBusinessSnMatches(request, transaction);
        assertEnoughRemainingControlAmount(request, originalMovement, "控制消费金额超过原占用剩余额度");
        assertTransactionControlAmountNotExceeded(request, transaction, SpendControlMovementType.CONSUMED, "控制消费");
        SpendControlMovementDTO consumedMovement = spendControlMovementService.recordMovement(
                toRecordRequest(request, originalMovement, SpendControlMovementType.CONSUMED));
        assertConsumeBackedByTrustedCompletion(request, transaction);
        logMovement("控制消费", request, consumedMovement);
        return consumedMovement;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO release(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlMovementDTO originalMovement = getOriginalReservedMovement(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        AssertUtils.isTrue(transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION,
                "控制释放必须使用授权交易事实，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(Objects.equals(originalMovement.getTransactionSn(), request.getTransactionSn()),
                "控制释放资金交易与原控制占用不一致，transactionSn = {}, originalMovementSn = {}",
                request.getTransactionSn(), request.getOriginalMovementSn());
        assertControlMovementMatchesOriginalMovement(request, originalMovement, "控制释放");
        assertControlMovementMatchesTransaction(request, transaction, "控制释放");
        assertTransactionBusinessSnMatches(request, transaction);
        assertEnoughRemainingControlAmount(request, originalMovement, "控制释放金额超过原占用剩余额度");
        SpendControlMovementDTO releasedMovement = spendControlMovementService.recordMovement(
                toRecordRequest(request, originalMovement, SpendControlMovementType.RELEASED));
        assertReleaseBackedByTrustedReversal(request, transaction);
        logMovement("控制释放", request, releasedMovement);
        return releasedMovement;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO refund(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlMovementDTO originalMovement = getOriginalReservedMovement(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertClosedTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() == DefaultFundsTransactionType.REFUND,
                "退款控制补偿必须使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlMovementMatchesOriginalMovement(request, originalMovement, "退款控制补偿");
        assertControlMovementMatchesTransaction(request, transaction, "退款控制补偿");
        List<SpendControlMovementDTO> referencedConsumedMovements = assertRefundReferencesConsumedTransaction(request,
                transaction);
        assertRefundReferenceTransactionValid(request, transaction);
        assertReferencedConsumedMovementsMatchOriginalMovement(originalMovement, referencedConsumedMovements);
        assertRefundDoesNotExceedReferencedConsumedAmount(request, transaction, referencedConsumedMovements);
        assertRefundDoesNotExceedNetConsumedAmount(request, originalMovement);
        assertTransactionControlAmountNotExceeded(request, transaction, SpendControlMovementType.REFUND_COMPENSATED,
                "退款控制补偿");
        SpendControlMovementDTO movement = spendControlMovementService.recordMovement(
                toRecordRequest(request, originalMovement, SpendControlMovementType.REFUND_COMPENSATED));
        logMovement("退款控制补偿", request, movement);
        return movement;
    }

    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO compensateBusinessConfirmedRefund(
            @NonNull SpendControlBusinessConfirmedRefundCompensationRequest request) {
        validateBusinessConfirmedRefundRequest(request);
        RecordSpendControlMovementRequest recordRequest = toRecordRequest(request);
        if (hasRecordedMovement(request)) {
            SpendControlMovementDTO movement = spendControlMovementService.recordMovement(recordRequest);
            log.info("业务确认退款控制补偿幂等复用，tenantId={}, movementSn={}, businessScene={}, businessSn={}, "
                            + "amount={}, currency={}",
                    request.getTenantId(), request.getMovementSn(), request.getBusinessScene(), request.getBusinessSn(),
                    request.getAmount(), request.getCurrency());
            return movement;
        }
        assertPaymentInstrumentAvailable(request);
        SpendControlMovementDTO movement = spendControlMovementService.recordMovement(recordRequest);
        log.info("业务确认退款控制补偿完成，等待事务提交，tenantId={}, movementSn={}, businessScene={}, "
                        + "businessSn={}, amount={}, currency={}",
                request.getTenantId(), request.getMovementSn(), request.getBusinessScene(), request.getBusinessSn(),
                request.getAmount(), request.getCurrency());
        return movement;
    }

    private void logMovement(String action,
                             SpendControlTransactionConsumptionRequest request,
                             SpendControlMovementDTO movement) {
        log.info("{}完成，等待事务提交，tenantId={}, movementSn={}, originalMovementSn={}, transactionSn={}, "
                        + "businessScene={}, businessSn={}, movementType={}, amount={}, currency={}",
                action, request.getTenantId(), request.getMovementSn(), request.getOriginalMovementSn(),
                request.getTransactionSn(), request.getBusinessScene(), request.getBusinessSn(),
                movement.getMovementType(), request.getAmount(), request.getCurrency());
    }

    private void validateTransactionControlRequest(SpendControlTransactionConsumptionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "控制额度变动 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getMovementSn(), "控制额度变动流水号不能为空");
        AssertUtils.hasText(request.getOriginalMovementSn(), "原控制额度变动流水号不能为空");
        AssertUtils.hasText(request.getTransactionSn(), "资金交易流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制额度变动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getMovementDigest(), "控制额度变动摘要");
    }

    private void validateBusinessConfirmedRefundRequest(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "控制额度变动 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getMovementSn(), "控制额度变动流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "控制额度变动目标账户不能为空");
        AssertUtils.notNull(request.getAmount(), "控制金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "控制金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(controlScopeId(request), "控制范围标识不能为空");
        AssertUtils.hasText(request.getPeriodId(), "控制周期标识不能为空");
        AssertUtils.hasText(request.getReasonCode(), "业务确认退款控制补偿原因码不能为空");
        AssertUtils.hasText(request.getOperatorId(), "业务确认退款控制补偿操作者不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "业务确认退款控制补偿审计引用不能为空");
    }

    private void assertPaymentInstrumentAvailable(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        paymentInstrumentCapabilityApplicationService.resolvePaymentInstrumentCapability(
                new ResolvePaymentInstrumentCapabilityRequest()
                        .setTenantId(request.getTenantId())
                        .setInstrumentSn(request.getInstrumentSn())
                        .setAction(PaymentInstrumentAction.REFUND)
                        .setCurrency(request.getCurrency()));
    }

    private boolean hasRecordedMovement(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        return !spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn())).isEmpty();
    }

    private SpendControlMovementDTO getOriginalReservedMovement(SpendControlTransactionConsumptionRequest request) {
        List<SpendControlMovementDTO> movements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setMovementSn(request.getOriginalMovementSn()));
        AssertUtils.isTrue(movements.size() == 1,
                "原控制额度变动不存在，originalMovementSn = {}", request.getOriginalMovementSn());
        SpendControlMovementDTO originalMovement = movements.getFirst();
        AssertUtils.isTrue(originalMovement.getMovementType() == SpendControlMovementType.RESERVED,
                "原控制额度变动必须是 RESERVED，originalMovementSn = {}", request.getOriginalMovementSn());
        return originalMovement;
    }

    private FundsTransactionDTO getExistingFundsTransaction(SpendControlTransactionConsumptionRequest request) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.findFundsTransactionBySn(
                        request.getTenantId(), request.getTransactionSn())
                .orElse(null);
        AssertUtils.notNull(transaction, "资金交易不存在，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), request.getTenantId()),
                "资金交易租户不一致，transactionSn = {}", request.getTransactionSn());
        return transaction;
    }

    private void assertClosedTransaction(SpendControlTransactionConsumptionRequest request,
                                         FundsTransactionDTO transaction) {
        AssertUtils.isTrue(transaction.getState() == FundsTransactionState.CLOSED,
                "资金交易必须已关闭，transactionSn = {}", request.getTransactionSn());
    }

    private void assertConsumableTransaction(SpendControlTransactionConsumptionRequest request,
                                             FundsTransactionDTO transaction) {
        if (transaction.getTransactionMode() != FundsTransactionMode.AUTHORIZATION) {
            assertClosedTransaction(request, transaction);
            return;
        }
        long completedAmount = transaction.getCompletedAmount() == null ? 0L : transaction.getCompletedAmount();
        AssertUtils.isTrue(completedAmount > 0L,
                "授权交易必须已有可信完成金额，transactionSn = {}", request.getTransactionSn());
    }

    private void assertControlMovementMatchesOriginalMovement(SpendControlTransactionConsumptionRequest request,
                                                              SpendControlMovementDTO originalMovement,
                                                              String actionName) {
        AssertUtils.isTrue(Objects.equals(originalMovement.getBusinessScene(), request.getBusinessScene()),
                "{}业务场景不一致，movementSn = {}", actionName, request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(originalMovement.getBusinessSn(), request.getBusinessSn()),
                "{}业务流水不一致，movementSn = {}", actionName, request.getMovementSn());
        AssertUtils.isTrue(Objects.equals(originalMovement.getTargetAccountId(), request.getTargetAccountId()),
                "{}目标账户不一致，movementSn = {}", actionName, request.getMovementSn());
        AssertUtils.isTrue(originalMovement.getCurrency() == request.getCurrency(),
                "{}币种不一致，movementSn = {}", actionName, request.getMovementSn());
        AssertUtils.isTrue(originalMovement.getAmount() >= request.getAmount(),
                "{}金额超过原占用金额，movementSn = {}", actionName, request.getMovementSn());
    }

    private void assertControlMovementMatchesTransaction(SpendControlTransactionConsumptionRequest request,
                                                         FundsTransactionDTO transaction,
                                                         String actionName) {
        AssertUtils.isTrue(Objects.equals(transaction.getBusinessScene(), request.getBusinessScene()),
                "资金交易业务场景不一致，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(transaction.getCurrency() == request.getCurrency(),
                "资金交易币种不一致，transactionSn = {}", request.getTransactionSn());
        AssertUtils.isTrue(transaction.getAmount() >= request.getAmount(),
                "{}金额超过资金交易金额，transactionSn = {}", actionName, request.getTransactionSn());
    }

    private void assertTransactionBusinessSnMatches(SpendControlTransactionConsumptionRequest request,
                                                    FundsTransactionDTO transaction) {
        AssertUtils.isTrue(Objects.equals(transaction.getBusinessSn(), request.getBusinessSn()),
                "资金交易业务流水不一致，transactionSn = {}", request.getTransactionSn());
    }

    private void assertTransactionControlAmountNotExceeded(SpendControlTransactionConsumptionRequest request,
                                                           FundsTransactionDTO transaction,
                                                           SpendControlMovementType movementType,
                                                           String actionName) {
        List<SpendControlMovementDTO> transactionMovements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn())
                        .setMovementType(movementType)
                        .setTransactionSn(request.getTransactionSn()));
        long usedTransactionAmount = transactionMovements.stream()
                .filter(activity -> !Objects.equals(activity.getMovementSn(), request.getMovementSn()))
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
        long remainingTransactionAmount = transaction.getAmount() - usedTransactionAmount;
        AssertUtils.isTrue(remainingTransactionAmount >= request.getAmount(),
                "{}累计金额超过资金交易金额，movementSn = {}, transactionSn = {}, "
                        + "remainingTransactionAmount = {}, amount = {}",
                actionName,
                request.getMovementSn(),
                request.getTransactionSn(),
                remainingTransactionAmount,
                request.getAmount());
    }

    private void assertReleaseBackedByTrustedReversal(SpendControlTransactionConsumptionRequest request,
                                                      FundsTransactionDTO transaction) {
        long releasedAmount = spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn())
                        .setMovementType(SpendControlMovementType.RELEASED)
                        .setTransactionSn(request.getTransactionSn()))
                .stream()
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
        long trustedReversedAmount = transaction.getReversedAmount() == null ? 0L : transaction.getReversedAmount();
        AssertUtils.isTrue(releasedAmount <= trustedReversedAmount,
                "控制释放累计金额超过资金交易可信撤销金额，movementSn = {}, transactionSn = {}, "
                        + "trustedReversedAmount = {}, amount = {}",
                request.getMovementSn(),
                request.getTransactionSn(),
                trustedReversedAmount,
                request.getAmount());
    }

    private void assertConsumeBackedByTrustedCompletion(SpendControlTransactionConsumptionRequest request,
                                                        FundsTransactionDTO transaction) {
        if (transaction.getTransactionMode() != FundsTransactionMode.AUTHORIZATION) {
            return;
        }
        long consumedAmount = spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn())
                        .setMovementType(SpendControlMovementType.CONSUMED)
                        .setTransactionSn(request.getTransactionSn()))
                .stream()
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
        long trustedCompletedAmount = transaction.getCompletedAmount() == null ? 0L : transaction.getCompletedAmount();
        AssertUtils.isTrue(consumedAmount <= trustedCompletedAmount,
                "控制消费累计金额超过资金交易可信完成金额，movementSn = {}, transactionSn = {}, "
                        + "trustedCompletedAmount = {}, amount = {}",
                request.getMovementSn(),
                request.getTransactionSn(),
                trustedCompletedAmount,
                request.getAmount());
    }

    private void assertEnoughRemainingControlAmount(SpendControlTransactionConsumptionRequest request,
                                                    SpendControlMovementDTO originalMovement,
                                                    String message) {
        ControlMovementUsage usage = controlMovementUsage(request, originalMovement);
        AssertUtils.isTrue(usage.remainingAmount() >= request.getAmount(),
                "{}, movementSn = {}, remainingControlAmount = {}, amount = {}",
                message,
                request.getMovementSn(),
                usage.remainingAmount(),
                request.getAmount());
    }

    private void assertRefundDoesNotExceedNetConsumedAmount(SpendControlTransactionConsumptionRequest request,
                                                            SpendControlMovementDTO originalMovement) {
        ControlMovementUsage usage = controlMovementUsage(request, originalMovement);
        AssertUtils.isTrue(usage.netConsumedAmount() >= request.getAmount(),
                "退款控制补偿金额超过已消费控制金额，movementSn = {}, netConsumedAmount = {}, amount = {}",
                request.getMovementSn(),
                usage.netConsumedAmount(),
                request.getAmount());
    }

    private List<SpendControlMovementDTO> assertRefundReferencesConsumedTransaction(
            SpendControlTransactionConsumptionRequest request,
            FundsTransactionDTO transaction) {
        AssertUtils.hasText(transaction.getReferenceTransactionSn(),
                "退款交易必须引用原消费交易，transactionSn = {}", request.getTransactionSn());
        List<SpendControlMovementDTO> consumedMovements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn())
                        .setMovementType(SpendControlMovementType.CONSUMED)
                        .setTransactionSn(transaction.getReferenceTransactionSn()));
        AssertUtils.isFalse(consumedMovements.isEmpty(),
                "退款交易未关联已消费控制额度变动，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                transaction.getReferenceTransactionSn());
        return consumedMovements;
    }

    private void assertRefundReferenceTransactionValid(SpendControlTransactionConsumptionRequest request,
                                                       FundsTransactionDTO refundTransaction) {
        FundsTransactionDTO referencedTransaction = fundsTransactionQueryService.findFundsTransactionBySn(
                        request.getTenantId(), refundTransaction.getReferenceTransactionSn())
                .orElse(null);
        AssertUtils.notNull(referencedTransaction,
                "退款交易引用的原消费交易不存在，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(Objects.equals(referencedTransaction.getTenantId(), request.getTenantId()),
                "退款交易引用的原消费交易租户不一致，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(referencedTransaction.getTransactionType() != DefaultFundsTransactionType.REFUND,
                "退款交易不能引用另一笔退款交易，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(referencedTransaction.getState() == FundsTransactionState.CLOSED,
                "退款交易引用的原消费交易必须已关闭，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(Objects.equals(referencedTransaction.getBusinessScene(), request.getBusinessScene()),
                "退款交易引用的原消费交易业务场景不一致，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(Objects.equals(referencedTransaction.getBusinessSn(), request.getBusinessSn()),
                "退款交易引用的原消费交易业务流水不一致，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
        AssertUtils.isTrue(referencedTransaction.getCurrency() == request.getCurrency(),
                "退款交易引用的原消费交易币种不一致，transactionSn = {}, referenceTransactionSn = {}",
                request.getTransactionSn(),
                refundTransaction.getReferenceTransactionSn());
    }

    private void assertReferencedConsumedMovementsMatchOriginalMovement(
            SpendControlMovementDTO originalMovement,
            List<SpendControlMovementDTO> referencedConsumedMovements) {
        for (SpendControlMovementDTO consumedMovement : referencedConsumedMovements) {
            AssertUtils.isTrue(Objects.equals(consumedMovement.getBusinessScene(), originalMovement.getBusinessScene()),
                    "被引用已消费控制额度变动业务场景不一致，movementSn = {}, originalMovementSn = {}",
                    consumedMovement.getMovementSn(),
                    originalMovement.getMovementSn());
            AssertUtils.isTrue(Objects.equals(consumedMovement.getBusinessSn(), originalMovement.getBusinessSn()),
                    "被引用已消费控制额度变动业务流水不一致，movementSn = {}, originalMovementSn = {}",
                    consumedMovement.getMovementSn(),
                    originalMovement.getMovementSn());
            AssertUtils.isTrue(Objects.equals(consumedMovement.getTargetAccountId(),
                            originalMovement.getTargetAccountId()),
                    "被引用已消费控制额度变动目标账户不一致，movementSn = {}, originalMovementSn = {}",
                    consumedMovement.getMovementSn(),
                    originalMovement.getMovementSn());
            AssertUtils.isTrue(consumedMovement.getCurrency() == originalMovement.getCurrency(),
                    "被引用已消费控制额度变动币种不一致，movementSn = {}, originalMovementSn = {}",
                    consumedMovement.getMovementSn(),
                    originalMovement.getMovementSn());
        }
    }

    private void assertRefundDoesNotExceedReferencedConsumedAmount(
            SpendControlTransactionConsumptionRequest request,
            FundsTransactionDTO transaction,
            List<SpendControlMovementDTO> referencedConsumedMovements) {
        long referencedConsumedAmount = referencedConsumedMovements.stream()
                .filter(activity -> !Objects.equals(activity.getMovementSn(), request.getMovementSn()))
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
        long referencedRefundCompensatedAmount = sumRefundCompensatedAmountForReference(request,
                transaction.getReferenceTransactionSn());
        long referencedNetConsumedAmount = referencedConsumedAmount - referencedRefundCompensatedAmount;
        AssertUtils.isTrue(referencedNetConsumedAmount >= request.getAmount(),
                "退款控制补偿金额超过被引用已消费控制金额，movementSn = {}, referenceTransactionSn = {}, "
                        + "referencedNetConsumedAmount = {}, amount = {}",
                request.getMovementSn(),
                transaction.getReferenceTransactionSn(),
                referencedNetConsumedAmount,
                request.getAmount());
    }

    private long sumRefundCompensatedAmountForReference(SpendControlTransactionConsumptionRequest request,
                                                       String referenceTransactionSn) {
        List<SpendControlMovementDTO> refundCompensatedMovements =
                spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn())
                        .setMovementType(SpendControlMovementType.REFUND_COMPENSATED));
        long refundCompensatedAmount = 0L;
        for (SpendControlMovementDTO activity : refundCompensatedMovements) {
            if (Objects.equals(activity.getMovementSn(), request.getMovementSn())) {
                continue;
            }
            if (refundMovementReferencesConsumedTransaction(request, activity, referenceTransactionSn)) {
                refundCompensatedAmount += activity.getAmount();
            }
        }
        return refundCompensatedAmount;
    }

    private boolean refundMovementReferencesConsumedTransaction(SpendControlTransactionConsumptionRequest request,
                                                               SpendControlMovementDTO activity,
                                                               String referenceTransactionSn) {
        AssertUtils.hasText(activity.getTransactionSn(),
                "退款控制补偿活动缺少资金交易流水，movementSn = {}", activity.getMovementSn());
        FundsTransactionDTO refundTransaction = fundsTransactionQueryService.findFundsTransactionBySn(
                        request.getTenantId(), activity.getTransactionSn())
                .orElse(null);
        AssertUtils.notNull(refundTransaction,
                "退款控制补偿活动缺少资金交易事实，movementSn = {}, transactionSn = {}",
                activity.getMovementSn(),
                activity.getTransactionSn());
        AssertUtils.isTrue(Objects.equals(refundTransaction.getTenantId(), request.getTenantId()),
                "退款控制补偿活动资金交易租户不一致，movementSn = {}, transactionSn = {}",
                activity.getMovementSn(),
                activity.getTransactionSn());
        AssertUtils.isTrue(refundTransaction.getTransactionType() == DefaultFundsTransactionType.REFUND,
                "退款控制补偿活动必须关联退款交易事实，movementSn = {}, transactionSn = {}",
                activity.getMovementSn(),
                activity.getTransactionSn());
        return Objects.equals(refundTransaction.getReferenceTransactionSn(), referenceTransactionSn);
    }

    private ControlMovementUsage controlMovementUsage(SpendControlTransactionConsumptionRequest request,
                                                      SpendControlMovementDTO originalMovement) {
        List<SpendControlMovementDTO> linkedMovements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(request.getTenantId())
                        .setOriginalMovementSn(request.getOriginalMovementSn()));
        List<SpendControlMovementDTO> effectiveMovements = linkedMovements.stream()
                .filter(activity -> !Objects.equals(activity.getMovementSn(), request.getMovementSn()))
                .toList();
        effectiveMovements.forEach(activity -> assertLinkedMovementMatchesOriginalMovement(activity, originalMovement));
        long grossConsumedAmount = sumAmount(effectiveMovements, SpendControlMovementType.CONSUMED);
        long refundCompensatedAmount = sumAmount(effectiveMovements, SpendControlMovementType.REFUND_COMPENSATED);
        long releasedAmount = effectiveMovements.stream()
                .filter(activity -> activity.getMovementType().isReleaseMovement())
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
        return new ControlMovementUsage(originalMovement.getAmount(),
                grossConsumedAmount,
                refundCompensatedAmount,
                releasedAmount);
    }

    private void assertLinkedMovementMatchesOriginalMovement(SpendControlMovementDTO linkedMovement,
                                                             SpendControlMovementDTO originalMovement) {
        AssertUtils.isTrue(Objects.equals(linkedMovement.getBusinessScene(), originalMovement.getBusinessScene()),
                "关联控制额度变动业务场景不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getBusinessSn(), originalMovement.getBusinessSn()),
                "关联控制额度变动业务流水不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getTargetAccountId(), originalMovement.getTargetAccountId()),
                "关联控制额度变动目标账户不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(linkedMovement.getCurrency() == originalMovement.getCurrency(),
                "关联控制额度变动币种不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getSpendRuleId(), originalMovement.getSpendRuleId()),
                "关联控制额度变动 Spend Rule 标识不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getSpendRuleVersion(),
                        originalMovement.getSpendRuleVersion()),
                "关联控制额度变动 Spend Rule 版本不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getControlScopeId(), originalMovement.getControlScopeId()),
                "关联控制额度变动控制范围不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
        AssertUtils.isTrue(Objects.equals(linkedMovement.getPeriodId(), originalMovement.getPeriodId()),
                "关联控制额度变动周期不一致，movementSn = {}, originalMovementSn = {}",
                linkedMovement.getMovementSn(),
                originalMovement.getMovementSn());
    }

    private long sumAmount(List<SpendControlMovementDTO> movements, SpendControlMovementType movementType) {
        return movements.stream()
                .filter(activity -> activity.getMovementType() == movementType)
                .mapToLong(SpendControlMovementDTO::getAmount)
                .sum();
    }

    private RecordSpendControlMovementRequest toRecordRequest(SpendControlTransactionConsumptionRequest request,
                                                              SpendControlMovementDTO originalMovement,
                                                              SpendControlMovementType movementType) {
        return new RecordSpendControlMovementRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn())
                .setMovementType(movementType)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setOriginalMovementSn(request.getOriginalMovementSn())
                .setTransactionSn(request.getTransactionSn())
                .setInstrumentSn(originalMovement.getInstrumentSn())
                .setAction(originalMovement.getAction())
                .setTargetAccountId(request.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(originalMovement.getSpendRuleId())
                .setSpendRuleVersion(originalMovement.getSpendRuleVersion())
                .setSpendDecisionSn(originalMovement.getSpendDecisionSn())
                .setSpendDecisionResult(originalMovement.getSpendDecisionResult())
                .setSpendDecisionDigest(originalMovement.getSpendDecisionDigest())
                .setControlScopeId(originalMovement.getControlScopeId())
                .setPeriodId(originalMovement.getPeriodId())
                .setReasonCode(request.getReasonCode())
                .setOperatorId(request.getOperatorId())
                .setAuditReferenceSn(request.getAuditReferenceSn())
                .setMovementDigest(request.getMovementDigest())
                .setDescription(request.getDescription())
                .setContextVariables(request.getContextVariables());
    }

    private RecordSpendControlMovementRequest toRecordRequest(
            SpendControlBusinessConfirmedRefundCompensationRequest request) {
        String controlScopeId = controlScopeId(request);
        return new RecordSpendControlMovementRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn())
                .setMovementType(SpendControlMovementType.REFUND_COMPENSATED)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.REFUND)
                .setTargetAccountId(request.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setControlScopeId(controlScopeId)
                .setPeriodId(request.getPeriodId())
                .setReasonCode(request.getReasonCode())
                .setOperatorId(request.getOperatorId())
                .setAuditReferenceSn(request.getAuditReferenceSn())
                .setMovementDigest(businessConfirmedRefundMovementDigest(request, controlScopeId))
                .setDescription(request.getDescription())
                .setContextVariables(request.getContextVariables());
    }

    private String controlScopeId(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        return request.getControlScopeId();
    }

    /**
     * 摘要只覆盖业务确认后不可变的控制补偿事实。
     */
    private String businessConfirmedRefundMovementDigest(SpendControlBusinessConfirmedRefundCompensationRequest request,
                                                        String controlScopeId) {
        Map<String, Object> digestValues = new TreeMap<>();
        digestValues.put("amount", request.getAmount());
        digestValues.put("auditReferenceSn", request.getAuditReferenceSn());
        digestValues.put("businessScene", request.getBusinessScene());
        digestValues.put("businessSn", request.getBusinessSn());
        digestValues.put("controlScopeId", controlScopeId);
        digestValues.put("currency", request.getCurrency().name());
        digestValues.put("instrumentSn", request.getInstrumentSn());
        digestValues.put("periodId", request.getPeriodId());
        digestValues.put("reasonCode", request.getReasonCode());
        digestValues.put("spendRuleId", request.getSpendRuleId());
        digestValues.put("spendRuleVersion", request.getSpendRuleVersion());
        digestValues.put("targetAccountId", targetAccountDigest(request));
        digestValues.put("tenantId", request.getTenantId());
        return SHA256_PREFIX + FundsStableHashSupport.sha256CanonicalJson(
                REFUND_COMPENSATION_DIGEST_DOMAIN, digestValues);
    }

    private String targetAccountDigest(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        return request.getTargetAccountId().type() + ":" + request.getTargetAccountId().id();
    }

    private record ControlMovementUsage(long originalReservedAmount,
                                        long grossConsumedAmount,
                                        long refundCompensatedAmount,
                                        long releasedAmount) {

        private long netConsumedAmount() {
            return grossConsumedAmount - refundCompensatedAmount;
        }

        private long remainingAmount() {
            return originalReservedAmount - grossConsumedAmount - releasedAmount;
        }
    }
}
