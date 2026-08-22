package com.wind.funds.reconciliation.application.gate.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirement;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirementHead;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirementPair;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import com.wind.funds.reconciliation.dal.entities.ReconciliationStageGateEvidence;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchLineageMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationGateRequirementHeadMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationGateRequirementMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationGateRequirementPairMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceSnapshotMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationStageGateEvidenceMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchState;
import com.wind.funds.reconciliation.enums.ReconciliationGateBlockerCode;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.enums.ReconciliationRunOutcome;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGatePairDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationGateRequirementRequest;
import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.GateRequirementRef;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.RequiredPairRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.jackson.WindJson;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 基于要求的对账门禁实现。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@NullMarked
@Service
@AllArgsConstructor
public class ReconciliationGateApplicationServiceImpl implements ReconciliationGateApplicationService {

    private static final String REQUIREMENT_IDENTITY_NAMESPACE = "wind-funds.reconciliation.gate-requirement";

    private final ReconciliationGateRequirementMapper requirementMapper;

    private final ReconciliationGateRequirementPairMapper requirementPairMapper;

    private final ReconciliationGateRequirementHeadMapper requirementHeadMapper;

    private final ReconciliationStageGateEvidenceMapper stageGateEvidenceMapper;

    private final ReconciliationBatchLineageMapper batchLineageMapper;

    private final ReconciliationBatchMapper batchMapper;

    private final ReconciliationRunResultMapper runResultMapper;

    private final ReconciliationSourceSnapshotMapper sourceSnapshotMapper;

