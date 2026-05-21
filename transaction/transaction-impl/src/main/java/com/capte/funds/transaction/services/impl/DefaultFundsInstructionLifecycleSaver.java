package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.entities.table.FundsTransactionDetailNameRefs;
import com.capte.funds.transaction.dal.entities.table.FundsTransactionNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.mapstruct.FundsTransactionConverter;
import com.capte.funds.transaction.model.FundsTransactionParticipant;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.support.FundsRequestHashSupport;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 默认资金指令业务生命周期保存服务。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class DefaultFundsInstructionLifecycleSaver implements FundsInstructionLifecycleRecorder {

    private static final WindSequenceType FUNDS_TRANSACTION_SEQUENCE_TYPE = WindSequenceType.immutable(
            "FUNDS_TRANSACTION", "FT", 6);

    private static final WindSequenceType FUNDS_TRANSACTION_DETAIL_SEQUENCE_TYPE = WindSequenceType.immutable(
            "FUNDS_TRANSACTION_DETAIL", "FTD", 6);

    private static final int MAX_ERROR_MESSAGE_LENGTH = 512;

    private final FundsTransactionMapper fundsTransactionMapper;

    private final FundsTransactionDetailMapper fundsTransactionDetailMapper;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case FREEZE, UNFREEZE -> false;
            default -> true;
        };
    }

    @Override
    public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                  @NonNull ResolvedRouteSpec resolvedRoute,
                                                                  @NonNull RouteSnapshotSpec routeSnapshot) {
        FundsTransaction transaction = findOrCreateTransaction(instruction, routeSnapshot);
        List<FundsTransactionDetail> existingDetails = findExistingTransactionDetails(instruction, routeSnapshot,
                transaction.getSn());
        if (!existingDetails.isEmpty() && existingDetails.stream().allMatch(this::isCompletedDetail)) {
            return lifecycleResult(transaction.getSn(), existingDetails);
        }
        assertPostingSummaryAllowed(instruction, transaction);
        List<FundsTransactionDetail> details = findOrCreateTransactionDetails(instruction, routeSnapshot,
                transaction.getSn());
        return lifecycleResult(transaction.getSn(), details);
    }

    private FundsInstructionLifecycleResult lifecycleResult(String transactionSn, List<FundsTransactionDetail> details) {
        return new FundsInstructionLifecycleResult()
                .setTransactionSn(transactionSn)
                .setTransactionDetailSns(details.stream().map(FundsTransactionDetail::getSn).toList())
                .setLedgerTransactionSn(resolveLedgerTransactionSn(details))
                .setCompleted(details.stream().allMatch(this::isCompletedDetail));
    }

    @Override
    public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                              @NonNull FundsInstructionLifecycleResult result,
                              @Nullable String ledgerTransactionSn) {
        List<FundsTransactionDetail> details = findDetailsBySn(result.getTransactionDetailSns());
        if (details.stream().allMatch(this::isCompletedDetail)) {
            return;
        }
        FundsTransaction transaction = findTransactionBySn(result.getTransactionSn());
        assertSucceededSummaryAllowed(transaction, details);
        for (FundsTransactionDetail detail : details) {
            detail.setStatus(resolveCompletedDetailStatus(detail));
            detail.setLedgerTransactionSn(ledgerTransactionSn);
            detail.setErrorCode(null);
            detail.setErrorMessage(null);
            AssertUtils.isTrue(fundsTransactionDetailMapper.update(detail) == 1,
                    "更新资金交易明细状态失败，sn = {}", detail.getSn());
        }

        applySucceededSummary(transaction, details);
        AssertUtils.isTrue(fundsTransactionMapper.update(transaction) == 1,
                "更新资金交易聚合状态失败，sn = {}", transaction.getSn());
    }

    @Override
    public void markFailed(@NonNull FundsInstructionSpec instruction,
                           @NonNull FundsInstructionLifecycleResult result,
                           @NonNull Throwable cause) {
        List<FundsTransactionDetail> details = findDetailsBySn(result.getTransactionDetailSns());
        if (details.stream().allMatch(this::isCompletedDetail)) {
            return;
        }
        for (FundsTransactionDetail detail : details) {
            detail.setStatus(FundsTransactionDetailStatus.FAILED);
            detail.setErrorCode(cause.getClass().getSimpleName());
            detail.setErrorMessage(truncate(cause.getMessage()));
            AssertUtils.isTrue(fundsTransactionDetailMapper.update(detail) == 1,
                    "更新资金交易明细失败状态失败，sn = {}", detail.getSn());
        }

        FundsTransaction transaction = findTransactionBySn(result.getTransactionSn());
        if (!isStableTransactionStatus(transaction.getStatus())) {
            transaction.setStatus(FundsTransactionStatus.FAILED);
        }
        AssertUtils.isTrue(fundsTransactionMapper.update(transaction) == 1,
                "更新资金交易失败状态失败，sn = {}", transaction.getSn());
    }

    private FundsTransaction findOrCreateTransaction(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
        FundsTransaction result = findReferenceTransaction(instruction);
        if (result != null) {
            return result;
        }
        String mainBusinessSn = resolveMainBusinessSn(instruction);
        result = findTransactionByBusiness(instruction, mainBusinessSn);
        if (result != null) {
            return result;
        }
        return createTransaction(instruction, routeSnapshot, mainBusinessSn);
    }

    private FundsTransaction findReferenceTransaction(FundsInstructionSpec instruction) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        if (reference == null
                || !StringUtils.hasText(reference.getReferenceSn())
                || !isFundsTransactionReference(reference.getReferenceType())) {
            return null;
        }
        return findTransactionBySnNullable(reference.getReferenceSn());
    }

    private boolean isFundsTransactionReference(FundsInstructionReferenceType referenceType) {
        return switch (referenceType) {
            case ORIGINAL_TRANSACTION, AUTHORIZATION, REFUND, FEE -> true;
            case FREEZE_ORDER, EXTERNAL_TRANSACTION -> false;
        };
    }

    private FundsTransaction createTransaction(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot, String businessSn) {
        FundsTransaction entity = FundsTransactionConverter.INSTANCE.convertToFundsTransaction(instruction);
        entity.setSn(TemporalSequenceFactory.hourNext(FUNDS_TRANSACTION_SEQUENCE_TYPE));
        entity.setTransactionMode(resolveTransactionMode(instruction.getInstructionType()));
        entity.setBusinessSn(businessSn);
        entity.setReferenceTransactionSn(resolveReferenceSn(instruction.getReference()));
        entity.setRouteSnapshot(RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot));
        fundsTransactionMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金交易聚合记录失败");
        return entity;
    }

    private List<FundsTransactionDetail> findExistingTransactionDetails(FundsInstructionSpec instruction,
                                                                        RouteSnapshotSpec routeSnapshot,
                                                                        String transactionSn) {
        List<RouteParticipantSpec> participants = routeSnapshot.getParticipants();
        AssertUtils.isFalse(participants.isEmpty(),
                "RouteSnapshot participants 不能为空");
        List<FundsTransactionDetail> businessDetails = findDetailsByBusinessEvent(instruction, transactionSn);
        if (!businessDetails.isEmpty()) {
            AssertUtils.isTrue(businessDetails.size() == participants.size(),
                    "资金交易明细请求参数不一致，transactionSn = {}，businessSn = {}",
                    transactionSn, instruction.getBusinessSn());
        }
        List<FundsTransactionDetail> result = new ArrayList<>(participants.size());
        for (RouteParticipantSpec participant : participants) {
            String requestHash = computeDetailRequestHash(instruction, routeSnapshot, participant);
            FundsTransactionDetail detail = findDetailByBusinessEventAndParticipant(instruction, transactionSn,
                    participant);
            if (detail == null) {
                AssertUtils.isTrue(businessDetails.isEmpty(),
                        "资金交易明细请求参数不一致，transactionSn = {}，businessSn = {}",
                        transactionSn, instruction.getBusinessSn());
                return List.of();
            }
            AssertUtils.isTrue(Objects.equals(detail.getRequestHash(), requestHash),
                    "资金交易明细请求参数不一致，sn = {}", detail.getSn());
            result.add(detail);
        }
        return result;
    }

    private List<FundsTransactionDetail> findOrCreateTransactionDetails(FundsInstructionSpec instruction,
                                                                        RouteSnapshotSpec routeSnapshot,
                                                                        String transactionSn) {
        List<RouteParticipantSpec> participants = routeSnapshot.getParticipants();
        AssertUtils.isFalse(participants.isEmpty(),
                "RouteSnapshot participants 不能为空");
        List<FundsTransactionDetail> businessDetails = findDetailsByBusinessEvent(instruction, transactionSn);
        if (!businessDetails.isEmpty()) {
            AssertUtils.isTrue(businessDetails.size() == participants.size(),
                    "资金交易明细请求参数不一致，transactionSn = {}，businessSn = {}",
                    transactionSn, instruction.getBusinessSn());
        }
        List<FundsTransactionDetail> result = new ArrayList<>(participants.size());
        for (RouteParticipantSpec participant : participants) {
            String requestHash = computeDetailRequestHash(instruction, routeSnapshot, participant);
            FundsTransactionDetail detail = findDetailByBusinessEventAndParticipant(instruction, transactionSn,
                    participant);
            if (detail == null) {
                AssertUtils.isTrue(businessDetails.isEmpty(),
                        "资金交易明细请求参数不一致，transactionSn = {}，businessSn = {}",
                        transactionSn, instruction.getBusinessSn());
                detail = createTransactionDetail(instruction, routeSnapshot, transactionSn, participant, requestHash);
            } else {
                AssertUtils.isTrue(Objects.equals(detail.getRequestHash(), requestHash),
                        "资金交易明细请求参数不一致，sn = {}", detail.getSn());
            }
            result.add(detail);
        }
        return result;
    }

    private FundsTransactionDetail createTransactionDetail(FundsInstructionSpec instruction,
                                                           RouteSnapshotSpec routeSnapshot,
                                                           String transactionSn,
                                                           RouteParticipantSpec participant,
                                                           String requestHash) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        FundsTransactionParticipant transactionParticipant = toTransactionParticipant(instruction, routeSnapshot,
                participant);
        FundsTransactionDetail entity = FundsTransactionConverter.INSTANCE.convertToFundsTransactionDetail(instruction);
        entity.setSn(TemporalSequenceFactory.hourNext(FUNDS_TRANSACTION_DETAIL_SEQUENCE_TYPE));
        entity.setTransactionSn(transactionSn);
        entity.setSubjectId(transactionParticipant.getSubjectId());
        entity.setSubjectType(transactionParticipant.getSubjectType());
        entity.setParticipantRole(transactionParticipant.getParticipantRole());
        entity.setRequestHash(requestHash);
        entity.setFundsEffectType(transactionParticipant.getFundsEffectType());
        entity.setReferenceDetailSn(resolveReferenceSn(reference));
        entity.setReferenceLedgerTransactionSn(resolveReferenceLedgerTransactionSn(reference));
        entity.setAmount(transactionParticipant.getAmount());
        entity.setCurrency(transactionParticipant.getCurrency());
        entity.setStatus(FundsTransactionDetailStatus.PROCESSING);
        entity.setDescription(transactionParticipant.getDescription());
        entity.setContextVariables(toJson(transactionParticipant.getContextVariables()));
        fundsTransactionDetailMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金交易生命周期明细失败");
        return entity;
    }

    private FundsTransaction findTransactionByBusiness(FundsInstructionSpec instruction, String businessSn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.businessScene.eq(instruction.getBusinessScene()))
                .and(ref.businessSn.eq(businessSn));
        return fundsTransactionMapper.selectOneByQuery(wrapper);
    }

    private FundsTransactionDetail findDetailByBusinessEventAndParticipant(FundsInstructionSpec instruction,
                                                                           String transactionSn,
                                                                           RouteParticipantSpec participant) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.transactionSn.eq(transactionSn))
                .and(ref.businessScene.eq(instruction.getBusinessScene()))
                .and(ref.businessSn.eq(instruction.getBusinessSn()))
                .and(ref.transactionType.eq(instruction.getTransactionType()))
                .and(ref.eventType.eq(instruction.getEventType()))
                .and(ref.subjectId.eq(participant.getSubjectRef().getSubjectId()))
                .and(ref.subjectType.eq(participant.getSubjectRef().getSubjectType().name()))
                .and(ref.participantRole.eq(RouteParticipantRole.valueOf(participant.getParticipantRole().name())))
                .and(ref.fundsEffectType.eq(resolveFundsEffectType(instruction)));
        return fundsTransactionDetailMapper.selectOneByQuery(wrapper);
    }

    private List<FundsTransactionDetail> findDetailsByBusinessEvent(FundsInstructionSpec instruction,
                                                                    String transactionSn) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.transactionSn.eq(transactionSn))
                .and(ref.businessScene.eq(instruction.getBusinessScene()))
                .and(ref.businessSn.eq(instruction.getBusinessSn()))
                .and(ref.transactionType.eq(instruction.getTransactionType()))
                .and(ref.eventType.eq(instruction.getEventType()))
                .and(ref.fundsEffectType.eq(resolveFundsEffectType(instruction)));
        return fundsTransactionDetailMapper.selectListByQuery(wrapper);
    }

    private FundsTransaction findTransactionBySn(String sn) {
        FundsTransaction result = findTransactionBySnNullable(sn);
        AssertUtils.notNull(result, "资金交易聚合记录不存在，sn = {}", sn);
        return result;
    }

    private FundsTransaction findTransactionBySnNullable(String sn) {
        FundsTransactionNameRefs ref = FundsTransactionNameRefs.fundsTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        return fundsTransactionMapper.selectOneByQuery(wrapper);
    }

    private FundsTransactionDetail findDetailBySn(String sn) {
        FundsTransactionDetailNameRefs ref = FundsTransactionDetailNameRefs.fundsTransactionDetail;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        FundsTransactionDetail result = fundsTransactionDetailMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "资金交易生命周期明细不存在，sn = {}", sn);
        return result;
    }

    private List<FundsTransactionDetail> findDetailsBySn(List<String> sns) {
        AssertUtils.isFalse(sns == null || sns.isEmpty(), "资金交易明细流水不能为空");
        List<FundsTransactionDetail> result = new ArrayList<>(sns.size());
        for (String sn : sns) {
            result.add(findDetailBySn(sn));
        }
        return result;
    }

    private void applySucceededSummary(FundsTransaction transaction, List<FundsTransactionDetail> details) {
        FundsTransactionDetail primaryDetail = details.getFirst();
        long amount = resolvePrimaryAmount(details);
        switch (primaryDetail.getEventType()) {
            case AUTHORIZE -> {
                if (primaryDetail.getStatus() == FundsTransactionDetailStatus.REJECTED) {
                    transaction.setStatus(FundsTransactionStatus.REJECTED);
                    return;
                }
                transaction.setAuthorizedAmount(transaction.getAuthorizedAmount() + amount);
                transaction.setStatus(FundsTransactionStatus.OPEN);
            }
            case REVERSAL -> applyReversedSummary(transaction, amount);
            case SETTLE -> applySettledSummary(transaction, amount);
            case TOPUP, TRANSFER, PAY, WITHDRAW, FEE_CHARGE -> applyPostedSummary(transaction, primaryDetail, details);
            case AUTH_REFUND, REFUND -> applyRefundedSummary(transaction, amount);
            case FEE_REFUND -> applyFeeRefundedSummary(transaction, amount);
            case CHARGEBACK -> applyChargebackSummary(transaction, amount);
            case FREEZE, UNFREEZE, BALANCE_ADJUST, LIMIT_ADJUST ->
                    transaction.setStatus(resolveBalanceControlStatus(primaryDetail));
        }
    }

    private void assertPostingSummaryAllowed(FundsInstructionSpec instruction, FundsTransaction transaction) {
        FundsTransactionEventType eventType = instruction.getEventType();
        long amount = instruction.getAmount().getAmount();
        switch (eventType) {
            case AUTH_REFUND, REFUND -> {
                if (shouldAssertSettledReversibleAmount(transaction)) {
                    assertSettledReversibleAmountSufficient(transaction, amount);
                }
            }
            case CHARGEBACK -> assertSettledReversibleAmountSufficient(transaction, amount);
            default -> {
            }
        }
    }

    private void assertSucceededSummaryAllowed(FundsTransaction transaction, List<FundsTransactionDetail> details) {
        FundsTransactionDetail primaryDetail = details.getFirst();
        long amount = resolvePrimaryAmount(details);
        switch (primaryDetail.getEventType()) {
            case AUTH_REFUND, REFUND -> {
                if (shouldAssertSettledReversibleAmount(transaction)) {
                    assertSettledReversibleAmountSufficient(transaction, amount);
                }
            }
            case CHARGEBACK -> assertSettledReversibleAmountSufficient(transaction, amount);
            default -> {
            }
        }
    }

    private long resolvePrimaryAmount(List<FundsTransactionDetail> details) {
        return details.stream()
                .filter(detail -> detail.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER)
                .map(FundsTransactionDetail::getAmount)
                .findFirst()
                .orElseGet(() -> details.getFirst().getAmount());
    }

    private void applyReversedSummary(FundsTransaction transaction, long amount) {
        transaction.setReversedAmount(transaction.getReversedAmount() + amount);
        transaction.setStatus(isAuthorizationClosed(transaction) ? FundsTransactionStatus.CLOSED
                : FundsTransactionStatus.OPEN);
    }

    private void applySettledSummary(FundsTransaction transaction, long amount) {
        transaction.setSettledAmount(transaction.getSettledAmount() + amount);
        transaction.setStatus(isAuthorizationClosed(transaction) ? FundsTransactionStatus.CLOSED
                : FundsTransactionStatus.OPEN);
    }

    private void applyPostedSummary(FundsTransaction transaction,
                                    FundsTransactionDetail detail,
                                    List<FundsTransactionDetail> details) {
        long amount = resolvePrimaryAmount(details);
        if (detail.getTransactionType() == DefaultFundsTransactionType.FEE) {
            transaction.setFeeAmount(transaction.getFeeAmount() + amount);
        } else {
            transaction.setSettledAmount(transaction.getSettledAmount() + amount);
            transaction.setFeeAmount(transaction.getFeeAmount() + resolveFeeAmount(details));
        }
        transaction.setStatus(FundsTransactionStatus.CLOSED);
    }

    private long resolveFeeAmount(List<FundsTransactionDetail> details) {
        return details.stream()
                .filter(detail -> detail.getParticipantRole() == RouteParticipantRole.FEE_RECEIVER)
                .mapToLong(FundsTransactionDetail::getAmount)
                .sum();
    }

    private void applyRefundedSummary(FundsTransaction transaction, long amount) {
        transaction.setRefundedAmount(transaction.getRefundedAmount() + amount);
        transaction.setStatus(resolveSettledReversibleStatus(transaction));
    }

    private void applyFeeRefundedSummary(FundsTransaction transaction, long amount) {
        transaction.setRefundedAmount(transaction.getRefundedAmount() + amount);
        transaction.setStatus(transaction.getSettledAmount() > 0
                && transaction.getRefundedAmount() >= transaction.getSettledAmount()
                ? FundsTransactionStatus.CLOSED : FundsTransactionStatus.OPEN);
    }

    private void applyChargebackSummary(FundsTransaction transaction, long amount) {
        transaction.setDeclinedAmount(transaction.getDeclinedAmount() + amount);
        transaction.setStatus(resolveSettledReversibleStatus(transaction));
    }

    private void assertSettledReversibleAmountSufficient(FundsTransaction transaction, long amount) {
        long remainingAmount = transaction.getSettledAmount() - transaction.getRefundedAmount()
                - transaction.getDeclinedAmount();
        AssertUtils.isTrue(amount <= remainingAmount,
                "资金交易已结算可回退金额不足，sn = {}，remainingAmount = {}，amount = {}",
                transaction.getSn(), remainingAmount, amount);
    }

    private boolean shouldAssertSettledReversibleAmount(FundsTransaction transaction) {
        return transaction.getTransactionType() != DefaultFundsTransactionType.REFUND;
    }

    private FundsTransactionStatus resolveSettledReversibleStatus(FundsTransaction transaction) {
        return transaction.getSettledAmount() > 0
                && transaction.getRefundedAmount() + transaction.getDeclinedAmount() >= transaction.getSettledAmount()
                ? FundsTransactionStatus.CLOSED : FundsTransactionStatus.OPEN;
    }

    private boolean isAuthorizationClosed(FundsTransaction transaction) {
        if (transaction.getAuthorizedAmount() <= 0) {
            return false;
        }
        return transaction.getReversedAmount() + transaction.getSettledAmount() >= transaction.getAuthorizedAmount();
    }

    private FundsTransactionStatus resolveBalanceControlStatus(FundsTransactionDetail detail) {
        return switch (detail.getEventType()) {
            case FREEZE -> FundsTransactionStatus.OPEN;
            case UNFREEZE, BALANCE_ADJUST, LIMIT_ADJUST ->
                    FundsTransactionStatus.CLOSED;
            default -> FundsTransactionStatus.CLOSED;
        };
    }

    private boolean isCompletedDetail(FundsTransactionDetail detail) {
        return detail.getStatus() == FundsTransactionDetailStatus.SUCCEEDED
                || detail.getStatus() == FundsTransactionDetailStatus.REJECTED;
    }

    private FundsTransactionDetailStatus resolveCompletedDetailStatus(FundsTransactionDetail detail) {
        return switch (detail.getEventType()) {
            case AUTHORIZE -> Boolean.FALSE.equals(resolveApproved(detail)) ? FundsTransactionDetailStatus.REJECTED
                    : FundsTransactionDetailStatus.SUCCEEDED;
            default -> FundsTransactionDetailStatus.SUCCEEDED;
        };
    }

    private Boolean resolveApproved(FundsTransactionDetail detail) {
        if (!StringUtils.hasText(detail.getContextVariables())) {
            return null;
        }
        JSONObject values = JSON.parseObject(detail.getContextVariables());
        return values.getBoolean(FundsInstructionContextKeys.APPROVED);
    }

    private boolean isStableTransactionStatus(FundsTransactionStatus status) {
        return status == FundsTransactionStatus.OPEN
                || status == FundsTransactionStatus.CLOSED
                || status == FundsTransactionStatus.REJECTED;
    }

    private FundsTransactionMode resolveTransactionMode(FundsInstructionType instructionType) {
        return switch (instructionType) {
            case DIRECT_TRANSACTION -> FundsTransactionMode.DIRECT;
            case AUTHORIZATION_TRANSACTION -> FundsTransactionMode.AUTHORIZATION;
            case BALANCE_CONTROL -> FundsTransactionMode.BALANCE_CONTROL;
        };
    }

    private FundsEffectType resolveFundsEffectType(FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case AUTHORIZE, FREEZE -> FundsEffectType.HOLD;
            case REVERSAL, UNFREEZE -> FundsEffectType.RELEASE;
            case SETTLE, WITHDRAW -> FundsEffectType.CONSUME;
            case AUTH_REFUND, REFUND, CHARGEBACK, FEE_REFUND -> FundsEffectType.RETURN;
            case BALANCE_ADJUST, LIMIT_ADJUST -> FundsEffectType.ADJUST;
            case TOPUP, TRANSFER, PAY, FEE_CHARGE -> resolvePostedFundsEffectType(instruction);
        };
    }

    private FundsEffectType resolvePostedFundsEffectType(FundsInstructionSpec instruction) {
        return switch (instruction.getTransactionType()) {
            case TOPUP, TRANSFER, PAY, FEE -> FundsEffectType.DIRECT;
            case WITHDRAW -> FundsEffectType.CONSUME;
            case REFUND -> FundsEffectType.RETURN;
            case ADJUSTMENT -> FundsEffectType.ADJUST;
        };
    }

    private String resolveMainBusinessSn(FundsInstructionSpec instruction) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        if (reference != null && StringUtils.hasText(reference.getReferenceBusinessSn())) {
            return reference.getReferenceBusinessSn();
        }
        return instruction.getBusinessSn();
    }

    private String resolveReferenceSn(FundsInstructionReferenceSpec reference) {
        return reference == null ? null : reference.getReferenceSn();
    }

    private String resolveReferenceLedgerTransactionSn(FundsInstructionReferenceSpec reference) {
        return reference == null ? null : reference.getReferenceLedgerTransactionSn();
    }

    private String computeDetailRequestHash(FundsInstructionSpec instruction,
                                            RouteSnapshotSpec routeSnapshot,
                                            RouteParticipantSpec participant) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", instruction.getTenantId());
        values.put("instructionType", instruction.getInstructionType().name());
        values.put("eventType", instruction.getEventType().name());
        values.put("transactionType", instruction.getTransactionType().name());
        values.put("amount", instruction.getAmount().getAmount());
        values.put("currency", instruction.getAmount().getCurrency().name());
        values.put("originalAmount", instruction.getOriginalAmount().getAmount());
        values.put("originalCurrency", instruction.getOriginalAmount().getCurrency().name());
        values.put("exchangeRate", instruction.getExchangeRate());
        values.put("businessScene", instruction.getBusinessScene());
        values.put("businessSn", instruction.getBusinessSn());
        values.put("reference", referenceSummary(instruction.getReference()));
        values.put("route", routeRequestHashSummary(routeSnapshot));
        values.put("participant", participantSummary(participant));
        return FundsRequestHashSupport.sha256Json(values);
    }

    private Map<String, Object> routeRequestHashSummary(RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = new TreeMap<>(RouteSnapshotJsonSupport.routeSummary(routeSnapshot));
        values.remove("snapshotId");
        values.remove("resolvedAt");
        values.remove("expiresAt");
        return FundsRequestHashSupport.stableHashMap(values);
    }

    private Map<String, Object> referenceSummary(FundsInstructionReferenceSpec reference) {
        Map<String, Object> values = new TreeMap<>();
        if (reference == null) {
            return values;
        }
        values.put("referenceType", reference.getReferenceType().name());
        values.put("referenceSn", reference.getReferenceSn());
        values.put("referenceBusinessSn", reference.getReferenceBusinessSn());
        values.put("referenceLedgerTransactionSn", reference.getReferenceLedgerTransactionSn());
        values.put("externalTransactionId", reference.getExternalTransactionId());
        values.put("authCode", reference.getAuthCode());
        values.put("contextVariables", FundsRequestHashSupport.stableHashMap(reference.getContextVariables()));
        return values;
    }

    private Map<String, Object> participantSummary(RouteParticipantSpec participant) {
        Map<String, Object> values = new TreeMap<>();
        values.put("participantRole", participant.getParticipantRole().name());
        values.put("subjectRef", subjectSummary(participant.getSubjectRef()));
        values.put("ledgerProfileCode", participant.getLedgerProfileCode());
        values.put("currency", participant.getCurrency());
        values.put("amount", moneySummary(participant.getAmount()));
        values.put("contextVariables", FundsRequestHashSupport.stableHashMap(participant.getContextVariables()));
        return values;
    }

    private List<String> replayConsumedLegIds(RouteSnapshotSpec routeSnapshot, RouteParticipantSpec participant) {
        String subjectKey = subjectKey(participant.getSubjectRef());
        return routeSnapshot.getLegs()
                .stream()
                .filter(leg -> StringUtils.hasText(leg.getReplayRefLegId()))
                .filter(leg -> subjectKey(leg.getSourceNode().getSubjectRef()).equals(subjectKey)
                        || subjectKey(leg.getTargetNode().getSubjectRef()).equals(subjectKey))
                .map(RouteLegSpec::getReplayRefLegId)
                .distinct()
                .toList();
    }

    private String resolveLedgerTransactionSn(List<FundsTransactionDetail> details) {
        return details.stream()
                .map(FundsTransactionDetail::getLedgerTransactionSn)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Money resolveParticipantAmount(FundsInstructionSpec instruction, RouteParticipantSpec participant) {
        return participant.getAmount() == null ? instruction.getAmount() : participant.getAmount();
    }

    private FundsTransactionParticipant toTransactionParticipant(FundsInstructionSpec instruction,
                                                                 RouteSnapshotSpec routeSnapshot,
                                                                 RouteParticipantSpec participant) {
        Money amount = resolveParticipantAmount(instruction, participant);
        return new FundsTransactionParticipant()
                .setSubjectId(participant.getSubjectRef().getSubjectId())
                .setSubjectType(participant.getSubjectRef().getSubjectType().name())
                .setParticipantRole(RouteParticipantRole.valueOf(participant.getParticipantRole().name()))
                .setAmount(amount.getAmount())
                .setCurrency(amount.getCurrency())
                .setFundsEffectType(resolveFundsEffectType(instruction))
                .setDescription(resolveParticipantDescription(instruction, participant))
                .setContextVariables(mergedContext(instruction, routeSnapshot, participant));
    }

    private String resolveParticipantDescription(FundsInstructionSpec instruction,
                                                 RouteParticipantSpec participant) {
        return StringUtils.hasText(participant.getDescription()) ? participant.getDescription()
                : instruction.getDescription();
    }

    private Map<String, Object> mergedContext(FundsInstructionSpec instruction, RouteParticipantSpec participant) {
        Map<String, Object> result = new LinkedHashMap<>(instruction.getContextVariables());
        result.putAll(participant.getContextVariables());
        return result;
    }

    private Map<String, Object> mergedContext(FundsInstructionSpec instruction,
                                              RouteSnapshotSpec routeSnapshot,
                                              RouteParticipantSpec participant) {
        Map<String, Object> result = mergedContext(instruction, participant);
        List<String> replayConsumedLegIds = replayConsumedLegIds(routeSnapshot, participant);
        if (!replayConsumedLegIds.isEmpty()) {
            result.put(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_IDS, replayConsumedLegIds);
            result.put(FundsInstructionContextKeys.REPLAY_CONSUMED_LEG_AMOUNTS,
                    replayConsumedLegAmounts(routeSnapshot, replayConsumedLegIds));
        }
        return result;
    }

    private Map<String, Long> replayConsumedLegAmounts(RouteSnapshotSpec routeSnapshot,
                                                       List<String> replayConsumedLegIds) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            String replayRefLegId = leg.getReplayRefLegId();
            if (!StringUtils.hasText(replayRefLegId)
                    || !replayConsumedLegIds.contains(replayRefLegId)) {
                continue;
            }
            result.put(replayRefLegId, leg.getAmount().getAmount());
        }
        return result;
    }

    private String subjectKey(SubjectRef subjectRef) {
        return subjectRef.getSubjectType().name()
                + ":"
                + subjectRef.getSubjectId();
    }

    private Map<String, Object> subjectSummary(SubjectRef subjectRef) {
        Map<String, Object> values = new TreeMap<>();
        if (subjectRef == null) {
            return values;
        }
        values.put("subjectId", subjectRef.getSubjectId());
        values.put("subjectType", subjectRef.getSubjectType().name());
        values.put("tenantId", subjectRef.getTenantId());
        values.put("currency", subjectRef.getCurrency());
        values.put("ledgerProfileCode", subjectRef.getLedgerProfileCode());
        return values;
    }

    private Map<String, Object> moneySummary(Money money) {
        Map<String, Object> values = new TreeMap<>();
        if (money == null) {
            return values;
        }
        values.put("amount", money.getAmount());
        values.put("currency", money.getCurrency().name());
        return values;
    }

    private String toJson(Object value) {
        return JSON.toJSONString(value);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

}
