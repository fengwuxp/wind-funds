package com.wind.funds.transaction.application.spend.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.application.spend.SpendControlTransactionConsumptionApplicationService;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.model.request.SpendControlBusinessConfirmedRefundCompensationRequest;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 交易结果消费控制额度变动流水应用服务实现。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Service
@AllArgsConstructor
public class SpendControlTransactionConsumptionApplicationServiceImpl
        implements SpendControlTransactionConsumptionApplicationService {

    private static final String SHA256_PREFIX = "sha256:";

    private final SpendControlMovementService spendControlMovementService;

    private final PaymentInstrumentService paymentInstrumentService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO consume(@NonNull SpendControlTransactionConsumptionRequest request) {
        validateTransactionControlRequest(request);
        SpendControlMovementDTO originalMovement = getOriginalReservedMovement(request);
        FundsTransactionDTO transaction = getExistingFundsTransaction(request);
        assertClosedTransaction(request, transaction);
        AssertUtils.isTrue(transaction.getTransactionType() != DefaultFundsTransactionType.REFUND,
                "控制消费不能使用退款交易事实，transactionSn = {}", request.getTransactionSn());
        assertControlMovementMatchesOriginalMovement(request, originalMovement, "控制消费");
        assertControlMovementMatchesTransaction(request, transaction, "控制消费");
        assertTransactionBusinessSnMatches(request, transaction);
        assertEnoughRemainingControlAmount(request, originalMovement, "控制消费金额超过原占用剩余额度");
        assertTransactionControlAmountNotExceeded(request, transaction, SpendControlMovementType.CONSUMED, "控制消费");
        return spendControlMovementService.recordMovement(
                toRecordRequest(request, originalMovement, SpendControlMovementType.CONSUMED));
    }

    @Override
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
        return spendControlMovementService.recordMovement(
                toRecordRequest(request, originalMovement, SpendControlMovementType.REFUND_COMPENSATED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlMovementDTO compensateBusinessConfirmedRefund(
            @NonNull SpendControlBusinessConfirmedRefundCompensationRequest request) {
        validateBusinessConfirmedRefundRequest(request);
        RecordSpendControlMovementRequest recordRequest = toRecordRequest(request);
        if (hasRecordedMovement(request)) {
            return spendControlMovementService.recordMovement(recordRequest);
        }
        assertPaymentInstrumentActive(request);
        BudgetControlProjectionDTO projection = getBudgetControlProjection(request);
        assertBusinessConfirmedRefundAmountAllowed(request, projection);
        return spendControlMovementService.recordMovement(recordRequest);
    }

    private void validateTransactionControlRequest(SpendControlTransactionConsumptionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
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
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
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

    private void assertPaymentInstrumentActive(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        List<PaymentInstrumentDTO> instruments = paymentInstrumentService.queryPaymentInstruments(
                new PaymentInstrumentQuery()
                        .setTenantId(request.getTenantId())
                        .setSn(request.getInstrumentSn()),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isFalse(instruments.isEmpty(), "支付工具不存在，instrumentSn = {}", request.getInstrumentSn());
        AssertUtils.isTrue(instruments.size() == 1, "支付工具不唯一，instrumentSn = {}", request.getInstrumentSn());
        PaymentInstrumentDTO instrument = instruments.getFirst();
        AssertUtils.isTrue(instrument.getStatus() == FundsAccountStatus.ACTIVE,
                "支付工具状态不可用，instrumentSn = {}", request.getInstrumentSn());
        AssertUtils.isTrue(instrument.getCurrency() == request.getCurrency(),
                "支付工具币种与请求币种不一致，instrumentSn = {}", request.getInstrumentSn());
        AssertUtils.isTrue(isCurrentEffective(instrument.getValidFrom(), instrument.getValidTo()),
                "支付工具不在当前有效期内，instrumentSn = {}", request.getInstrumentSn());
    }

    private boolean hasRecordedMovement(SpendControlBusinessConfirmedRefundCompensationRequest request) {
        return !spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(request.getTenantId())
                .setMovementSn(request.getMovementSn())).isEmpty();
    }

    private boolean isCurrentEffective(LocalDateTime validFrom, LocalDateTime validTo) {
        LocalDateTime now = LocalDateTime.now();
        return (validFrom == null || !validFrom.isAfter(now)) && (validTo == null || validTo.isAfter(now));
    }

    private BudgetControlProjectionDTO getBudgetControlProjection(
            SpendControlBusinessConfirmedRefundCompensationRequest request) {
        String controlScopeId = controlScopeId(request);
        return spendControlMovementService.getBudgetControlProjection(new BudgetControlProjectionQuery()
                .setTenantId(request.getTenantId())
                .setControlScopeId(controlScopeId)
                .setPeriodId(request.getPeriodId())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setTargetAccountId(request.getTargetAccountId()));
    }

    private void assertBusinessConfirmedRefundAmountAllowed(
            SpendControlBusinessConfirmedRefundCompensationRequest request,
            BudgetControlProjectionDTO projection) {
        AssertUtils.isTrue(projection.getConsumedAmount() >= request.getAmount(),
                "业务确认退款补偿金额超过当前周期净消费控制金额，movementSn = {}, consumedAmount = {}, amount = {}",
                request.getMovementSn(),
                projection.getConsumedAmount(),
                request.getAmount());
        long availableAfterCompensation = projection.getAvailableControlAmount() + request.getAmount();
        AssertUtils.isTrue(availableAfterCompensation <= projection.getLimitAmount(),
                "业务确认退款补偿后可用控制额度不能超过周期控制额度，movementSn = {}, limitAmount = {}, "
                        + "availableAfterCompensation = {}",
                request.getMovementSn(),
                projection.getLimitAmount(),
                availableAfterCompensation);
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
        FundsTransactionDTO referencedTransaction = fundsTransactionQueryService.queryFundsTransaction(
                        refundTransaction.getReferenceTransactionSn())
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
        AssertUtils.isTrue(referencedTransaction.getStatus() == FundsTransactionStatus.CLOSED,
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
        FundsTransactionDTO refundTransaction = fundsTransactionQueryService.queryFundsTransaction(
                        activity.getTransactionSn())
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
        return SHA256_PREFIX + FundsStableHashSupport.sha256Json(digestValues);
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
            return originalReservedAmount - netConsumedAmount() - releasedAmount;
        }
    }
}