    private final ReconciliationDifferenceMapper differenceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GateRequirementRef recordGateRequirement(RecordReconciliationGateRequirementRequest request,
                                                    WindOperator operator) {
        validateRequirement(request, operator);
        List<RequiredPairRef> pairs = normalizedPairs(request.getRequiredPairs());
        List<String> evidenceRefs = normalizedEvidenceRefs(request.getEvidenceRefs());
        String semanticDigest = requirementSemanticDigest(request, pairs);
        String evidenceBundleDigest = FundsStableHashSupport.sha256Json(evidenceRefs);
        ReconciliationGateRequirement existing = requirementMapper.selectByStageAndVersion(
                request.getTenantId(), request.getStageRef().getStageKind(),
                request.getStageRef().getStageIdentity().getOwnerNamespace(),
                request.getStageRef().getStageIdentity().getValue(), request.getRequirementVersion());
        if (existing != null) {
            return replay(existing, semanticDigest, evidenceBundleDigest);
        }

        ReconciliationGateRequirementHead head = requirementHeadMapper.selectForUpdate(
                request.getTenantId(), request.getStageRef().getStageKind(),
                request.getStageRef().getStageIdentity().getOwnerNamespace(),
                request.getStageRef().getStageIdentity().getValue());
        existing = requirementMapper.selectByStageAndVersion(
                request.getTenantId(), request.getStageRef().getStageKind(),
                request.getStageRef().getStageIdentity().getOwnerNamespace(),
                request.getStageRef().getStageIdentity().getValue(), request.getRequirementVersion());
        if (existing != null) {
            return replay(existing, semanticDigest, evidenceBundleDigest);
        }
        validateExpectedCurrent(head, request.getExpectedCurrentRequirementRef());
        ReconciliationGateRequirement requirement = toRequirement(
                request, head, semanticDigest, evidenceRefs, evidenceBundleDigest, operator);
        if (head == null) {
            try {
                publishHead(requirement, null);
            } catch (DuplicateKeyException exception) {
                return replayFirstPublicationWinner(request, semanticDigest, evidenceBundleDigest);
            }
        }
        requirementMapper.insertSelective(requirement);
        insertPairs(requirement, pairs);
        if (head != null) {
            publishHead(requirement, head);
        }
        return toRequirementRef(requirement);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request, WindOperator operator) {
        ReconciliationGateDecisionDTO decision = evaluate(request, operator, true);
        if (decision.isPassed()) {
            ReconciliationStageGateEvidence evidence = saveConsumedEvidence(decision, operator);
            decision.setEvidenceRefs(withFirst(evidence.getSn(), decision.getEvidenceRefs()));
        }
        return decision;
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ReconciliationGateDecisionDTO inspectGate(CheckReconciliationGateRequest request, WindOperator operator) {
        return evaluate(request, operator, false);
    }

    private ReconciliationGateDecisionDTO evaluate(CheckReconciliationGateRequest request,
                                                    WindOperator operator,
                                                    boolean lockCurrentFacts) {
        validateCheck(request, operator);
        GateStageRef stageRef = request.getStageRef();
        ReconciliationGateRequirementHead head = lockCurrentFacts
                ? requirementHeadMapper.selectForUpdate(request.getTenantId(), stageRef.getStageKind(),
                stageRef.getStageIdentity().getOwnerNamespace(), stageRef.getStageIdentity().getValue())
                : requirementHeadMapper.selectByStage(request.getTenantId(), stageRef.getStageKind(),
                stageRef.getStageIdentity().getOwnerNamespace(), stageRef.getStageIdentity().getValue());
        if (head == null) {
            return blockedWithoutRequirement(stageRef, operator);
        }
        ReconciliationGateRequirement requirement = requirementMapper.selectByStageAndVersion(
                request.getTenantId(), stageRef.getStageKind(), stageRef.getStageIdentity().getOwnerNamespace(),
                stageRef.getStageIdentity().getValue(), head.getCurrentRequirementVersion());
        if (!matchesHead(requirement, head)) {
            return blockedForHead(stageRef, operator);
        }
        List<ReconciliationGateRequirementPair> requiredPairs = requirementPairMapper.selectByRequirement(
                request.getTenantId(), requirement.getRequirementIdentityOwnerNamespace(),
                requirement.getRequirementIdentityValue());
        if (requiredPairs.isEmpty()) {
            return blockedForHead(stageRef, operator);
        }

        List<ReconciliationGatePairDecisionDTO> pairDecisions = requiredPairs.stream()
                .map(pair -> evaluatePair(request.getTenantId(), pair, lockCurrentFacts))
                .toList();
        boolean passed = pairDecisions.stream().allMatch(pair -> pair.getBlockerCodes().isEmpty());
        GateRequirementRef requirementRef = toRequirementRef(requirement);
        List<String> evidenceRefs = decisionEvidence(requirement, pairDecisions);
        String decisionDigest = decisionDigest(stageRef, requirementRef, pairDecisions, passed);
        return new ReconciliationGateDecisionDTO()
                .setPassed(passed)
                .setDecisionResult(passed
                        ? ReconciliationGateDecisionResult.PASSED : ReconciliationGateDecisionResult.BLOCKED)
                .setStageRef(stageRef)
                .setRequirementRef(requirementRef)
                .setPairDecisions(pairDecisions)
                .setDecisionDigest(decisionDigest)
                .setEvidenceRefs(evidenceRefs)
                .setExplanation(passed ? "全部 mandatory pair 的 current lineage 已对平"
                        : "至少一个 mandatory pair 未满足准入条件")
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operator.getOperatorAsText());
    }

