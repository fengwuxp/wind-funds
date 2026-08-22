package com.wind.funds.reconciliation.application.clearing.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.reconciliation.application.clearing.ClearingBatchApplicationService;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ClearingBatch;
import com.wind.funds.reconciliation.dal.entities.ClearingBatchDetail;
import com.wind.funds.reconciliation.dal.entities.ClearingCandidate;
import com.wind.funds.reconciliation.dal.mapper.ClearingBatchDetailMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingCandidateMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationStageGateEvidenceMapper;
import com.wind.funds.reconciliation.dal.entities.table.ClearingBatchNameRefs;
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import com.wind.funds.reconciliation.model.dto.ClearingBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.query.ClearingBatchQuery;
import com.wind.funds.reconciliation.model.request.CancelClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.ReplaceClearingBatchCandidatesRequest;
import com.wind.funds.reconciliation.model.request.ReturnClearingBatchToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 清算批次应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ClearingBatchApplicationServiceImpl implements ClearingBatchApplicationService {

    private static final WindSequenceType BATCH_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_BATCH", "CLB", 6);

    private static final WindSequenceType BATCH_DETAIL_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_BATCH_DETAIL", "CLD", 6);

    private static final String FUNDS_REJECTED_BLOCK_REASON = "CLEARING_FUNDS_REJECTED";

    private static final int MAX_FAILURE_REASON_LENGTH = 512;

    private final ClearingBatchMapper clearingBatchMapper;

    private final ClearingBatchDetailMapper clearingBatchDetailMapper;

    private final ClearingCandidateMapper clearingCandidateMapper;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    private final ReconciliationStageGateEvidenceMapper stageGateEvidenceMapper;

    private final FundsClearingTransactionService fundsClearingTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingBatchDTO createBatch(CreateClearingBatchRequest request, WindOperator operator) {
        validateCandidateRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getCandidateSns(), operator);
        List<ClearingCandidate> candidates = requiredCandidates(request.getTenantId(), request.getCandidateSns());
        validateBatchBoundary(candidates, ClearingCandidateState.READY);
        ClearingBatch batch = newBatch(request.getTenantId(), candidates, operator);
        clearingBatchMapper.insertSelective(batch);
        AssertUtils.notNull(batch.getId(), "创建清算批次失败");
        replaceDetails(batch, candidates, operator);
        return toDTO(requiredBatch(request.getTenantId(), batch.getSn()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingBatchDTO replaceDraftCandidates(ReplaceClearingBatchCandidatesRequest request,
                                                    WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getClearingBatchSn(), operator);
        validateCandidateSns(request.getCandidateSns());
        ClearingBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getClearingBatchSn());
        AssertUtils.isTrue(batch.getState() == ClearingBatchState.DRAFT,
                "只有 DRAFT 清算批次可以替换候选，status = {}", batch.getState());
        List<ClearingCandidate> candidates = requiredCandidates(request.getTenantId(), request.getCandidateSns());
        validateBatchBoundary(candidates, ClearingCandidateState.READY);
        applySummary(batch, candidates);
        AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "更新清算批次草稿失败");
        replaceDetails(batch, candidates, operator);
        return toDTO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingBatchDTO submitBatch(SubmitClearingBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getClearingBatchSn(), operator);
        ClearingBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getClearingBatchSn());
        if (batch.getState() == ClearingBatchState.REVIEWING) {
            validateReviewingBatch(batch);
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getState() == ClearingBatchState.DRAFT,
                "只有 DRAFT 清算批次可以提交复核，status = {}", batch.getState());
        List<ClearingCandidate> candidates = batchCandidates(batch);
        validateBatchBoundary(candidates, ClearingCandidateState.READY);
        applySummary(batch, candidates);
        replaceDetails(batch, candidates, operator);
        LocalDateTime now = LocalDateTime.now();
        String updatedBy = operator.getOperatorAsText();
        for (ClearingCandidate candidate : candidates) {
            AssertUtils.isTrue(clearingCandidateMapper.lockReadyCandidate(batch.getTenantId(), candidate.getSn(),
                            batch.getSn(), updatedBy, now) == 1,
                    "锁定清算候选失败，candidateSn = {}", candidate.getSn());
        }
        batch.setActiveAmountDigest(batch.getAmountDigest());
        batch.setState(ClearingBatchState.REVIEWING);
        batch.setSubmittedBy(updatedBy);
        batch.setSubmittedTime(now);
        AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "提交清算批次复核失败");
        return toDTO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingBatchDTO returnToDraft(ReturnClearingBatchToDraftRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getClearingBatchSn(), operator);
        validateReason(request.getReason(), ReturnClearingBatchToDraftRequest.MAX_REASON_LENGTH, "清算批次退回原因");
        ClearingBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getClearingBatchSn());
        if (batch.getState() == ClearingBatchState.DRAFT) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getState() == ClearingBatchState.REVIEWING,
                "只有 REVIEWING 清算批次可以退回草稿，status = {}", batch.getState());
        assertNoFundsFact(batch);
        releaseCandidates(batch, operator);
        batch.setState(ClearingBatchState.DRAFT);
        batch.setActiveAmountDigest(null);
        releaseActiveAmountDigest(batch);
        batch.setReturnedBy(operator.getOperatorAsText());
        batch.setReturnedTime(LocalDateTime.now());
        batch.setReturnReason(request.getReason());
        AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "退回清算批次草稿失败");
        return toDTO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public ClearingBatchDTO confirmBatch(ConfirmClearingBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getClearingBatchSn(), operator);
        ClearingBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getClearingBatchSn());
        if (batch.getState() == ClearingBatchState.CONFIRMED) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getState() == ClearingBatchState.REVIEWING,
                "只有 REVIEWING 清算批次可以确认，status = {}", batch.getState());
        List<ClearingCandidate> candidates = validateReviewingBatch(batch);
        List<ReconciliationGateDecisionDTO> gateDecisions = candidates.stream()
                .map(candidate -> validateCurrentCandidate(batch, candidate, operator))
                .toList();
        try {
            String fundsTransactionSn = fundsClearingTransactionService.confirm(new FundsClearingConfirmRequest()
                    .setAccountId(FundsAccountId.immutable(batch.getSubjectId(), batch.getSubjectType()))
                    .setAmount(Money.immutable(batch.getTotalAmount(), batch.getCurrency()))
                    .setClearingBatchSn(batch.getSn())
                    .setSourceTransactionSns(candidates.stream()
                            .map(ClearingCandidate::getFundsTransactionSn)
                            .distinct()
                            .sorted()
                            .toList())
                    .setDescription("clearing batch confirmation"), operator);
            markCandidatesCleared(batch, candidates, gateDecisions, operator);
            batch.setFundsTransactionSn(fundsTransactionSn);
            batch.setState(ClearingBatchState.CONFIRMED);
            batch.setActiveAmountDigest(null);
            releaseActiveAmountDigest(batch);
            batch.setConfirmedBy(operator.getOperatorAsText());
            batch.setConfirmedTime(LocalDateTime.now());
            AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "确认清算批次失败");
            log.info("清算批次确认完成，tenantId = {}, clearingBatchSn = {}, fundsTransactionSn = {}",
                    batch.getTenantId(), batch.getSn(), fundsTransactionSn);
            return toDTO(batch);
        } catch (LedgerPostingRejectedException exception) {
            recordDeterministicFailure(batch, candidates, exception, operator);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingBatchDTO cancelBatch(CancelClearingBatchRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getClearingBatchSn(), operator);
        validateReason(request.getReason(), CancelClearingBatchRequest.MAX_REASON_LENGTH, "清算批次取消原因");
        ClearingBatch batch = requiredBatchForUpdate(request.getTenantId(), request.getClearingBatchSn());
        if (batch.getState() == ClearingBatchState.CANCELLED) {
            return toDTO(batch);
        }
        AssertUtils.isTrue(batch.getState() == ClearingBatchState.DRAFT
                        || batch.getState() == ClearingBatchState.REVIEWING,
                "只有 DRAFT 或 REVIEWING 清算批次可以取消，status = {}", batch.getState());
        if (batch.getState() == ClearingBatchState.REVIEWING) {
            assertNoFundsFact(batch);
            releaseCandidates(batch, operator);
        }
        batch.setState(ClearingBatchState.CANCELLED);
        batch.setActiveAmountDigest(null);
        releaseActiveAmountDigest(batch);
        batch.setCancelledBy(operator.getOperatorAsText());
        batch.setCancelledTime(LocalDateTime.now());
        batch.setCancelReason(request.getReason());
        AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "取消清算批次失败");
        return toDTO(batch);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClearingBatchDTO getBatch(Long tenantId, String clearingBatchSn) {
        validateQuery(tenantId, clearingBatchSn);
        return toDTO(requiredBatch(tenantId, clearingBatchSn));
    }

    @Override
    @Transactional(readOnly = true)
    public WindPagination<ClearingBatchDTO> queryBatches(
            ClearingBatchQuery query,
            WindQuery<? extends QueryOrderField> options) {
        AssertUtils.notNull(query, "清算批次查询条件不能为空");
        AssertUtils.notNull(options, "清算批次查询选项不能为空");
        Long currentTenantId = TenantContextHolder.requireTenantId();
        Long tenantId = query.getTenantId() == null ? currentTenantId : query.getTenantId();
        AssertUtils.equals(currentTenantId, tenantId, "清算批次查询 tenantId 与当前租户不一致");
        ClearingBatchNameRefs batch = ClearingBatchNameRefs.clearingBatch;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(batch)
                .where(batch.tenantId.eq(tenantId))
                .and(batch.state.eq(query.getState()))
                .and(batch.gmtModified.le(query.getGmtModifiedMax()))
                .orderBy(batch.gmtModified.asc(), batch.id.asc());
        return MybatisQueryHelper.<ClearingBatch, ClearingBatchDTO>query(wrapper)
                .counter(clearingBatchMapper::selectCountByQuery)
                .resultQueryFunc(clearingBatchMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private ClearingBatch newBatch(Long tenantId, List<ClearingCandidate> candidates, WindOperator operator) {
        ClearingBatch result = new ClearingBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(tenantId);
        result.setState(ClearingBatchState.DRAFT);
        result.setCreatedBy(operator.getOperatorAsText());
        applySummary(result, candidates);
        return result;
    }

    private void applySummary(ClearingBatch batch, List<ClearingCandidate> candidates) {
        ClearingCandidate first = candidates.getFirst();
        batch.setSubjectType(first.getSubjectType());
        batch.setSubjectId(first.getSubjectId());
        batch.setCurrency(first.getCurrency());
        batch.setBusinessLine(first.getBusinessLine());
        batch.setClearingPeriod(first.getClearingPeriod());
        batch.setClearingRuleCode(first.getClearingRuleCode());
        batch.setClearingRuleVersion(first.getClearingRuleVersion());
        batch.setCandidateCount(candidates.size());
        batch.setTotalAmount(totalAmount(candidates));
        batch.setAmountDigest(amountDigest(batch.getTenantId(), candidates));
    }

    private void replaceDetails(ClearingBatch batch,
                                List<ClearingCandidate> candidates,
                                WindOperator operator) {
        clearingBatchDetailMapper.deleteByBatchSn(batch.getTenantId(), batch.getSn());
        for (ClearingCandidate candidate : candidates) {
            ClearingBatchDetail detail = new ClearingBatchDetail();
            detail.setSn(TemporalSequenceFactory.hourNext(BATCH_DETAIL_SEQUENCE_TYPE));
            detail.setTenantId(batch.getTenantId());
            detail.setClearingBatchSn(batch.getSn());
            detail.setCandidateSn(candidate.getSn());
            detail.setSplitBatchSn(candidate.getSplitBatchSn());
            detail.setSplittableDetailSn(candidate.getSplittableDetailSn());
            detail.setFundsTransactionDetailSn(candidate.getFundsTransactionDetailSn());
            detail.setLedgerEntrySn(candidate.getLedgerEntrySn());
            detail.setAmount(candidate.getAmount());
            detail.setCurrency(candidate.getCurrency());
            detail.setCreatedBy(operator.getOperatorAsText());
            clearingBatchDetailMapper.insertSelective(detail);
        }
    }

    private List<ClearingCandidate> validateReviewingBatch(ClearingBatch batch) {
        List<ClearingBatchDetail> details = clearingBatchDetailMapper.selectByBatchSn(
                batch.getTenantId(), batch.getSn());
        AssertUtils.isTrue(details.size() == batch.getCandidateCount(), "清算批次候选数量与摘要不一致");
        List<ClearingCandidate> candidates = requiredCandidates(batch.getTenantId(),
                details.stream().map(ClearingBatchDetail::getCandidateSn).toList());
        validateBatchBoundary(candidates, ClearingCandidateState.LOCKED);
        for (ClearingCandidate candidate : candidates) {
            AssertUtils.equals(batch.getSn(), candidate.getLockedClearingBatchSn(),
                    "清算候选未由当前批次锁定，candidateSn = {}", candidate.getSn());
            ClearingBatchDetail detail = details.stream()
                    .filter(item -> item.getCandidateSn().equals(candidate.getSn()))
                    .findFirst()
                    .orElseThrow();
            AssertUtils.isTrue(Objects.equals(detail.getSplitBatchSn(), candidate.getSplitBatchSn())
                            && Objects.equals(detail.getSplittableDetailSn(), candidate.getSplittableDetailSn())
                            && Objects.equals(detail.getFundsTransactionDetailSn(), candidate.getFundsTransactionDetailSn())
                            && Objects.equals(detail.getLedgerEntrySn(), candidate.getLedgerEntrySn())
                            && Objects.equals(detail.getAmount(), candidate.getAmount())
                            && detail.getCurrency() == candidate.getCurrency(),
                    "清算批次明细与候选事实不一致，candidateSn = {}", candidate.getSn());
        }
        AssertUtils.isTrue(totalAmount(candidates) == batch.getTotalAmount()
                        && amountDigest(batch.getTenantId(), candidates).equals(batch.getAmountDigest())
                        && Objects.equals(batch.getActiveAmountDigest(), batch.getAmountDigest()),
                "清算批次经济范围摘要不一致");
        return candidates;
    }

    private ReconciliationGateDecisionDTO validateCurrentCandidate(ClearingBatch batch,
                                                                   ClearingCandidate candidate,
                                                                   WindOperator operator) {
        AssertUtils.isTrue(!candidate.getClearingAvailableTime().isAfter(LocalDateTime.now().plusSeconds(1)),
                "清算候选尚未到可清算时间，candidateSn = {}", candidate.getSn());
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(
                candidate.getFundsTransactionSn()).orElse(null);
        AssertUtils.notNull(transaction, "清算来源交易不存在，fundsTransactionSn = {}",
                candidate.getFundsTransactionSn());
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), batch.getTenantId())
                        && (transaction.getState() == FundsTransactionState.OPEN
                        || transaction.getState() == FundsTransactionState.CLOSED)
                        && defaultAmount(transaction.getRefundedAmount()) == 0
                        && transaction.getCurrency() == candidate.getCurrency(),
                "清算来源交易已变化，fundsTransactionSn = {}", candidate.getFundsTransactionSn());
        AssertUtils.hasText(transaction.getRouteSnapshot(),
                "清算来源交易 RouteSnapshot 不存在，fundsTransactionSn = {}", candidate.getFundsTransactionSn());
        String currentRouteDigest = FundsStableHashSupport.sha256Json(
                Map.of("routeSnapshot", transaction.getRouteSnapshot()));
        AssertUtils.equals(candidate.getRouteSnapshotDigest(), currentRouteDigest,
                "清算来源交易 RouteSnapshot 已变化，fundsTransactionSn = {}", candidate.getFundsTransactionSn());
        ReconciliationGateDecisionDTO decision = reconciliationGateApplicationService.checkGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(batch.getTenantId())
                        .setStageRef(new GateStageRef()
                                .setStageKind("CLEARING_CONFIRM_ITEM")
                                .setStageIdentity(new StableIdentity()
                                        .setOwnerNamespace("clearing-candidate")
                                        .setValue(candidate.getSn()))),
                operator);
        AssertUtils.isTrue(decision.isPassed(),
                "清算确认时对账 Gate 未通过，candidateSn = {}", candidate.getSn());
        return decision;
    }

    private void recordDeterministicFailure(ClearingBatch batch,
                                            List<ClearingCandidate> candidates,
                                            LedgerPostingRejectedException exception,
                                            WindOperator operator) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(
                exception.getFundsTransactionSn()).orElse(null);
        if (transaction == null || transaction.getState() != FundsTransactionState.FAILED) {
            throw new IllegalStateException("清算资金结果未知，不能释放候选", exception);
        }
        LocalDateTime now = LocalDateTime.now();
        String updatedBy = operator.getOperatorAsText();
        for (ClearingCandidate candidate : candidates) {
            AssertUtils.isTrue(stageGateEvidenceMapper.deleteByStage(batch.getTenantId(),
                            "CLEARING_CONFIRM_ITEM", "clearing-candidate", candidate.getSn()) == 1,
                    "删除失败清算动作的 Gate 成功消费证据失败，candidateSn = {}", candidate.getSn());
            AssertUtils.isTrue(clearingCandidateMapper.blockLockedCandidate(batch.getTenantId(), candidate.getSn(),
                            batch.getSn(), FUNDS_REJECTED_BLOCK_REASON, updatedBy, now) == 1,
                    "阻断明确失败的清算候选失败，candidateSn = {}", candidate.getSn());
        }
        batch.setFundsTransactionSn(transaction.getSn());
        batch.setState(ClearingBatchState.FAILED);
        batch.setActiveAmountDigest(null);
        releaseActiveAmountDigest(batch);
        batch.setFailedBy(updatedBy);
        batch.setFailedTime(now);
        batch.setFailureReason(truncate(exception.getMessage(), MAX_FAILURE_REASON_LENGTH));
        AssertUtils.isTrue(clearingBatchMapper.update(batch) == 1, "记录清算批次明确失败事实失败");
    }

    private void markCandidatesCleared(ClearingBatch batch,
                                       List<ClearingCandidate> candidates,
                                       List<ReconciliationGateDecisionDTO> gateDecisions,
                                       WindOperator operator) {
        LocalDateTime now = LocalDateTime.now();
        String updatedBy = operator.getOperatorAsText();
        for (int index = 0; index < candidates.size(); index++) {
            ClearingCandidate candidate = candidates.get(index);
            candidate.setGateEvidenceRef(requiredStageEvidenceRef(gateDecisions.get(index)));
            AssertUtils.isTrue(clearingCandidateMapper.update(candidate) == 1,
                    "记录清算候选 Gate 消费证据失败，candidateSn = {}", candidate.getSn());
            AssertUtils.isTrue(clearingCandidateMapper.markLockedCandidateCleared(batch.getTenantId(),
                            candidate.getSn(), batch.getSn(), updatedBy, now) == 1,
                    "推进清算候选完成失败，candidateSn = {}", candidate.getSn());
        }
    }

    private String requiredStageEvidenceRef(ReconciliationGateDecisionDTO decision) {
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "Gate 通过时必须持有消费证据");
        return decision.getEvidenceRefs().getFirst();
    }

    private void releaseCandidates(ClearingBatch batch, WindOperator operator) {
        List<ClearingCandidate> candidates = validateReviewingBatch(batch);
        LocalDateTime now = LocalDateTime.now();
        String updatedBy = operator.getOperatorAsText();
        for (ClearingCandidate candidate : candidates) {
            AssertUtils.isTrue(clearingCandidateMapper.releaseLockedCandidate(batch.getTenantId(), candidate.getSn(),
                            batch.getSn(), updatedBy, now) == 1,
                    "释放清算候选失败，candidateSn = {}", candidate.getSn());
        }
    }

    private void assertNoFundsFact(ClearingBatch batch) {
        AssertUtils.isTrue(fundsTransactionQueryService.findFundsTransactionByBusiness(
                        batch.getTenantId(), FundsTransactionEventType.CLEARING_CONFIRM.name(), batch.getSn()).isEmpty(),
                "清算批次已存在资金事实，不能退回或取消，clearingBatchSn = {}", batch.getSn());
    }

    private void releaseActiveAmountDigest(ClearingBatch batch) {
        AssertUtils.isTrue(clearingBatchMapper.releaseActiveAmountDigest(batch.getTenantId(), batch.getSn()) == 1,
                "释放清算批次经济范围占用失败");
    }

    private List<ClearingCandidate> batchCandidates(ClearingBatch batch) {
        List<ClearingBatchDetail> details = clearingBatchDetailMapper.selectByBatchSn(
                batch.getTenantId(), batch.getSn());
        AssertUtils.isTrue(details.size() == batch.getCandidateCount(), "清算批次草稿成员数量不一致");
        return requiredCandidates(batch.getTenantId(), details.stream()
                .map(ClearingBatchDetail::getCandidateSn)
                .toList());
    }

    private List<ClearingCandidate> requiredCandidates(Long tenantId, List<String> candidateSns) {
        List<String> sortedSns = candidateSns.stream().sorted().toList();
        AssertUtils.isTrue(sortedSns.stream().distinct().count() == sortedSns.size(),
                "清算候选流水号不能重复");
        List<ClearingCandidate> result = clearingCandidateMapper.selectBySns(tenantId, sortedSns);
        AssertUtils.isTrue(result.size() == sortedSns.size(), "存在未找到的清算候选");
        return result;
    }

    private void validateBatchBoundary(List<ClearingCandidate> candidates,
                                       ClearingCandidateState requiredState) {
        AssertUtils.notEmpty(candidates, "清算候选不能为空");
        ClearingCandidate first = candidates.getFirst();
        for (ClearingCandidate candidate : candidates) {
            AssertUtils.isTrue(candidate.getState() == requiredState,
                    "清算候选状态必须为 {}，candidateSn = {}", requiredState, candidate.getSn());
            AssertUtils.isTrue(candidate.getAmount() != null && candidate.getAmount() > 0,
                    "清算候选金额必须大于 0，candidateSn = {}", candidate.getSn());
            AssertUtils.isTrue(Objects.equals(first.getSubjectType(), candidate.getSubjectType())
                            && Objects.equals(first.getSubjectId(), candidate.getSubjectId()),
                    "一个清算批次必须属于同一账务主体");
            AssertUtils.isTrue(first.getCurrency() == candidate.getCurrency(),
                    "一个清算批次必须使用同一币种");
            AssertUtils.isTrue(Objects.equals(first.getBusinessLine(), candidate.getBusinessLine()),
                    "一个清算批次必须属于同一业务线");
            AssertUtils.isTrue(Objects.equals(first.getClearingPeriod(), candidate.getClearingPeriod()),
                    "一个清算批次必须属于同一清算周期");
            AssertUtils.isTrue(Objects.equals(first.getClearingRuleCode(), candidate.getClearingRuleCode())
                            && Objects.equals(first.getClearingRuleVersion(), candidate.getClearingRuleVersion()),
                    "一个清算批次必须使用同一清算规则版本");
        }
    }

    private long totalAmount(List<ClearingCandidate> candidates) {
        return candidates.stream().map(ClearingCandidate::getAmount).reduce(0L, Math::addExact);
    }

    private String amountDigest(Long tenantId, List<ClearingCandidate> candidates) {
        ClearingCandidate first = candidates.getFirst();
        List<Map<String, Object>> members = candidates.stream()
                .sorted(Comparator.comparing(ClearingCandidate::getSplittableDetailSn))
                .map(candidate -> {
                    Map<String, Object> member = new TreeMap<>();
                    member.put("splittableDetailSn", candidate.getSplittableDetailSn());
                    member.put("fundsTransactionDetailSn", candidate.getFundsTransactionDetailSn());
                    member.put("amount", candidate.getAmount());
                    member.put("currency", candidate.getCurrency());
                    return member;
                })
                .toList();
        Map<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", tenantId);
        facts.put("subjectType", first.getSubjectType());
        facts.put("subjectId", first.getSubjectId());
        facts.put("currency", first.getCurrency());
        facts.put("businessLine", first.getBusinessLine());
        facts.put("clearingPeriod", first.getClearingPeriod());
        facts.put("clearingRuleCode", first.getClearingRuleCode());
        facts.put("clearingRuleVersion", first.getClearingRuleVersion());
        facts.put("members", members);
        return FundsStableHashSupport.sha256Json(facts);
    }

    private ClearingBatch requiredBatch(Long tenantId, String clearingBatchSn) {
        ClearingBatch result = clearingBatchMapper.selectBySn(tenantId, clearingBatchSn);
        AssertUtils.notNull(result, "清算批次不存在，clearingBatchSn = {}", clearingBatchSn);
        return result;
    }

    private ClearingBatch requiredBatchForUpdate(Long tenantId, String clearingBatchSn) {
        ClearingBatch result = clearingBatchMapper.selectBySnForUpdate(tenantId, clearingBatchSn);
        AssertUtils.notNull(result, "清算批次不存在，clearingBatchSn = {}", clearingBatchSn);
        return result;
    }

    private void validateCandidateRequest(Long tenantId, List<String> candidateSns, WindOperator operator) {
        AssertUtils.notNull(tenantId, "清算批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "清算批次 tenantId 与当前租户不一致");
        validateCandidateSns(candidateSns);
        AssertUtils.notNull(operator, "清算批次操作人不能为空");
    }

    private void validateCandidateSns(List<String> candidateSns) {
        AssertUtils.notEmpty(candidateSns, "清算候选流水号不能为空");
        AssertUtils.isTrue(candidateSns.size() <= CreateClearingBatchRequest.MAX_CANDIDATE_COUNT,
                "单个清算批次候选数量不能超过 {}", CreateClearingBatchRequest.MAX_CANDIDATE_COUNT);
        AssertUtils.isTrue(candidateSns.stream().allMatch(StringUtils::hasText),
                "清算候选流水号不能为空");
    }

    private void validateCommandRequest(Long tenantId, String clearingBatchSn, WindOperator operator) {
        validateQuery(tenantId, clearingBatchSn);
        AssertUtils.notNull(operator, "清算批次操作人不能为空");
    }

    private void validateReason(String reason, int maxLength, String fieldName) {
        AssertUtils.hasText(reason, "{}不能为空", fieldName);
        AssertUtils.isTrue(reason.trim().length() <= maxLength, "{}长度不能超过 {}", fieldName, maxLength);
    }

    private void validateQuery(Long tenantId, String clearingBatchSn) {
        AssertUtils.notNull(tenantId, "清算批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "清算批次 tenantId 与当前租户不一致");
        AssertUtils.hasText(clearingBatchSn, "清算批次流水号不能为空");
    }

    private long defaultAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ClearingBatchDTO toDTO(ClearingBatch source) {
        String reason = switch (source.getState()) {
            case DRAFT -> source.getReturnReason();
            case CANCELLED -> source.getCancelReason();
            case FAILED -> source.getFailureReason();
            default -> null;
        };
        return new ClearingBatchDTO()
                .setId(source.getId())
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSubjectType(source.getSubjectType())
                .setSubjectId(source.getSubjectId())
                .setCurrency(source.getCurrency())
                .setBusinessLine(source.getBusinessLine())
                .setClearingPeriod(source.getClearingPeriod())
                .setClearingRuleCode(source.getClearingRuleCode())
                .setClearingRuleVersion(source.getClearingRuleVersion())
                .setCandidateCount(source.getCandidateCount())
                .setTotalAmount(source.getTotalAmount())
                .setAmountDigest(source.getAmountDigest())
                .setFundsTransactionSn(source.getFundsTransactionSn())
                .setState(source.getState())
                .setCreatedTime(source.getGmtCreate())
                .setSubmittedTime(source.getSubmittedTime())
                .setConfirmedTime(source.getConfirmedTime())
                .setReturnedTime(source.getReturnedTime())
                .setCancelledTime(source.getCancelledTime())
                .setFailedTime(source.getFailedTime())
                .setReason(reason);
    }
}
