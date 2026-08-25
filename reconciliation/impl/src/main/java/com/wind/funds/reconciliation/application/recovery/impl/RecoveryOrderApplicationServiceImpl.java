package com.wind.funds.reconciliation.application.recovery.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.recovery.RecoveryOrderApplicationService;
import com.wind.funds.reconciliation.dal.entities.RecoveryOrder;
import com.wind.funds.reconciliation.dal.entities.RecoveryResult;
import com.wind.funds.reconciliation.dal.mapper.RecoveryOrderMapper;
import com.wind.funds.reconciliation.dal.mapper.RecoveryResultMapper;
import com.wind.funds.reconciliation.enums.RecoveryOrderState;
import com.wind.funds.reconciliation.model.dto.RecoveryOrderDTO;
import com.wind.funds.reconciliation.model.request.CreateRecoveryOrderRequest;
import com.wind.funds.reconciliation.model.request.RecordRecoveryResultRequest;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 追偿责任与已完成资金结果登记实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class RecoveryOrderApplicationServiceImpl implements RecoveryOrderApplicationService {

    private static final String RECOVERY_BUSINESS_SCENE = "RECOVERY";

    private static final WindSequenceType ORDER_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECOVERY_ORDER", "RCV", 6);

    private static final WindSequenceType RESULT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECOVERY_RESULT", "RCR", 6);

    private final RecoveryOrderMapper recoveryOrderMapper;

    private final RecoveryResultMapper recoveryResultMapper;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecoveryOrderDTO createOrder(CreateRecoveryOrderRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        String digest = createDigest(request);
        RecoveryOrder existing = selectBySource(request);
        if (existing != null) {
            RecoveryOrderDTO result = sameSource(existing, digest);
            log.info("追偿单创建幂等复用，tenantId={}, sourceType={}, sourceSn={}, recoveryOrderSn={}, state={}",
                    request.getTenantId(), request.getSourceType(), request.getSourceSn(), result.getSn(),
                    result.getState());
            return result;
        }

        RecoveryOrder order = newOrder(request, digest, operator);
        try {
            recoveryOrderMapper.insertSelective(order);
        } catch (DuplicateKeyException exception) {
            existing = selectBySourceForUpdate(request);
            AssertUtils.notNull(existing, "追偿来源唯一键冲突但未找到已有追偿单");
            RecoveryOrderDTO result = sameSource(existing, digest);
            log.info("追偿单并发幂等复用，tenantId={}, sourceType={}, sourceSn={}, recoveryOrderSn={}, state={}",
                    request.getTenantId(), request.getSourceType(), request.getSourceSn(), result.getSn(),
                    result.getState());
            return result;
        }
        AssertUtils.notNull(order.getId(), "创建追偿单失败");
        RecoveryOrderDTO result = toDTO(order);
        log.info("追偿单创建完成，等待事务提交，tenantId={}, sourceType={}, sourceSn={}, recoveryOrderSn={}, "
                        + "expectedAmount={}, currency={}, state={}",
                request.getTenantId(), request.getSourceType(), request.getSourceSn(), result.getSn(),
                result.getExpectedAmount(), result.getCurrency(), result.getState());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecoveryOrderDTO recordResult(RecordRecoveryResultRequest request, WindOperator operator) {
        validateRecordRequest(request, operator);
        RecoveryOrder order = requiredOrderForUpdate(request.getTenantId(), request.getRecoveryOrderSn());
        String requestDigest = resultRequestDigest(request);
        RecoveryResult replay = recoveryResultMapper.selectByIdempotencyKey(
                request.getTenantId(), request.getIdempotencyKey());
        if (replay != null) {
            RecoveryOrderDTO result = sameResult(order, replay, request, requestDigest);
            log.info("追偿结果幂等复用，tenantId={}, recoveryOrderSn={}, fundsTransactionSn={}, state={}",
                    request.getTenantId(), request.getRecoveryOrderSn(), request.getFundsTransactionSn(),
                    result.getState());
            return result;
        }
        RecoveryResult claimed = recoveryResultMapper.selectByFundsTransactionSn(
                request.getTenantId(), request.getFundsTransactionSn());
        AssertUtils.isTrue(claimed == null,
                "资金交易已被其他追偿单认领，fundsTransactionSn = {}", request.getFundsTransactionSn());

        FundsTransactionDTO transaction = requiredRecoveryTransaction(order, request.getFundsTransactionSn());
        long remainingAmount = order.getExpectedAmount() - order.getRecoveredAmount();
        AssertUtils.isTrue(transaction.getAmount() <= remainingAmount,
                "追偿结果金额超过剩余应追金额，remainingAmount = {}", remainingAmount);
        RecoveryResult result = newResult(order, transaction, request, requestDigest, operator);
        try {
            recoveryResultMapper.insertSelective(result);
        } catch (DuplicateKeyException exception) {
            RecoveryResult concurrentReplay = recoveryResultMapper.selectByIdempotencyKeyForUpdate(
                    request.getTenantId(), request.getIdempotencyKey());
            if (concurrentReplay != null) {
                RecoveryOrderDTO concurrentResult = sameResult(order, concurrentReplay, request, requestDigest);
                log.info("追偿结果并发幂等复用，tenantId={}, recoveryOrderSn={}, fundsTransactionSn={}, state={}",
                        request.getTenantId(), request.getRecoveryOrderSn(), request.getFundsTransactionSn(),
                        concurrentResult.getState());
                return concurrentResult;
            }
            RecoveryResult concurrentClaim = recoveryResultMapper.selectByFundsTransactionSnForUpdate(
                    request.getTenantId(), request.getFundsTransactionSn());
            AssertUtils.notNull(concurrentClaim, "追偿结果唯一键冲突但未找到已有结果");
            throw new IllegalArgumentException("资金交易已被其他追偿单认领，fundsTransactionSn = "
                    + request.getFundsTransactionSn());
        }

        order.setRecoveredAmount(order.getRecoveredAmount() + transaction.getAmount());
        order.setLastFundsTransactionSn(transaction.getSn());
        if (Objects.equals(order.getRecoveredAmount(), order.getExpectedAmount())) {
            order.setState(RecoveryOrderState.RECOVERED);
            order.setRecoveredTime(LocalDateTime.now());
        } else {
            order.setState(RecoveryOrderState.PARTIALLY_RECOVERED);
        }
        AssertUtils.isTrue(recoveryOrderMapper.update(order) == 1, "更新追偿单累计结果失败");
        RecoveryOrderDTO resultDto = toDTO(order);
        log.info("追偿结果记录完成，等待事务提交，tenantId={}, recoveryOrderSn={}, fundsTransactionSn={}, "
                        + "recoveredAmount={}, expectedAmount={}, currency={}, state={}",
                request.getTenantId(), request.getRecoveryOrderSn(), request.getFundsTransactionSn(),
                resultDto.getRecoveredAmount(), resultDto.getExpectedAmount(), resultDto.getCurrency(),
                resultDto.getState());
        return resultDto;
    }

    @Override
    @Transactional(readOnly = true)
    public RecoveryOrderDTO getOrder(Long tenantId, String recoveryOrderSn) {
        validateQuery(tenantId, recoveryOrderSn);
        return toDTO(requiredOrder(tenantId, recoveryOrderSn));
    }

    private RecoveryOrder newOrder(CreateRecoveryOrderRequest request,
                                   String digest,
                                   WindOperator operator) {
        RecoveryOrder result = new RecoveryOrder();
        result.setSn(TemporalSequenceFactory.hourNext(ORDER_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setSourceType(request.getSourceType());
        result.setSourceSn(request.getSourceSn());
        result.setResponsibleSubjectType(request.getResponsibleSubjectType());
        result.setResponsibleSubjectId(request.getResponsibleSubjectId());
        result.setExpectedAmount(request.getExpectedAmount());
        result.setRecoveredAmount(0L);
        result.setCurrency(request.getCurrency());
        result.setState(RecoveryOrderState.CREATED);
        result.setSourceDigest(request.getSourceDigest());
        result.setOrderDigest(digest);
        result.setApprovalRef(request.getApprovalRef());
        result.setEvidenceRef(request.getEvidenceRef());
        result.setCreatedBy(operator.getOperatorAsText());
        result.setVersion(0);
        return result;
    }

    private RecoveryResult newResult(RecoveryOrder order,
                                     FundsTransactionDTO transaction,
                                     RecordRecoveryResultRequest request,
                                     String requestDigest,
                                     WindOperator operator) {
        RecoveryResult result = new RecoveryResult();
        result.setSn(TemporalSequenceFactory.hourNext(RESULT_SEQUENCE_TYPE));
        result.setTenantId(order.getTenantId());
        result.setRecoveryOrderSn(order.getSn());
        result.setFundsTransactionSn(transaction.getSn());
        result.setAmount(transaction.getAmount());
        result.setCurrency(transaction.getCurrency());
        result.setIdempotencyKey(request.getIdempotencyKey());
        result.setResultDigest(requestDigest);
        result.setApprovalRef(request.getApprovalRef());
        result.setEvidenceRef(request.getEvidenceRef());
        result.setRecordedBy(operator.getOperatorAsText());
        return result;
    }

    private FundsTransactionDTO requiredRecoveryTransaction(RecoveryOrder order, String transactionSn) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(transactionSn)
                .orElseThrow(() -> new IllegalArgumentException(
                        "追偿资金交易不存在，fundsTransactionSn = " + transactionSn));
        AssertUtils.equals(order.getTenantId(), transaction.getTenantId(), "追偿资金交易租户不一致");
        AssertUtils.equals(FundsTransactionState.CLOSED, transaction.getState(), "追偿资金交易必须已关闭");
        AssertUtils.equals(RECOVERY_BUSINESS_SCENE, transaction.getBusinessScene(),
                "追偿资金交易 businessScene 必须为 RECOVERY");
        AssertUtils.notNull(transaction.getAmount(), "追偿资金交易金额不能为空");
        AssertUtils.isTrue(transaction.getAmount() > 0, "追偿资金交易金额必须大于 0");
        AssertUtils.equals(order.getCurrency(), transaction.getCurrency(), "追偿资金交易币种不一致");

        List<FundsTransactionDetailDTO> details = fundsTransactionQueryService.queryFundsTransactionDetails(transactionSn);
        boolean responsibleSubjectIncluded = details.stream().anyMatch(detail ->
                detail.getState() == FundsTransactionDetailState.SUCCEEDED
                        && Objects.equals(order.getTenantId(), detail.getTenantId())
                        && Objects.equals(order.getResponsibleSubjectType(), detail.getSubjectType())
                        && Objects.equals(order.getResponsibleSubjectId(), detail.getSubjectId())
                        && Objects.equals(transaction.getAmount(), detail.getAmount())
                        && order.getCurrency() == detail.getCurrency());
        AssertUtils.isTrue(responsibleSubjectIncluded, "追偿资金交易未包含责任主体成功明细");
        return transaction;
    }

    private RecoveryOrderDTO sameSource(RecoveryOrder order, String digest) {
        AssertUtils.equals(order.getOrderDigest(), digest, "同一追偿来源事实已发生漂移");
        return toDTO(order);
    }

    private RecoveryOrderDTO sameResult(RecoveryOrder order,
                                        RecoveryResult result,
                                        RecordRecoveryResultRequest request,
                                        String digest) {
        AssertUtils.equals(order.getSn(), result.getRecoveryOrderSn(), "追偿结果幂等键已被其他追偿单占用");
        AssertUtils.equals(request.getFundsTransactionSn(), result.getFundsTransactionSn(),
                "追偿结果幂等键对应不同资金交易");
        AssertUtils.equals(digest, result.getResultDigest(), "追偿结果幂等请求事实已发生漂移");
        return toDTO(order);
    }

    private RecoveryOrder selectBySource(CreateRecoveryOrderRequest request) {
        return recoveryOrderMapper.selectBySource(request.getTenantId(), request.getSourceType(), request.getSourceSn(),
                request.getResponsibleSubjectType(), request.getResponsibleSubjectId(), request.getCurrency().name());
    }

    private RecoveryOrder selectBySourceForUpdate(CreateRecoveryOrderRequest request) {
        return recoveryOrderMapper.selectBySourceForUpdate(request.getTenantId(), request.getSourceType(),
                request.getSourceSn(), request.getResponsibleSubjectType(), request.getResponsibleSubjectId(),
                request.getCurrency().name());
    }

    private RecoveryOrder requiredOrder(Long tenantId, String recoveryOrderSn) {
        RecoveryOrder result = recoveryOrderMapper.selectBySn(tenantId, recoveryOrderSn);
        AssertUtils.notNull(result, "追偿单不存在，recoveryOrderSn = {}", recoveryOrderSn);
        return result;
    }

    private RecoveryOrder requiredOrderForUpdate(Long tenantId, String recoveryOrderSn) {
        RecoveryOrder result = recoveryOrderMapper.selectBySnForUpdate(tenantId, recoveryOrderSn);
        AssertUtils.notNull(result, "追偿单不存在，recoveryOrderSn = {}", recoveryOrderSn);
        return result;
    }

    private RecoveryOrderDTO toDTO(RecoveryOrder source) {
        return new RecoveryOrderDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSourceType(source.getSourceType())
                .setSourceSn(source.getSourceSn())
                .setResponsibleSubjectType(source.getResponsibleSubjectType())
                .setResponsibleSubjectId(source.getResponsibleSubjectId())
                .setExpectedAmount(source.getExpectedAmount())
                .setRecoveredAmount(source.getRecoveredAmount())
                .setRemainingAmount(source.getExpectedAmount() - source.getRecoveredAmount())
                .setCurrency(source.getCurrency())
                .setState(source.getState())
                .setLastFundsTransactionSn(source.getLastFundsTransactionSn())
                .setRecoveredTime(source.getRecoveredTime());
    }

    private String createDigest(CreateRecoveryOrderRequest request) {
        return FundsStableHashSupport.sha256Json(Map.of(
                "tenantId", request.getTenantId(),
                "sourceType", request.getSourceType(),
                "sourceSn", request.getSourceSn(),
                "responsibleSubjectType", request.getResponsibleSubjectType(),
                "responsibleSubjectId", request.getResponsibleSubjectId(),
                "expectedAmount", request.getExpectedAmount(),
                "currency", request.getCurrency().name(),
                "sourceDigest", request.getSourceDigest(),
                "approvalRef", request.getApprovalRef(),
                "evidenceRef", request.getEvidenceRef()));
    }

    private String resultRequestDigest(RecordRecoveryResultRequest request) {
        return FundsStableHashSupport.sha256Json(Map.of(
                "tenantId", request.getTenantId(),
                "recoveryOrderSn", request.getRecoveryOrderSn(),
                "fundsTransactionSn", request.getFundsTransactionSn(),
                "idempotencyKey", request.getIdempotencyKey(),
                "approvalRef", request.getApprovalRef(),
                "evidenceRef", request.getEvidenceRef()));
    }

    private void validateCreateRequest(CreateRecoveryOrderRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建追偿单请求不能为空");
        AssertUtils.notNull(operator, "创建追偿单操作人不能为空");
        validateTenant(request.getTenantId());
        AssertUtils.hasText(request.getSourceType(), "追偿来源类型不能为空");
        AssertUtils.hasText(request.getSourceSn(), "追偿来源流水不能为空");
        AssertUtils.hasText(request.getResponsibleSubjectType(), "追偿责任主体类型不能为空");
        AssertUtils.hasText(request.getResponsibleSubjectId(), "追偿责任主体不能为空");
        AssertUtils.notNull(request.getExpectedAmount(), "追偿应追金额不能为空");
        AssertUtils.isTrue(request.getExpectedAmount() > 0, "追偿应追金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "追偿币种不能为空");
        AssertUtils.isTrue(isSha256(request.getSourceDigest()), "追偿来源摘要必须是 SHA-256");
        AssertUtils.hasText(request.getApprovalRef(), "追偿来源审批引用不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "追偿来源证据引用不能为空");
    }

    private void validateRecordRequest(RecordRecoveryResultRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "登记追偿结果请求不能为空");
        AssertUtils.notNull(operator, "登记追偿结果操作人不能为空");
        validateQuery(request.getTenantId(), request.getRecoveryOrderSn());
        AssertUtils.hasText(request.getFundsTransactionSn(), "追偿资金交易流水不能为空");
        AssertUtils.hasText(request.getIdempotencyKey(), "追偿结果幂等键不能为空");
        AssertUtils.hasText(request.getApprovalRef(), "追偿结果审批引用不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "追偿结果证据引用不能为空");
    }

    private void validateQuery(Long tenantId, String recoveryOrderSn) {
        validateTenant(tenantId);
        AssertUtils.hasText(recoveryOrderSn, "追偿单流水号不能为空");
    }

    private void validateTenant(Long tenantId) {
        AssertUtils.notNull(tenantId, "追偿租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId, "追偿 tenantId 与当前租户不一致");
    }

    private boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }
}