    private ReconciliationGatePairDecisionDTO evaluatePair(Long tenantId,
                                                           ReconciliationGateRequirementPair pair,
                                                           boolean lockCurrentFacts) {
        RequiredPairRef requiredPairRef = toRequiredPairRef(pair);
        List<ReconciliationGateBlockerCode> blockers = new ArrayList<>();
        ReconciliationBatchLineage lineage = lockCurrentFacts
                ? batchLineageMapper.selectForUpdate(tenantId, pair.getScopeOwnerNamespace(),
                pair.getScopeIdentityValue(), pair.getPairOwnerNamespace(), pair.getPairIdentityValue())
                : batchLineageMapper.selectByScopeAndPair(tenantId, pair.getScopeOwnerNamespace(),
                pair.getScopeIdentityValue(), pair.getPairOwnerNamespace(), pair.getPairIdentityValue());
        if (lineage == null) {
            blockers.add(ReconciliationGateBlockerCode.REQUIRED_PAIR_RUN_NOT_FOUND);
            return pairDecision(requiredPairRef, null, null, blockers, List.of());
        }
        ReconciliationBatch batch = lockCurrentFacts
                ? batchMapper.selectBySnForUpdate(tenantId, lineage.getCurrentBatchSn())
                : batchMapper.selectBySn(tenantId, lineage.getCurrentBatchSn());
        if (batch == null || !samePair(batch, pair)) {
            blockers.add(ReconciliationGateBlockerCode.RUN_NOT_CURRENT);
            return pairDecision(requiredPairRef, lineage.getCurrentBatchSn(), null, blockers, List.of());
        }
        ReconciliationRunResult run = StringUtils.hasText(batch.getRunResultSn())
                ? runResultMapper.selectBySn(tenantId, batch.getRunResultSn()) : null;
        if (batch.getState() != ReconciliationBatchState.COMPLETED || run == null
                || !Objects.equals(run.getReconciliationBatchSn(), batch.getSn())) {
            blockers.add(ReconciliationGateBlockerCode.RUN_NOT_COMPLETED);
        }
        if (run != null && !samePair(run, pair)) {
            blockers.add(ReconciliationGateBlockerCode.RUN_NOT_CURRENT);
        }
        if (run != null && !sameRule(run, pair)) {
            blockers.add(ReconciliationGateBlockerCode.RULE_MISMATCH);
        }
        List<ReconciliationSourceSnapshot> snapshots = sourceSnapshotMapper.selectByBatch(tenantId, batch.getSn());
        if (snapshots.size() != 2 || snapshots.stream().anyMatch(snapshot -> !snapshot.getCoverageComplete())) {
            blockers.add(ReconciliationGateBlockerCode.COVERAGE_INCOMPLETE);
        }
        if (run != null && run.getOutcome() != ReconciliationRunOutcome.BALANCED) {
            blockers.add(ReconciliationGateBlockerCode.RUN_NOT_BALANCED);
        }
        if (differenceMapper.countBlockingByRequiredPair(tenantId,
                pair.getScopeOwnerNamespace(), pair.getScopeIdentityValue(),
                pair.getPairOwnerNamespace(), pair.getPairIdentityValue(), lineage.getCurrentBatchSn()) > 0) {
            blockers.add(ReconciliationGateBlockerCode.BLOCKING_DIFFERENCE_PRESENT);
        }
        List<String> evidenceRefs = run == null ? List.of() : withFirst(
                "run:" + run.getSn(), parseEvidenceRefs(run.getEvidenceRefs()));
        return pairDecision(requiredPairRef, batch.getSn(), run, List.copyOf(blockers), evidenceRefs);
    }

    private ReconciliationGatePairDecisionDTO pairDecision(RequiredPairRef requiredPairRef,
                                                           String batchSn,
                                                           ReconciliationRunResult run,
                                                           List<ReconciliationGateBlockerCode> blockers,
                                                           List<String> evidenceRefs) {
        return new ReconciliationGatePairDecisionDTO()
                .setRequiredPairRef(requiredPairRef)
                .setCurrentRunResultSn(run == null ? null : run.getSn())
                .setCurrentBatchSn(batchSn)
                .setCurrentLineageRef(batchSn)
                .setResultDigest(run == null ? null : run.getResultDigest())
                .setOutcome(run == null ? null : run.getOutcome())
                .setBlockerCodes(blockers)
                .setEvidenceRefs(evidenceRefs);
    }

