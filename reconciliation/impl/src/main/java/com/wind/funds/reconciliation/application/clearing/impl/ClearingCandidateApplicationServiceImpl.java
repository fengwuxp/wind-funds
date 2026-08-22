package com.wind.funds.reconciliation.application.clearing.impl;

import com.wind.jackson.WindJson;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.application.clearing.ClearingCandidateApplicationService;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ClearingCandidate;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitResultSnapshot;
import com.wind.funds.reconciliation.dal.mapper.ClearingCandidateMapper;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplitResultSnapshotMapper;
import com.wind.funds.reconciliation.dal.entities.table.ClearingCandidateNameRefs;
import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import com.wind.funds.reconciliation.model.dto.ClearingCandidateDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.query.ClearingCandidateQuery;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ExcludeClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.LockClearingCandidateRequest;
import com.wind.funds.reconciliation.model.request.ReleaseClearingCandidateLockRequest;
import com.wind.funds.reconciliation.model.request.RestoreClearingCandidateRequest;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;

/**
 * 清算候选应用服务实现。
 *
 * <p>本实现只生成和管理清算资格事实。清算候选状态变化不会写入资金交易、账本分录或余额。</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class ClearingCandidateApplicationServiceImpl implements ClearingCandidateApplicationService {

    private static final WindSequenceType CANDIDATE_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_CANDIDATE", "CLC", 6);

    private final ClearingCandidateMapper clearingCandidateMapper;

    private final ClearingSplitResultSnapshotMapper clearingSplitResultSnapshotMapper;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingCandidateDTO createCandidate(CreateClearingCandidateRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        ClearingSplitResultSnapshot snapshot = clearingSplitResultSnapshotMapper.selectBySn(
                request.getTenantId(), request.getSplitResultSn());
        AssertUtils.notNull(snapshot, "清分结果快照不存在，splitResultSn = {}", request.getSplitResultSn());
        Long amount = snapshot.getAmount();
        AssertUtils.notNull(amount, "清分结果快照金额不能为空，splitResultSn = {}", snapshot.getSn());
        AssertUtils.isTrue(amount > 0, "清算候选金额必须大于 0，splitResultSn = {}", snapshot.getSn());

        String candidateDigest = candidateDigest(snapshot, request, amount);
        ClearingCandidate existing = clearingCandidateMapper.selectByDigest(request.getTenantId(), candidateDigest);
        if (existing != null) {
            return toDTO(existing);
        }
        ClearingCandidate active = clearingCandidateMapper.selectByActiveDetailForUpdate(
                request.getTenantId(), snapshot.getSplittableDetailSn());
        AssertUtils.isTrue(active == null, "同一可清分明细已有有效清算候选，splittableDetailSn = {}",
                snapshot.getSplittableDetailSn());

        ClearingCandidate candidate = toCandidate(snapshot, request, amount,
                candidateDigest, operator);
        CandidateDecision decision = evaluate(candidate, operator);
        applyDecision(candidate, decision, operator);
        try {
            clearingCandidateMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ClearingCandidate winner = clearingCandidateMapper.selectByDigest(
                    request.getTenantId(), candidateDigest);
            AssertUtils.notNull(winner, "清算候选唯一键冲突后未找到幂等结果");
            return toDTO(winner);
        }
        AssertUtils.notNull(candidate.getId(), "创建清算候选失败");
        log.info("清算候选生成完成，tenantId = {}, candidateSn = {}, splitResultSn = {}, status = {}",
                request.getTenantId(), candidate.getSn(), request.getSplitResultSn(), candidate.getState());
        return toDTO(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingCandidateDTO excludeCandidate(ExcludeClearingCandidateRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getCandidateSn(), operator);
        AssertUtils.notNull(request, "排除清算候选请求不能为空");
        AssertUtils.hasText(request.getReason(), "清算候选排除原因不能为空");
        ClearingCandidate candidate = requiredCandidateForUpdate(request.getTenantId(), request.getCandidateSn());
        if (candidate.getState() == ClearingCandidateState.EXCLUDED) {
            return toDTO(candidate);
        }
        AssertUtils.isTrue(candidate.getState() != ClearingCandidateState.LOCKED
                        && candidate.getState() != ClearingCandidateState.CLEARED,
                "LOCKED 或 CLEARED 清算候选不能直接排除，candidateSn = {}", candidate.getSn());
        candidate.setState(ClearingCandidateState.EXCLUDED);
        candidate.setActiveSplittableDetailSn(null);
        candidate.setExclusionReason(request.getReason());
        candidate.setBlockReason(null);
        candidate.setLockedClearingBatchSn(null);
        candidate.setUpdatedBy(operator.getOperatorAsText());
        candidate.setStatusChangedTime(LocalDateTime.now());
        AssertUtils.isTrue(clearingCandidateMapper.update(candidate) == 1, "排除清算候选失败");
        return toDTO(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingCandidateDTO restoreCandidate(RestoreClearingCandidateRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getCandidateSn(), operator);
        ClearingCandidate candidate = requiredCandidateForUpdate(request.getTenantId(), request.getCandidateSn());
        if (candidate.getState() == ClearingCandidateState.READY
                || candidate.getState() == ClearingCandidateState.LOCKED
                || candidate.getState() == ClearingCandidateState.CLEARED) {
            return toDTO(candidate);
        }
        CandidateDecision decision = evaluate(candidate, operator);
        applyDecision(candidate, decision, operator);
        AssertUtils.isTrue(clearingCandidateMapper.update(candidate) == 1, "重新评估清算候选失败");
        return toDTO(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingCandidateDTO lockCandidate(LockClearingCandidateRequest request, WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getCandidateSn(), operator);
        AssertUtils.notNull(request, "锁定清算候选请求不能为空");
        AssertUtils.hasText(request.getClearingBatchSn(), "清算批次流水号不能为空");
        ClearingCandidate candidate = requiredCandidateForUpdate(request.getTenantId(), request.getCandidateSn());
        if (candidate.getState() == ClearingCandidateState.LOCKED) {
            AssertUtils.equals(request.getClearingBatchSn(), candidate.getLockedClearingBatchSn(),
                    "清算候选已被其他批次锁定，candidateSn = {}", candidate.getSn());
            return toDTO(candidate);
        }
        AssertUtils.equals(ClearingCandidateState.READY, candidate.getState(),
                "只有 READY 清算候选可以锁定，status = {}", candidate.getState());
        candidate.setState(ClearingCandidateState.LOCKED);
        candidate.setLockedClearingBatchSn(request.getClearingBatchSn());
        candidate.setUpdatedBy(operator.getOperatorAsText());
        candidate.setStatusChangedTime(LocalDateTime.now());
        AssertUtils.isTrue(clearingCandidateMapper.update(candidate) == 1, "锁定清算候选失败");
        return toDTO(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingCandidateDTO releaseCandidateLock(ReleaseClearingCandidateLockRequest request,
                                                     WindOperator operator) {
        validateCommandRequest(request == null ? null : request.getTenantId(),
                request == null ? null : request.getCandidateSn(), operator);
        AssertUtils.notNull(request, "释放清算候选锁定请求不能为空");
        AssertUtils.hasText(request.getClearingBatchSn(), "清算批次流水号不能为空");
        ClearingCandidate candidate = requiredCandidateForUpdate(request.getTenantId(), request.getCandidateSn());
        if (candidate.getState() == ClearingCandidateState.READY) {
            return toDTO(candidate);
        }
        AssertUtils.equals(ClearingCandidateState.LOCKED, candidate.getState(),
                "只有 LOCKED 清算候选可以释放批次锁定，status = {}", candidate.getState());
        AssertUtils.equals(request.getClearingBatchSn(), candidate.getLockedClearingBatchSn(),
                "清算候选锁定批次不一致，candidateSn = {}", candidate.getSn());
        candidate.setState(ClearingCandidateState.READY);
        candidate.setLockedClearingBatchSn(null);
        candidate.setBlockReason(null);
        candidate.setExclusionReason(null);
        candidate.setUpdatedBy(operator.getOperatorAsText());
        candidate.setStatusChangedTime(LocalDateTime.now());
        AssertUtils.isTrue(clearingCandidateMapper.update(candidate) == 1, "释放清算候选锁定失败");
        return toDTO(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public ClearingCandidateDTO getCandidate(Long tenantId, String candidateSn) {
        validateQuery(tenantId, candidateSn);
        return toDTO(requiredCandidate(tenantId, candidateSn));
    }

    @Override
    @Transactional(readOnly = true)
    public WindPagination<ClearingCandidateDTO> queryCandidates(
            ClearingCandidateQuery query,
            WindQuery<? extends QueryOrderField> options) {
        AssertUtils.notNull(query, "清算候选查询条件不能为空");
        AssertUtils.notNull(options, "清算候选查询选项不能为空");
        Long currentTenantId = TenantContextHolder.requireTenantId();
        Long tenantId = query.getTenantId() == null ? currentTenantId : query.getTenantId();
        AssertUtils.equals(currentTenantId, tenantId, "清算候选查询 tenantId 与当前租户不一致");
        ClearingCandidateNameRefs candidate = ClearingCandidateNameRefs.clearingCandidate;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(candidate)
                .where(candidate.tenantId.eq(tenantId))
                .and(candidate.state.eq(query.getState()))
                .and(candidate.clearingAvailableTime.le(query.getClearingAvailableTimeMax()))
                .and(candidate.statusChangedTime.le(query.getStatusChangedTimeMax()))
                .and(candidate.lockedClearingBatchSn.eq(query.getLockedClearingBatchSn()))
                .orderBy(candidate.clearingAvailableTime.asc(), candidate.id.asc());
        return MybatisQueryHelper.<ClearingCandidate, ClearingCandidateDTO>query(wrapper)
                .counter(clearingCandidateMapper::selectCountByQuery)
                .resultQueryFunc(clearingCandidateMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private CandidateDecision evaluate(ClearingCandidate candidate, WindOperator operator) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(candidate.getClearingAvailableTime())) {
            return new CandidateDecision(ClearingCandidateState.WAITING_PERIOD, null);
        }
        ReconciliationGateDecisionDTO gate = reconciliationGateApplicationService.inspectGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(candidate.getTenantId())
                        .setStageRef(new GateStageRef()
                                .setStageKind("CLEARING_CONFIRM_ITEM")
                                .setStageIdentity(new StableIdentity()
                                        .setOwnerNamespace("clearing-candidate")
                                        .setValue(candidate.getSn()))),
                operator);
        if (gate.isPassed()) {
            return new CandidateDecision(ClearingCandidateState.READY, null);
        }
        return new CandidateDecision(ClearingCandidateState.BLOCKED, "RECONCILIATION_BLOCKED");
    }

    private void applyDecision(ClearingCandidate candidate, CandidateDecision decision, WindOperator operator) {
        candidate.setState(decision.state());
        candidate.setActiveSplittableDetailSn(decision.state() == ClearingCandidateState.EXCLUDED
                ? null : candidate.getSplittableDetailSn());
        candidate.setBlockReason(decision.blockReason());
        candidate.setExclusionReason(null);
        candidate.setLockedClearingBatchSn(null);
        candidate.setUpdatedBy(operator.getOperatorAsText());
        candidate.setStatusChangedTime(LocalDateTime.now());
    }

    private ClearingCandidate toCandidate(ClearingSplitResultSnapshot snapshot,
                                          CreateClearingCandidateRequest request,
                                          Long amount,
                                          String candidateDigest,
                                          WindOperator operator) {
        ClearingCandidate result = new ClearingCandidate();
        result.setSn(TemporalSequenceFactory.hourNext(CANDIDATE_SEQUENCE_TYPE));
        result.setTenantId(snapshot.getTenantId());
        result.setSplitResultSn(snapshot.getSn());
        result.setSplitBatchSn(snapshot.getSplitBatchSn());
        result.setSplittableDetailSn(snapshot.getSplittableDetailSn());
        result.setSubjectType(snapshot.getSubjectType());
        result.setSubjectId(snapshot.getSubjectId());
        result.setCurrency(snapshot.getCurrency());
        result.setBusinessLine(snapshot.getBusinessLine());
        result.setClearingPeriod(request.getClearingPeriod());
        result.setAmount(amount);
        result.setFundsTransactionSn(snapshot.getFundsTransactionSn());
        result.setFundsTransactionDetailSn(snapshot.getFundsTransactionDetailSn());
        result.setLedgerTransactionSn(snapshot.getLedgerTransactionSn());
        result.setPostingPlanSn(snapshot.getPostingPlanSn());
        result.setLedgerEntrySn(snapshot.getLedgerEntrySn());
        result.setRouteSnapshotDigest(snapshot.getRouteSnapshotDigest());
        result.setClearingAvailableTime(request.getClearingAvailableTime());
        result.setClearingRuleCode(request.getClearingRuleCode());
        result.setClearingRuleVersion(request.getClearingRuleVersion());
        result.setGateEvidenceRef(snapshot.getGateEvidenceRef());
        result.setReconciliationEvidenceRefs(snapshot.getReconciliationEvidenceRefs());
        result.setSourceDigest(snapshot.getSourceDigest());
        result.setCandidateDigest(candidateDigest);
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private String candidateDigest(ClearingSplitResultSnapshot snapshot,
                                   CreateClearingCandidateRequest request,
                                   Long amount) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", snapshot.getTenantId());
        facts.put("splitResultSn", snapshot.getSn());
        facts.put("snapshotDigest", snapshot.getSnapshotDigest());
        facts.put("splittableDetailSn", snapshot.getSplittableDetailSn());
        facts.put("amount", amount);
        facts.put("clearingPeriod", request.getClearingPeriod());
        facts.put("clearingAvailableTime", request.getClearingAvailableTime());
        facts.put("clearingRuleCode", request.getClearingRuleCode());
        facts.put("clearingRuleVersion", request.getClearingRuleVersion());
        facts.put("gateEvidenceRef", snapshot.getGateEvidenceRef());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private ClearingCandidate requiredCandidate(Long tenantId, String candidateSn) {
        ClearingCandidate result = clearingCandidateMapper.selectBySn(tenantId, candidateSn);
        AssertUtils.notNull(result, "清算候选不存在，candidateSn = {}", candidateSn);
        return result;
    }

    private ClearingCandidate requiredCandidateForUpdate(Long tenantId, String candidateSn) {
        ClearingCandidate result = clearingCandidateMapper.selectBySnForUpdate(tenantId, candidateSn);
        AssertUtils.notNull(result, "清算候选不存在，candidateSn = {}", candidateSn);
        return result;
    }

    private void validateCreateRequest(CreateClearingCandidateRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "清算候选创建请求不能为空");
        validateQuery(request.getTenantId(), request.getSplitResultSn());
        AssertUtils.hasText(request.getClearingPeriod(), "清算周期不能为空");
        AssertUtils.hasText(request.getClearingRuleCode(), "清算规则编码不能为空");
        AssertUtils.hasText(request.getClearingRuleVersion(), "清算规则版本不能为空");
        AssertUtils.notNull(request.getClearingAvailableTime(), "最早可清算时间不能为空");
        AssertUtils.notNull(operator, "清算候选创建操作人不能为空");
    }

    private void validateCommandRequest(Long tenantId, String candidateSn, WindOperator operator) {
        validateQuery(tenantId, candidateSn);
        AssertUtils.notNull(operator, "清算候选操作人不能为空");
    }

    private void validateQuery(Long tenantId, String candidateSn) {
        AssertUtils.notNull(tenantId, "清算候选租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "清算候选 tenantId 与当前租户不一致");
        AssertUtils.isTrue(StringUtils.hasText(candidateSn), "清算候选流水号不能为空");
    }

    private ClearingCandidateDTO toDTO(ClearingCandidate source) {
        return new ClearingCandidateDTO()
                .setId(source.getId())
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSplitResultSn(source.getSplitResultSn())
                .setSplitBatchSn(source.getSplitBatchSn())
                .setSplittableDetailSn(source.getSplittableDetailSn())
                .setSubjectType(source.getSubjectType())
                .setSubjectId(source.getSubjectId())
                .setCurrency(source.getCurrency())
                .setBusinessLine(source.getBusinessLine())
                .setClearingPeriod(source.getClearingPeriod())
                .setAmount(source.getAmount())
                .setClearingAvailableTime(source.getClearingAvailableTime())
                .setFundsTransactionSn(source.getFundsTransactionSn())
                .setFundsTransactionDetailSn(source.getFundsTransactionDetailSn())
                .setLedgerTransactionSn(source.getLedgerTransactionSn())
                .setPostingPlanSn(source.getPostingPlanSn())
                .setLedgerEntrySn(source.getLedgerEntrySn())
                .setRouteSnapshotDigest(source.getRouteSnapshotDigest())
                .setClearingRuleCode(source.getClearingRuleCode())
                .setClearingRuleVersion(source.getClearingRuleVersion())
                .setGateEvidenceRef(source.getGateEvidenceRef())
                .setReconciliationEvidenceRefs(parseEvidenceRefs(source.getReconciliationEvidenceRefs()))
                .setSourceDigest(source.getSourceDigest())
                .setCandidateDigest(source.getCandidateDigest())
                .setState(source.getState())
                .setBlockReason(source.getBlockReason())
                .setExclusionReason(source.getExclusionReason())
                .setLockedClearingBatchSn(source.getLockedClearingBatchSn())
                .setStatusChangedTime(source.getStatusChangedTime())
                .setCreatedTime(source.getGmtCreate())
                .setModifiedTime(source.getGmtModified());
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    private record CandidateDecision(ClearingCandidateState state, String blockReason) {
    }
}