    private ReconciliationStageGateEvidence saveConsumedEvidence(ReconciliationGateDecisionDTO decision,
                                                                 WindOperator operator) {
        GateStageRef stageRef = decision.getStageRef();
        Long tenantId = TenantContextHolder.requireTenantId();
        ReconciliationStageGateEvidence existing = stageGateEvidenceMapper.selectByStage(
                tenantId, stageRef.getStageKind(), stageRef.getStageIdentity().getOwnerNamespace(),
                stageRef.getStageIdentity().getValue());
        if (existing != null) {
            AssertUtils.equals(existing.getDecisionDigest(), decision.getDecisionDigest(),
                    "同一 Stage identity 已消费不同 Gate 决策，禁止覆盖");
            return existing;
        }
        GateRequirementRef requirement = decision.getRequirementRef();
        String identityDigest = FundsStableHashSupport.sha256Json(stageMap(stageRef));
        ReconciliationStageGateEvidence evidence = new ReconciliationStageGateEvidence();
        evidence.setSn("RGE" + identityDigest.substring(0, 61));
        evidence.setTenantId(tenantId);
        evidence.setStageKind(stageRef.getStageKind());
        evidence.setStageIdentityOwnerNamespace(stageRef.getStageIdentity().getOwnerNamespace());
        evidence.setStageIdentityValue(stageRef.getStageIdentity().getValue());
        evidence.setRequirementIdentityOwnerNamespace(requirement.getRequirementIdentity().getOwnerNamespace());
        evidence.setRequirementIdentityValue(requirement.getRequirementIdentity().getValue());
        evidence.setRequirementVersion(requirement.getRequirementVersion());
        evidence.setRequirementSemanticDigest(requirement.getSemanticDigest());
        evidence.setRequirementEvidenceBundleDigest(requirement.getEvidenceBundleDigest());
        evidence.setConsumedPairEvidence(WindJson.toJsonString(decision.getPairDecisions().stream()
                .map(this::pairDecisionMap).toList()));
        evidence.setDecisionDigest(decision.getDecisionDigest());
        evidence.setEvidenceRefs(WindJson.toJsonString(decision.getEvidenceRefs()));
        evidence.setCreatedBy(operator.getOperatorAsText());
        try {
            stageGateEvidenceMapper.insertSelective(evidence);
        } catch (DuplicateKeyException exception) {
            ReconciliationStageGateEvidence winner = stageGateEvidenceMapper.selectByStage(
                    tenantId, stageRef.getStageKind(), stageRef.getStageIdentity().getOwnerNamespace(),
                    stageRef.getStageIdentity().getValue());
            AssertUtils.notNull(winner, "Stage Gate evidence 并发写入冲突后未找到唯一结果");
            AssertUtils.equals(winner.getDecisionDigest(), decision.getDecisionDigest(),
                    "同一 Stage identity 并发消费了不同 Gate 决策");
            return winner;
        }
        return evidence;
    }

    private void validateRequirement(RecordReconciliationGateRequirementRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "Gate Requirement 请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "Gate Requirement tenantId 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "Gate Requirement tenantId 与当前租户不一致");
        validateStageRef(request.getStageRef());
        AssertUtils.hasText(request.getRequirementVersion(), "Gate Requirement version 不能为空");
        AssertUtils.notEmpty(request.getRequiredPairs(), "Gate Requirement mandatory pair 不能为空");
        AssertUtils.notEmpty(request.getEvidenceRefs(), "Gate Requirement evidenceRefs 不能为空");
        AssertUtils.notNull(operator, "Gate Requirement 操作人不能为空");
    }

    private void validateCheck(CheckReconciliationGateRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账准入检查请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账准入检查租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账准入检查 tenantId 与当前租户不一致");
        validateStageRef(request.getStageRef());
        AssertUtils.notNull(operator, "对账准入检查操作人不能为空");
    }

    private void validateStageRef(GateStageRef stageRef) {
        AssertUtils.notNull(stageRef, "Gate Stage 引用不能为空");
        AssertUtils.hasText(stageRef.getStageKind(), "Gate Stage kind 不能为空");
        AssertUtils.notNull(stageRef.getStageIdentity(), "Gate Stage identity 不能为空");
        validateIdentity(stageRef.getStageIdentity(), "Gate Stage identity");
    }

    private void validateIdentity(StableIdentity identity, String label) {
        AssertUtils.hasText(identity.getOwnerNamespace(), label + " ownerNamespace 不能为空");
        AssertUtils.hasText(identity.getValue(), label + " value 不能为空");
    }

    private List<RequiredPairRef> normalizedPairs(List<RequiredPairRef> pairs) {
        List<RequiredPairRef> result = pairs.stream().peek(pair -> {
            AssertUtils.notNull(pair, "Gate mandatory pair 不能为空");
            AssertUtils.notNull(pair.getScopeIdentity(), "Gate mandatory pair scope 不能为空");
            AssertUtils.notNull(pair.getPairIdentity(), "Gate mandatory pair identity 不能为空");
            AssertUtils.notNull(pair.getComparisonRuleRef(), "Gate mandatory pair rule 不能为空");
            validateIdentity(pair.getScopeIdentity(), "Gate mandatory pair scope");
            validateIdentity(pair.getPairIdentity(), "Gate mandatory pair identity");
            AssertUtils.hasText(pair.getComparisonRuleRef().getNamespace(), "Gate mandatory pair rule namespace 不能为空");
            AssertUtils.hasText(pair.getComparisonRuleRef().getIdentity(), "Gate mandatory pair rule identity 不能为空");
            AssertUtils.hasText(pair.getComparisonRuleRef().getVersion(), "Gate mandatory pair rule version 不能为空");
        }).sorted(java.util.Comparator.comparing(this::pairKey)).toList();
        AssertUtils.isTrue(result.stream().map(this::pairKey).distinct().count() == result.size(),
                "Gate mandatory pair 不能重复");
        return result;
    }

    private List<String> normalizedEvidenceRefs(List<String> evidenceRefs) {
        List<String> result = evidenceRefs.stream().filter(StringUtils::hasText).map(String::trim)
                .distinct().sorted().toList();
        AssertUtils.isTrue(result.size() == evidenceRefs.size(), "Gate Requirement evidenceRefs 不能为空或重复");
        return result;
    }

    private void validateExpectedCurrent(ReconciliationGateRequirementHead head, GateRequirementRef expected) {
        if (head == null) {
            AssertUtils.isTrue(expected == null, "首次 Gate Requirement 发布不能携带 expected current");
            return;
        }
        AssertUtils.notNull(expected, "后继 Gate Requirement 发布必须携带 expected current");
        AssertUtils.isTrue(matchesHead(expected, head), "Gate Requirement expected current 冲突");
    }

    private GateRequirementRef replay(ReconciliationGateRequirement existing,
                                      String semanticDigest,
                                      String evidenceBundleDigest) {
        AssertUtils.isTrue(Objects.equals(existing.getSemanticDigest(), semanticDigest)
                        && Objects.equals(existing.getEvidenceBundleDigest(), evidenceBundleDigest),
                "同一 Stage 与 Requirement version 的语义或证据冲突");
        return toRequirementRef(existing);
    }

    private GateRequirementRef replayFirstPublicationWinner(RecordReconciliationGateRequirementRequest request,
                                                             String semanticDigest,
                                                             String evidenceBundleDigest) {
        ReconciliationGateRequirementHead winnerHead = requirementHeadMapper.selectForUpdate(
                request.getTenantId(), request.getStageRef().getStageKind(),
                request.getStageRef().getStageIdentity().getOwnerNamespace(),
                request.getStageRef().getStageIdentity().getValue());
        AssertUtils.notNull(winnerHead, "Gate Requirement 首次并发发布冲突后未找到 current head");
        AssertUtils.isTrue(Objects.equals(request.getRequirementVersion(), winnerHead.getCurrentRequirementVersion()),
                "Gate Requirement 首次并发发布版本冲突");
        ReconciliationGateRequirement winner = requirementMapper.selectByStageAndVersion(
                request.getTenantId(), request.getStageRef().getStageKind(),
                request.getStageRef().getStageIdentity().getOwnerNamespace(),
                request.getStageRef().getStageIdentity().getValue(), winnerHead.getCurrentRequirementVersion());
        AssertUtils.isTrue(matchesHead(winner, winnerHead),
                "Gate Requirement 首次并发发布冲突后 current head 不完整");
        return replay(winner, semanticDigest, evidenceBundleDigest);
    }

    private ReconciliationGateRequirement toRequirement(RecordReconciliationGateRequirementRequest request,
                                                        ReconciliationGateRequirementHead head,
                                                        String semanticDigest,
                                                        List<String> evidenceRefs,
                                                        String evidenceBundleDigest,
                                                        WindOperator operator) {
        String identityValue = FundsStableHashSupport.sha256Json(Map.of(
                "stage", stageMap(request.getStageRef()), "version", request.getRequirementVersion()));
        ReconciliationGateRequirement result = new ReconciliationGateRequirement();
        result.setTenantId(request.getTenantId());
        result.setStageKind(request.getStageRef().getStageKind());
        result.setStageIdentityOwnerNamespace(request.getStageRef().getStageIdentity().getOwnerNamespace());
        result.setStageIdentityValue(request.getStageRef().getStageIdentity().getValue());
        result.setRequirementIdentityOwnerNamespace(REQUIREMENT_IDENTITY_NAMESPACE);
        result.setRequirementIdentityValue(identityValue);
        result.setRequirementVersion(request.getRequirementVersion());
        result.setSemanticDigest(semanticDigest);
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        result.setEvidenceBundleDigest(evidenceBundleDigest);
        if (head != null) {
            result.setPreviousRequirementIdentityOwnerNamespace(head.getCurrentRequirementIdentityOwnerNamespace());
            result.setPreviousRequirementIdentityValue(head.getCurrentRequirementIdentityValue());
            result.setPreviousRequirementVersion(head.getCurrentRequirementVersion());
            result.setPreviousSemanticDigest(head.getCurrentSemanticDigest());
            result.setPreviousEvidenceBundleDigest(head.getCurrentEvidenceBundleDigest());
        }
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private void insertPairs(ReconciliationGateRequirement requirement, List<RequiredPairRef> pairs) {
        for (RequiredPairRef pair : pairs) {
            ReconciliationGateRequirementPair item = new ReconciliationGateRequirementPair();
            item.setTenantId(requirement.getTenantId());
            item.setRequirementIdentityOwnerNamespace(requirement.getRequirementIdentityOwnerNamespace());
            item.setRequirementIdentityValue(requirement.getRequirementIdentityValue());
            item.setScopeOwnerNamespace(pair.getScopeIdentity().getOwnerNamespace());
            item.setScopeIdentityValue(pair.getScopeIdentity().getValue());
            item.setPairOwnerNamespace(pair.getPairIdentity().getOwnerNamespace());
            item.setPairIdentityValue(pair.getPairIdentity().getValue());
            item.setRuleNamespace(pair.getComparisonRuleRef().getNamespace());
            item.setRuleIdentity(pair.getComparisonRuleRef().getIdentity());
            item.setRuleVersion(pair.getComparisonRuleRef().getVersion());
            requirementPairMapper.insertSelective(item);
        }
    }

    private void publishHead(ReconciliationGateRequirement requirement, ReconciliationGateRequirementHead head) {
        if (head == null) {
            ReconciliationGateRequirementHead created = new ReconciliationGateRequirementHead();
            created.setTenantId(requirement.getTenantId());
            created.setStageKind(requirement.getStageKind());
            created.setStageIdentityOwnerNamespace(requirement.getStageIdentityOwnerNamespace());
            created.setStageIdentityValue(requirement.getStageIdentityValue());
            created.setCurrentRequirementIdentityOwnerNamespace(requirement.getRequirementIdentityOwnerNamespace());
            created.setCurrentRequirementIdentityValue(requirement.getRequirementIdentityValue());
            created.setCurrentRequirementVersion(requirement.getRequirementVersion());
            created.setCurrentSemanticDigest(requirement.getSemanticDigest());
            created.setCurrentEvidenceBundleDigest(requirement.getEvidenceBundleDigest());
            created.setVersion(0);
            requirementHeadMapper.insertSelective(created);
            return;
        }
        AssertUtils.isTrue(requirementHeadMapper.advance(requirement.getTenantId(), requirement.getStageKind(),
                        requirement.getStageIdentityOwnerNamespace(), requirement.getStageIdentityValue(),
                        head.getCurrentRequirementIdentityOwnerNamespace(), head.getCurrentRequirementIdentityValue(),
                        head.getCurrentRequirementVersion(), head.getCurrentSemanticDigest(),
                        head.getCurrentEvidenceBundleDigest(), requirement.getRequirementIdentityOwnerNamespace(),
                        requirement.getRequirementIdentityValue(), requirement.getRequirementVersion(),
                        requirement.getSemanticDigest(), requirement.getEvidenceBundleDigest()) == 1,
                "Gate Requirement current head CAS 冲突");
    }

    private ReconciliationGateDecisionDTO blockedWithoutRequirement(GateStageRef stageRef, WindOperator operator) {
        return blocked(stageRef, operator, ReconciliationGateBlockerCode.REQUIREMENT_NOT_FOUND,
                "未找到当前 Gate Requirement，准入必须阻断");
    }

    private ReconciliationGateDecisionDTO blockedForHead(GateStageRef stageRef, WindOperator operator) {
        return blocked(stageRef, operator, ReconciliationGateBlockerCode.REQUIREMENT_HEAD_CONFLICT,
                "Gate Requirement current head 不完整或冲突，准入必须阻断");
    }

    private ReconciliationGateDecisionDTO blocked(GateStageRef stageRef,
                                                  WindOperator operator,
                                                  ReconciliationGateBlockerCode blocker,
                                                  String explanation) {
        return new ReconciliationGateDecisionDTO()
                .setPassed(false)
                .setDecisionResult(ReconciliationGateDecisionResult.BLOCKED)
                .setStageRef(stageRef)
                .setPairDecisions(List.of(new ReconciliationGatePairDecisionDTO()
                        .setBlockerCodes(List.of(blocker)).setEvidenceRefs(List.of())))
                .setEvidenceRefs(List.of())
                .setExplanation(explanation)
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operator.getOperatorAsText());
    }

    private boolean matchesHead(ReconciliationGateRequirement requirement, ReconciliationGateRequirementHead head) {
        return requirement != null
                && Objects.equals(requirement.getRequirementIdentityOwnerNamespace(),
                head.getCurrentRequirementIdentityOwnerNamespace())
                && Objects.equals(requirement.getRequirementIdentityValue(), head.getCurrentRequirementIdentityValue())
                && Objects.equals(requirement.getRequirementVersion(), head.getCurrentRequirementVersion())
                && Objects.equals(requirement.getSemanticDigest(), head.getCurrentSemanticDigest())
                && Objects.equals(requirement.getEvidenceBundleDigest(), head.getCurrentEvidenceBundleDigest());
    }

    private boolean matchesHead(GateRequirementRef expected, ReconciliationGateRequirementHead head) {
        return expected.getStageRef() != null
                && Objects.equals(expected.getStageRef().getStageKind(), head.getStageKind())
                && Objects.equals(expected.getStageRef().getStageIdentity().getOwnerNamespace(),
                head.getStageIdentityOwnerNamespace())
                && Objects.equals(expected.getStageRef().getStageIdentity().getValue(), head.getStageIdentityValue())
                && Objects.equals(expected.getRequirementIdentity().getOwnerNamespace(),
                head.getCurrentRequirementIdentityOwnerNamespace())
                && Objects.equals(expected.getRequirementIdentity().getValue(), head.getCurrentRequirementIdentityValue())
                && Objects.equals(expected.getRequirementVersion(), head.getCurrentRequirementVersion())
                && Objects.equals(expected.getSemanticDigest(), head.getCurrentSemanticDigest())
                && Objects.equals(expected.getEvidenceBundleDigest(), head.getCurrentEvidenceBundleDigest());
    }

    private boolean samePair(ReconciliationBatch batch, ReconciliationGateRequirementPair pair) {
        return Objects.equals(batch.getScopeOwnerNamespace(), pair.getScopeOwnerNamespace())
                && Objects.equals(batch.getScopeIdentityValue(), pair.getScopeIdentityValue())
                && Objects.equals(batch.getPairOwnerNamespace(), pair.getPairOwnerNamespace())
                && Objects.equals(batch.getPairIdentityValue(), pair.getPairIdentityValue());
    }

    private boolean samePair(ReconciliationRunResult run, ReconciliationGateRequirementPair pair) {
        return Objects.equals(run.getScopeOwnerNamespace(), pair.getScopeOwnerNamespace())
                && Objects.equals(run.getScopeIdentityValue(), pair.getScopeIdentityValue())
                && Objects.equals(run.getPairOwnerNamespace(), pair.getPairOwnerNamespace())
                && Objects.equals(run.getPairIdentityValue(), pair.getPairIdentityValue());
    }

    private boolean sameRule(ReconciliationRunResult run, ReconciliationGateRequirementPair pair) {
        return Objects.equals(run.getRuleNamespace(), pair.getRuleNamespace())
                && Objects.equals(run.getRuleIdentity(), pair.getRuleIdentity())
                && Objects.equals(run.getRuleVersion(), pair.getRuleVersion());
    }

    private GateRequirementRef toRequirementRef(ReconciliationGateRequirement requirement) {
        return new GateRequirementRef()
                .setStageRef(new GateStageRef().setStageKind(requirement.getStageKind())
                        .setStageIdentity(new StableIdentity()
                                .setOwnerNamespace(requirement.getStageIdentityOwnerNamespace())
                                .setValue(requirement.getStageIdentityValue())))
                .setRequirementIdentity(new StableIdentity()
                        .setOwnerNamespace(requirement.getRequirementIdentityOwnerNamespace())
                        .setValue(requirement.getRequirementIdentityValue()))
                .setRequirementVersion(requirement.getRequirementVersion())
                .setSemanticDigest(requirement.getSemanticDigest())
                .setEvidenceBundleDigest(requirement.getEvidenceBundleDigest());
    }

    private RequiredPairRef toRequiredPairRef(ReconciliationGateRequirementPair pair) {
        return new RequiredPairRef()
                .setScopeIdentity(new StableIdentity().setOwnerNamespace(pair.getScopeOwnerNamespace())
                        .setValue(pair.getScopeIdentityValue()))
                .setPairIdentity(new StableIdentity().setOwnerNamespace(pair.getPairOwnerNamespace())
                        .setValue(pair.getPairIdentityValue()))
                .setComparisonRuleRef(new ComparisonRuleRef().setNamespace(pair.getRuleNamespace())
                        .setIdentity(pair.getRuleIdentity()).setVersion(pair.getRuleVersion()));
    }

    private String requirementSemanticDigest(RecordReconciliationGateRequirementRequest request,
                                             List<RequiredPairRef> pairs) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", request.getTenantId());
        facts.put("stage", stageMap(request.getStageRef()));
        facts.put("requirementVersion", request.getRequirementVersion());
        facts.put("requiredPairs", pairs.stream().map(this::pairMap).toList());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private String decisionDigest(GateStageRef stageRef,
                                  GateRequirementRef requirement,
                                  List<ReconciliationGatePairDecisionDTO> pairs,
                                  boolean passed) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("stage", stageMap(stageRef));
        facts.put("requirementIdentity", identityMap(requirement.getRequirementIdentity()));
        facts.put("requirementVersion", requirement.getRequirementVersion());
        facts.put("requirementSemanticDigest", requirement.getSemanticDigest());
        facts.put("requirementEvidenceBundleDigest", requirement.getEvidenceBundleDigest());
        facts.put("pairDecisions", pairs.stream().map(this::pairDecisionMap).toList());
        facts.put("passed", passed);
        return FundsStableHashSupport.sha256Json(facts);
    }

    private Map<String, Object> stageMap(GateStageRef stageRef) {
        TreeMap<String, Object> result = new TreeMap<>();
        result.put("stageKind", stageRef.getStageKind());
        result.put("stageIdentity", identityMap(stageRef.getStageIdentity()));
        return result;
    }

    private Map<String, Object> pairMap(RequiredPairRef pair) {
        TreeMap<String, Object> result = new TreeMap<>();
        result.put("scopeIdentity", identityMap(pair.getScopeIdentity()));
        result.put("pairIdentity", identityMap(pair.getPairIdentity()));
        result.put("ruleNamespace", pair.getComparisonRuleRef().getNamespace());
        result.put("ruleIdentity", pair.getComparisonRuleRef().getIdentity());
        result.put("ruleVersion", pair.getComparisonRuleRef().getVersion());
        return result;
    }

    private Map<String, Object> pairDecisionMap(ReconciliationGatePairDecisionDTO pair) {
        TreeMap<String, Object> result = new TreeMap<>();
        result.put("requiredPair", pairMap(pair.getRequiredPairRef()));
        result.put("currentBatchSn", pair.getCurrentBatchSn());
        result.put("currentRunResultSn", pair.getCurrentRunResultSn());
        result.put("currentLineageRef", pair.getCurrentLineageRef());
        result.put("resultDigest", pair.getResultDigest());
        result.put("outcome", pair.getOutcome());
        result.put("blockers", pair.getBlockerCodes().stream().map(Enum::name).toList());
        result.put("evidenceRefs", pair.getEvidenceRefs());
        return result;
    }

    private Map<String, Object> identityMap(StableIdentity identity) {
        return Map.of("ownerNamespace", identity.getOwnerNamespace(), "value", identity.getValue());
    }

    private String pairKey(RequiredPairRef pair) {
        return pair.getScopeIdentity().getOwnerNamespace() + ':' + pair.getScopeIdentity().getValue() + '|'
                + pair.getPairIdentity().getOwnerNamespace() + ':' + pair.getPairIdentity().getValue();
    }

    private List<String> decisionEvidence(ReconciliationGateRequirement requirement,
                                          List<ReconciliationGatePairDecisionDTO> pairDecisions) {
        LinkedHashSet<String> result = new LinkedHashSet<>(parseEvidenceRefs(requirement.getEvidenceRefs()));
        pairDecisions.forEach(pair -> result.addAll(pair.getEvidenceRefs()));
        return List.copyOf(result);
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    private List<String> withFirst(String first, List<String> remaining) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(first);
        result.addAll(remaining);
        return List.copyOf(result);
    }
}
