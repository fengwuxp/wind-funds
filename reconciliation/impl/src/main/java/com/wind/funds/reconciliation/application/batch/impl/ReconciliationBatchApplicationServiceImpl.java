package com.wind.funds.reconciliation.application.batch.impl;

import com.wind.jackson.WindJson;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceItem;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchLineageMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceItemMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceSnapshotMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchState;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.AbortReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.NormalizedComparisonFactInput;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.model.request.ReplaceReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.SnapshotCoverage;
import com.wind.funds.reconciliation.model.value.StableIdentity;
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
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 对账批次应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationBatchApplicationServiceImpl implements ReconciliationBatchApplicationService {

    private static final WindSequenceType BATCH_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_BATCH", "RCB", 6);

    private static final WindSequenceType SOURCE_SNAPSHOT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_SOURCE_SNAPSHOT", "RSS", 6);

    private static final WindSequenceType SOURCE_ITEM_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_SOURCE_ITEM", "RSI", 6);

    private final ReconciliationBatchMapper reconciliationBatchMapper;

    private final ReconciliationBatchLineageMapper reconciliationBatchLineageMapper;

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationSourceSnapshotMapper reconciliationSourceSnapshotMapper;

    private final ReconciliationSourceItemMapper reconciliationSourceItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO createBatch(CreateReconciliationBatchRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        ReconciliationBatch candidate = toBatch(request, operator);
        ReconciliationBatch existing = reconciliationBatchMapper.selectByDigest(
                candidate.getTenantId(), candidate.getBatchDigest());
        if (existing != null) {
            return toBatchDTO(existing);
        }
        ReconciliationBatch previous = validatePreviousBatch(candidate);
        ReconciliationBatchLineage lineage = selectLineageForUpdate(candidate);
        if (previous == null) {
            if (lineage != null) {
                ReconciliationBatch current = reconciliationBatchMapper.selectBySn(
                        candidate.getTenantId(), lineage.getCurrentBatchSn());
                AssertUtils.isTrue(current != null && Objects.equals(current.getBatchDigest(), candidate.getBatchDigest()),
                        "同一对账 scope/pair 已存在 current batch，currentBatchSn = {}", lineage.getCurrentBatchSn());
                return toBatchDTO(current);
            }
            try {
                reconciliationBatchLineageMapper.insertSelective(toLineage(candidate));
            } catch (DuplicateKeyException exception) {
                ReconciliationBatchLineage winnerLineage = selectLineageForUpdate(candidate);
                AssertUtils.notNull(winnerLineage, "并发创建对账 current lineage 后无法读取赢家");
                ReconciliationBatch winner = reconciliationBatchMapper.selectBySn(
                        candidate.getTenantId(), winnerLineage.getCurrentBatchSn());
                AssertUtils.isTrue(winner != null
                                && Objects.equals(winner.getBatchDigest(), candidate.getBatchDigest()),
                        "同一对账 scope/pair 已存在 current batch，currentBatchSn = {}",
                        winnerLineage.getCurrentBatchSn());
                return toBatchDTO(winner);
            }
            reconciliationBatchMapper.insertSelective(candidate);
            AssertUtils.notNull(candidate.getId(), "创建对账批次失败");
            ReconciliationBatchDTO result = toBatchDTO(
                    reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn()));
            logBatch("create", result);
            return result;
        }

        AssertUtils.notNull(lineage, "对账重跑缺少 current lineage，previousBatchSn = {}", previous.getSn());
        if (!Objects.equals(lineage.getCurrentBatchSn(), previous.getSn())) {
            ReconciliationBatch rerun = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                    candidate.getTenantId(), previous.getSn());
            AssertUtils.notNull(rerun,
                    "上一对账批次不是 current lineage，previousBatchSn = {}, currentBatchSn = {}",
                    previous.getSn(), lineage.getCurrentBatchSn());
            AssertUtils.isTrue(Objects.equals(rerun.getBatchDigest(), candidate.getBatchDigest()),
                    "同一上一批次只允许一个直接重跑，previousBatchSn = {}", previous.getSn());
            return toBatchDTO(rerun);
        }
        assertPreviousDifferencesReady(previous);
        ReconciliationBatch rerun = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                candidate.getTenantId(), previous.getSn());
        if (rerun != null) {
            AssertUtils.isTrue(Objects.equals(rerun.getBatchDigest(), candidate.getBatchDigest()),
                    "同一上一批次只允许一个直接重跑，previousBatchSn = {}", previous.getSn());
            return toBatchDTO(rerun);
        }
        try {
            reconciliationBatchMapper.insertSelective(candidate);
            AssertUtils.isTrue(reconciliationBatchLineageMapper.advance(
                            candidate.getTenantId(), candidate.getScopeOwnerNamespace(),
                            candidate.getScopeIdentityValue(), candidate.getPairOwnerNamespace(),
                            candidate.getPairIdentityValue(), candidate.getPreviousBatchSn(), candidate.getSn()) == 1,
                    "推进对账 current lineage 失败，previousBatchSn = {}", candidate.getPreviousBatchSn());
        } catch (DuplicateKeyException exception) {
            ReconciliationBatch winner = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                    candidate.getTenantId(), previous.getSn());
            if (winner != null) {
                AssertUtils.isTrue(Objects.equals(winner.getBatchDigest(), candidate.getBatchDigest()),
                        "同一上一批次只允许一个直接重跑，previousBatchSn = {}", previous.getSn());
                return toBatchDTO(winner);
            }
            throw exception;
        }
        AssertUtils.notNull(candidate.getId(), "创建对账批次失败");
        ReconciliationBatchDTO result = toBatchDTO(
                reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn()));
        logBatch("rerun", result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO abortBatch(AbortReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "终止对账批次请求不能为空");
        validateTenant(request.getTenantId());
        AssertUtils.hasText(request.getReconciliationBatchSn(), "终止对账批次流水号不能为空");
        AssertUtils.hasText(request.getReason(), "终止对账批次原因不能为空");
        AssertUtils.notNull(operator, "终止对账批次操作人不能为空");
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn().trim());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        String abortReason = request.getReason().trim();
        String abortedBy = operator.getOperatorAsText();
        if (batch.getState() == ReconciliationBatchState.ABORTED) {
            AssertUtils.isTrue(Objects.equals(batch.getAbortReason(), abortReason)
                            && Objects.equals(batch.getAbortedBy(), abortedBy),
                    "对账批次已由不同终止事实关闭，reconciliationBatchSn = {}", batch.getSn());
            return toBatchDTO(batch);
        }
        ReconciliationBatchLineage lineage = selectLineageForUpdate(batch);
        AssertUtils.isTrue(lineage != null && Objects.equals(lineage.getCurrentBatchSn(), batch.getSn()),
                "只有当前批次血缘头可以终止，reconciliationBatchSn = {}", batch.getSn());
        AssertUtils.isTrue(batch.getState() != ReconciliationBatchState.COMPLETED,
                "已完成对账批次不能终止，请使用 replaceBatch 创建替代批次，reconciliationBatchSn = {}", batch.getSn());
        AssertUtils.isTrue(reconciliationBatchMapper.abort(batch.getTenantId(), batch.getSn(), batch.getState().name(),
                        abortedBy, LocalDateTime.now(), abortReason) == 1,
                "终止对账批次失败，reconciliationBatchSn = {}", batch.getSn());
        ReconciliationBatchDTO result = toBatchDTO(
                reconciliationBatchMapper.selectBySn(batch.getTenantId(), batch.getSn()));
        logBatch("abort", result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO replaceBatch(ReplaceReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "替代对账批次请求不能为空");
        validateTenant(request.getTenantId());
        AssertUtils.hasText(request.getReconciliationBatchSn(), "被替代对账批次流水号不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "替代对账批次规则版本不能为空");
        AssertUtils.hasText(request.getReason(), "替代对账批次原因不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "替代对账批次证据引用不能为空");
        AssertUtils.notNull(operator, "替代对账批次操作人不能为空");
        ReconciliationBatch replaced = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn().trim());
        AssertUtils.notNull(replaced, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        AssertUtils.isTrue(replaced.getState() == ReconciliationBatchState.COMPLETED,
                "只有已完成对账批次可以替代，reconciliationBatchSn = {}", replaced.getSn());
        ReconciliationBatch candidate = copyReplacement(replaced, request, operator);
        ReconciliationBatch existing = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                request.getTenantId(), replaced.getSn());
        if (existing != null) {
            AssertUtils.isTrue(Objects.equals(existing.getBatchDigest(), candidate.getBatchDigest()),
                    "对账批次已由不同替代事实处理，reconciliationBatchSn = {}", replaced.getSn());
            return toBatchDTO(existing);
        }
        reconciliationBatchMapper.insertSelective(candidate);
        AssertUtils.isTrue(reconciliationBatchLineageMapper.advance(
                        candidate.getTenantId(), candidate.getScopeOwnerNamespace(), candidate.getScopeIdentityValue(),
                        candidate.getPairOwnerNamespace(), candidate.getPairIdentityValue(), replaced.getSn(), candidate.getSn()) == 1,
                "推进替代对账 current lineage 失败，reconciliationBatchSn = {}", replaced.getSn());
        reconciliationDifferenceMapper.invalidateByCurrentBatch(candidate.getTenantId(), replaced.getSn());
        ReconciliationBatchDTO result = toBatchDTO(
                reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn()));
        logBatch("replace", result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationSourceSnapshotDTO recordSourceSnapshot(
            RecordReconciliationSourceSnapshotRequest request,
            WindOperator operator) {
        validateSourceRequest(request, operator);
        List<NormalizedComparisonFactInput> facts = request.getFacts().stream()
                .map(this::normalizedFact)
                .sorted((left, right) -> stableIdentityKey(left.getSourceFactRef())
                        .compareTo(stableIdentityKey(right.getSourceFactRef())))
                .toList();
        AssertUtils.isTrue(facts.stream().map(fact -> stableIdentityKey(fact.getSourceFactRef())).distinct().count()
                        == facts.size(),
                "同一来源快照的 source fact identity 不能重复");
        List<String> evidenceRefs = normalizedEvidenceRefs(request.getEvidenceRefs());
        String semanticDigest = FundsStableHashSupport.sha256Json(facts.stream().map(this::semanticFact).toList());
        String evidenceBundleDigest = FundsStableHashSupport.sha256Json(evidenceRefs);
        String sourceDigest = snapshotDigest(request, semanticDigest);
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn().trim());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        ReconciliationSourceSnapshot existing = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                request.getTenantId(), batch.getSn(), request.getSourceRole().name());
        if (existing != null) {
            AssertUtils.isTrue(Objects.equals(existing.getSourceDigest(), sourceDigest)
                            && Objects.equals(existing.getEvidenceBundleDigest(), evidenceBundleDigest),
                    "同一批次和来源角色的快照事实冲突，reconciliationBatchSn = {}, sourceRole = {}",
                    batch.getSn(), request.getSourceRole());
            return toSourceSnapshotDTO(existing);
        }
        AssertUtils.isTrue(batch.getState() == ReconciliationBatchState.CREATED
                        || batch.getState() == ReconciliationBatchState.DATA_COLLECTING,
                "当前对账批次状态不允许新增来源快照，reconciliationBatchSn = {}, status = {}",
                batch.getSn(), batch.getState());
        ReconciliationSourceSnapshot snapshot = toSnapshot(
                request, operator, sourceDigest, semanticDigest, evidenceBundleDigest, evidenceRefs);
        reconciliationSourceSnapshotMapper.insertSelective(snapshot);
        facts.forEach(fact -> persistFact(request.getTenantId(), snapshot.getSn(), operator, fact));
        advanceBatchSourceState(batch);
        ReconciliationSourceSnapshotDTO result = toSourceSnapshotDTO(
                reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                        request.getTenantId(), batch.getSn(), request.getSourceRole().name()));
        log.info("对账来源快照记录完成，等待事务提交，tenantId={}, batchSn={}, snapshotSn={}, sourceRole={}, "
                        + "sourceNamespace={}, factCount={}",
                request.getTenantId(), batch.getSn(), result.getSn(), result.getSourceRole(),
                result.getSourceNamespace(), facts.size());
        return result;
    }

    private void logBatch(String action, ReconciliationBatchDTO batch) {
        log.info("对账批次处理完成，等待事务提交，action={}, tenantId={}, batchSn={}, previousBatchSn={}, "
                        + "state={}, currency={}",
                action, batch.getTenantId(), batch.getSn(), batch.getPreviousBatchSn(), batch.getState(),
                batch.getCurrency());
    }

    private void validateCreateRequest(CreateReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建对账批次请求不能为空");
        validateTenant(request.getTenantId());
        validateIdentity(request.getScopeIdentity(), "scopeIdentity");
        validateIdentity(request.getPairIdentity(), "pairIdentity");
        AssertUtils.notNull(request.getCurrency(), "对账币种不能为空");
        validateRule(request.getComparisonRuleRef());
        AssertUtils.notNull(request.getWindowStart(), "对账窗口开始时间不能为空");
        AssertUtils.notNull(request.getWindowEnd(), "对账窗口结束时间不能为空");
        AssertUtils.isTrue(request.getWindowStart().isBefore(request.getWindowEnd()), "对账窗口开始时间必须早于结束时间");
        AssertUtils.hasText(request.getTimeSemantics(), "对账窗口时间语义不能为空");
        AssertUtils.hasText(request.getTimezoneId(), "对账窗口时区不能为空");
        AssertUtils.isTrue(isValidZoneId(request.getTimezoneId()), "对账窗口时区无效，timezoneId = {}", request.getTimezoneId());
        AssertUtils.notNull(operator, "创建对账批次操作人不能为空");
    }

    private void validateSourceRequest(RecordReconciliationSourceSnapshotRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "记录对账来源快照请求不能为空");
        validateTenant(request.getTenantId());
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账批次流水号不能为空");
        AssertUtils.notNull(request.getSourceRole(), "来源角色不能为空");
        AssertUtils.hasText(request.getSourceNamespace(), "逻辑来源命名空间不能为空");
        validateIdentity(request.getSnapshotIdentity(), "snapshotIdentity");
        AssertUtils.hasText(request.getSnapshotVersion(), "来源快照版本不能为空");
        AssertUtils.notNull(request.getCoverage(), "来源覆盖范围不能为空");
        AssertUtils.notNull(request.getCoverage().getComplete(), "来源覆盖完整标记不能为空");
        AssertUtils.notNull(request.getCoverage().getMemberCount(), "来源覆盖成员数不能为空");
        AssertUtils.notNull(request.getFacts(), "归一化比较事实不能为空");
        AssertUtils.isTrue(request.getFacts().size() <= RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT,
                "归一化比较事实数量不能超过 {}", RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT);
        AssertUtils.isTrue(request.getFacts().size() == request.getCoverage().getMemberCount(),
                "来源 coverage memberCount 必须与事实数一致");
        AssertUtils.notNull(request.getEvidenceRefs(), "来源证据引用不能为空");
        AssertUtils.notNull(operator, "记录对账来源快照操作人不能为空");
    }

    private void validateTenant(Long tenantId) {
        AssertUtils.notNull(tenantId, "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId, "tenantId 与当前租户不一致");
    }

    private void validateIdentity(StableIdentity identity, String fieldName) {
        AssertUtils.notNull(identity, "{} 不能为空", fieldName);
        AssertUtils.hasText(identity.getOwnerNamespace(), "{}.ownerNamespace 不能为空", fieldName);
        AssertUtils.isTrue(identity.getOwnerNamespace().trim().length() <= 64,
                "{}.ownerNamespace 长度不能超过 64", fieldName);
        AssertUtils.hasText(identity.getValue(), "{}.value 不能为空", fieldName);
        AssertUtils.isTrue(identity.getValue().trim().length() <= 128,
                "{}.value 长度不能超过 128", fieldName);
    }

    private void validateRule(ComparisonRuleRef ruleRef) {
        AssertUtils.notNull(ruleRef, "comparisonRuleRef 不能为空");
        AssertUtils.hasText(ruleRef.getNamespace(), "comparisonRuleRef.namespace 不能为空");
        AssertUtils.hasText(ruleRef.getIdentity(), "comparisonRuleRef.identity 不能为空");
        AssertUtils.hasText(ruleRef.getVersion(), "comparisonRuleRef.version 不能为空");
    }

    private ReconciliationBatch toBatch(CreateReconciliationBatchRequest request, WindOperator operator) {
        ReconciliationBatch result = new ReconciliationBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        setScopeAndRule(result, request.getScopeIdentity(), request.getPairIdentity(), request.getComparisonRuleRef());
        result.setCurrency(request.getCurrency());
        result.setWindowStart(request.getWindowStart());
        result.setWindowEnd(request.getWindowEnd());
        result.setTimeSemantics(request.getTimeSemantics().trim());
        result.setTimezoneId(request.getTimezoneId().trim());
        result.setPreviousBatchSn(normalizedOptionalText(request.getPreviousBatchSn()));
        result.setState(ReconciliationBatchState.CREATED);
        result.setCreatedBy(operator.getOperatorAsText());
        result.setBatchDigest(batchDigest(result));
        return result;
    }

    private ReconciliationBatch copyReplacement(ReconciliationBatch source,
                                                 ReplaceReconciliationBatchRequest request,
                                                 WindOperator operator) {
        ReconciliationBatch result = new ReconciliationBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(source.getTenantId());
        result.setScopeOwnerNamespace(source.getScopeOwnerNamespace());
        result.setScopeIdentityValue(source.getScopeIdentityValue());
        result.setPairOwnerNamespace(source.getPairOwnerNamespace());
        result.setPairIdentityValue(source.getPairIdentityValue());
        result.setCurrency(source.getCurrency());
        result.setRuleNamespace(source.getRuleNamespace());
        result.setRuleIdentity(source.getRuleIdentity());
        result.setRuleVersion(request.getRuleVersion().trim());
        result.setWindowStart(source.getWindowStart());
        result.setWindowEnd(source.getWindowEnd());
        result.setTimeSemantics(source.getTimeSemantics());
        result.setTimezoneId(source.getTimezoneId());
        result.setPreviousBatchSn(source.getSn());
        result.setState(ReconciliationBatchState.CREATED);
        result.setReplacementReason(request.getReason().trim());
        result.setReplacementEvidenceRef(request.getEvidenceRef().trim());
        result.setCreatedBy(operator.getOperatorAsText());
        result.setBatchDigest(batchDigest(result));
        return result;
    }

    private void setScopeAndRule(ReconciliationBatch target,
                                 StableIdentity scope,
                                 StableIdentity pair,
                                 ComparisonRuleRef rule) {
        target.setScopeOwnerNamespace(scope.getOwnerNamespace().trim());
        target.setScopeIdentityValue(scope.getValue().trim());
        target.setPairOwnerNamespace(pair.getOwnerNamespace().trim());
        target.setPairIdentityValue(pair.getValue().trim());
        target.setRuleNamespace(rule.getNamespace().trim());
        target.setRuleIdentity(rule.getIdentity().trim());
        target.setRuleVersion(rule.getVersion().trim());
    }

    private ReconciliationBatch validatePreviousBatch(ReconciliationBatch candidate) {
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return null;
        }
        ReconciliationBatch previous = reconciliationBatchMapper.selectBySnForUpdate(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
        AssertUtils.notNull(previous, "上一对账批次不存在，previousBatchSn = {}", candidate.getPreviousBatchSn());
        AssertUtils.isTrue(previous.getState() == ReconciliationBatchState.COMPLETED
                        || previous.getState() == ReconciliationBatchState.ABORTED,
                "只有已完成或已终止对账批次可以重跑，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(sameScope(previous, candidate), "重跑批次 scope/pair/currency/window 必须保持一致");
        return previous;
    }

    private boolean sameScope(ReconciliationBatch left, ReconciliationBatch right) {
        return Objects.equals(left.getScopeOwnerNamespace(), right.getScopeOwnerNamespace())
                && Objects.equals(left.getScopeIdentityValue(), right.getScopeIdentityValue())
                && Objects.equals(left.getPairOwnerNamespace(), right.getPairOwnerNamespace())
                && Objects.equals(left.getPairIdentityValue(), right.getPairIdentityValue())
                && left.getCurrency() == right.getCurrency()
                && Objects.equals(left.getWindowStart(), right.getWindowStart())
                && Objects.equals(left.getWindowEnd(), right.getWindowEnd())
                && Objects.equals(left.getTimeSemantics(), right.getTimeSemantics())
                && Objects.equals(left.getTimezoneId(), right.getTimezoneId());
    }

    private ReconciliationBatchLineage selectLineageForUpdate(ReconciliationBatch batch) {
        return reconciliationBatchLineageMapper.selectForUpdate(
                batch.getTenantId(), batch.getScopeOwnerNamespace(), batch.getScopeIdentityValue(),
                batch.getPairOwnerNamespace(), batch.getPairIdentityValue());
    }

    private ReconciliationBatchLineage toLineage(ReconciliationBatch batch) {
        ReconciliationBatchLineage result = new ReconciliationBatchLineage();
        result.setTenantId(batch.getTenantId());
        result.setScopeOwnerNamespace(batch.getScopeOwnerNamespace());
        result.setScopeIdentityValue(batch.getScopeIdentityValue());
        result.setPairOwnerNamespace(batch.getPairOwnerNamespace());
        result.setPairIdentityValue(batch.getPairIdentityValue());
        result.setCurrentBatchSn(batch.getSn());
        return result;
    }

    private void assertPreviousDifferencesReady(ReconciliationBatch previous) {
        if (previous.getState() != ReconciliationBatchState.COMPLETED) {
            return;
        }
        AssertUtils.hasText(previous.getRunResultSn(),
                "上一对账批次缺少完成运行结果，previousBatchSn = {}", previous.getSn());
        ReconciliationRunResult runResult = reconciliationRunResultMapper.selectBySn(
                previous.getTenantId(), previous.getRunResultSn());
        AssertUtils.isTrue(runResult != null
                        && Objects.equals(runResult.getReconciliationBatchSn(), previous.getSn()),
                "上一对账批次运行结果不存在或归属不一致，previousBatchSn = {}, runResultSn = {}",
                previous.getSn(), previous.getRunResultSn());
        int materializedDifferenceCount = reconciliationDifferenceMapper.countByCurrentBatch(
                previous.getTenantId(), previous.getSn());
        AssertUtils.isTrue(Objects.equals(runResult.getDifferenceCount(), materializedDifferenceCount),
                "对账差异尚未全部物化，previousBatchSn = {}, expected = {}, actual = {}",
                previous.getSn(), runResult.getDifferenceCount(), materializedDifferenceCount);
        Long unreadyDifferenceId = reconciliationDifferenceMapper.selectFirstUnreadyForRerunIdForUpdate(
                previous.getTenantId(), previous.getSn());
        AssertUtils.isTrue(unreadyDifferenceId == null,
                "对账差错必须先完成处理动作再发起重跑，previousBatchSn = {}, differenceId = {}",
                previous.getSn(), unreadyDifferenceId);
    }

    private String batchDigest(ReconciliationBatch batch) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", batch.getTenantId());
        facts.put("scope", batch.getScopeOwnerNamespace() + ":" + batch.getScopeIdentityValue());
        facts.put("pair", batch.getPairOwnerNamespace() + ":" + batch.getPairIdentityValue());
        facts.put("currency", batch.getCurrency());
        facts.put("rule", batch.getRuleNamespace() + ":" + batch.getRuleIdentity() + ":" + batch.getRuleVersion());
        facts.put("windowStart", batch.getWindowStart());
        facts.put("windowEnd", batch.getWindowEnd());
        facts.put("timeSemantics", batch.getTimeSemantics());
        facts.put("timezoneId", batch.getTimezoneId());
        facts.put("previousBatchSn", batch.getPreviousBatchSn());
        facts.put("replacementReason", batch.getReplacementReason());
        facts.put("replacementEvidenceRef", batch.getReplacementEvidenceRef());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private ReconciliationSourceSnapshot toSnapshot(RecordReconciliationSourceSnapshotRequest request,
                                                      WindOperator operator,
                                                      String sourceDigest,
                                                      String semanticDigest,
                                                      String evidenceBundleDigest,
                                                      List<String> evidenceRefs) {
        ReconciliationSourceSnapshot result = new ReconciliationSourceSnapshot();
        result.setSn(TemporalSequenceFactory.hourNext(SOURCE_SNAPSHOT_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setReconciliationBatchSn(request.getReconciliationBatchSn().trim());
        result.setSourceRole(request.getSourceRole());
        result.setSourceNamespace(request.getSourceNamespace().trim());
        result.setSnapshotOwnerNamespace(request.getSnapshotIdentity().getOwnerNamespace().trim());
        result.setSnapshotIdentityValue(request.getSnapshotIdentity().getValue().trim());
        result.setSnapshotVersion(request.getSnapshotVersion().trim());
        result.setCoverageComplete(request.getCoverage().getComplete());
        result.setCoverageWatermark(normalizedOptionalText(request.getCoverage().getWatermark()));
        result.setCoverageMemberCount(request.getCoverage().getMemberCount());
        result.setSourceDigest(sourceDigest);
        result.setSemanticDigest(semanticDigest);
        result.setEvidenceBundleDigest(evidenceBundleDigest);
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private void persistFact(Long tenantId,
                             String snapshotSn,
                             WindOperator operator,
                             NormalizedComparisonFactInput fact) {
        ReconciliationSourceItem item = new ReconciliationSourceItem();
        item.setSn(TemporalSequenceFactory.hourNext(SOURCE_ITEM_SEQUENCE_TYPE));
        item.setTenantId(tenantId);
        item.setSourceSnapshotSn(snapshotSn);
        item.setSourceFactOwnerNamespace(fact.getSourceFactRef().getOwnerNamespace());
        item.setSourceFactIdentityValue(fact.getSourceFactRef().getValue());
        item.setComparisonOwnerNamespace(fact.getComparisonIdentity().getOwnerNamespace());
        item.setComparisonIdentityValue(fact.getComparisonIdentity().getValue());
        item.setAmount(fact.getAmount());
        item.setCurrency(fact.getCurrency());
        item.setRuleNamespace(fact.getComparisonRuleRef().getNamespace());
        item.setRuleIdentity(fact.getComparisonRuleRef().getIdentity());
        item.setRuleVersion(fact.getComparisonRuleRef().getVersion());
        item.setComparisonStatusCode(fact.getComparisonStatusCode());
        item.setComparisonProven(fact.getComparisonProven());
        item.setClaimKind(fact.getClaimKind());
        item.setEconomicComponent(fact.getEconomicComponent());
        item.setDirection(fact.getDirection());
        item.setNormalizationVersion(fact.getNormalizationVersion());
        item.setSemanticDigest(FundsStableHashSupport.sha256Json(semanticFact(fact)));
        List<String> evidenceRefs = normalizedEvidenceRefs(fact.getEvidenceRefs());
        item.setEvidenceBundleDigest(FundsStableHashSupport.sha256Json(evidenceRefs));
        item.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        item.setCreatedBy(operator.getOperatorAsText());
        reconciliationSourceItemMapper.insertSelective(item);
        AssertUtils.notNull(item.getId(), "记录归一化对账事实失败");
    }

    private void advanceBatchSourceState(ReconciliationBatch batch) {
        List<ReconciliationSourceSnapshot> snapshots = reconciliationSourceSnapshotMapper.selectByBatch(
                batch.getTenantId(), batch.getSn());
        if (snapshots.size() == 2) {
            AssertUtils.isTrue(snapshots.stream().mapToInt(ReconciliationSourceSnapshot::getCoverageMemberCount).sum() > 0,
                    "对账批次两侧来源不能同时为空，reconciliationBatchSn = {}", batch.getSn());
        }
        ReconciliationBatchState target = snapshots.size() == 2
                ? ReconciliationBatchState.DATA_READY : ReconciliationBatchState.DATA_COLLECTING;
        AssertUtils.isTrue(reconciliationBatchMapper.updateState(
                        batch.getTenantId(), batch.getSn(), batch.getState().name(), target.name()) == 1,
                "推进对账批次来源状态失败，reconciliationBatchSn = {}", batch.getSn());
    }

    private ReconciliationBatchDTO toBatchDTO(ReconciliationBatch source) {
        return new ReconciliationBatchDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setScopeIdentity(identity(source.getScopeOwnerNamespace(), source.getScopeIdentityValue()))
                .setPairIdentity(identity(source.getPairOwnerNamespace(), source.getPairIdentityValue()))
                .setCurrency(source.getCurrency())
                .setComparisonRuleRef(rule(source.getRuleNamespace(), source.getRuleIdentity(), source.getRuleVersion()))
                .setWindowStart(source.getWindowStart())
                .setWindowEnd(source.getWindowEnd())
                .setTimeSemantics(source.getTimeSemantics())
                .setTimezoneId(source.getTimezoneId())
                .setPreviousBatchSn(source.getPreviousBatchSn())
                .setState(source.getState())
                .setRunResultSn(source.getRunResultSn())
                .setAbortedBy(source.getAbortedBy())
                .setAbortedTime(source.getAbortedTime())
                .setAbortReason(source.getAbortReason())
                .setReplacementReason(source.getReplacementReason())
                .setReplacementEvidenceRef(source.getReplacementEvidenceRef())
                .setBatchDigest(source.getBatchDigest())
                .setCreatedBy(source.getCreatedBy())
                .setCreatedTime(source.getGmtCreate())
                .setModifiedTime(source.getGmtModified());
    }

    private ReconciliationSourceSnapshotDTO toSourceSnapshotDTO(ReconciliationSourceSnapshot source) {
        List<NormalizedComparisonFactInput> facts = reconciliationSourceItemMapper.selectBySnapshot(
                        source.getTenantId(), source.getSn()).stream()
                .map(this::toFactDTO)
                .toList();
        return new ReconciliationSourceSnapshotDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setReconciliationBatchSn(source.getReconciliationBatchSn())
                .setSourceRole(source.getSourceRole())
                .setSourceNamespace(source.getSourceNamespace())
                .setSnapshotIdentity(identity(source.getSnapshotOwnerNamespace(), source.getSnapshotIdentityValue()))
                .setSnapshotVersion(source.getSnapshotVersion())
                .setCoverage(new SnapshotCoverage()
                        .setComplete(source.getCoverageComplete())
                        .setWatermark(source.getCoverageWatermark())
                        .setMemberCount(source.getCoverageMemberCount()))
                .setFacts(facts)
                .setSourceDigest(source.getSourceDigest())
                .setSemanticDigest(source.getSemanticDigest())
                .setEvidenceBundleDigest(source.getEvidenceBundleDigest())
                .setEvidenceRefs(parseRefs(source.getEvidenceRefs()))
                .setCreatedBy(source.getCreatedBy())
                .setCreatedTime(source.getGmtCreate());
    }

    private NormalizedComparisonFactInput toFactDTO(ReconciliationSourceItem source) {
        return new NormalizedComparisonFactInput()
                .setSourceFactRef(identity(source.getSourceFactOwnerNamespace(), source.getSourceFactIdentityValue()))
                .setComparisonIdentity(identity(source.getComparisonOwnerNamespace(), source.getComparisonIdentityValue()))
                .setAmount(source.getAmount())
                .setCurrency(source.getCurrency())
                .setComparisonRuleRef(rule(source.getRuleNamespace(), source.getRuleIdentity(), source.getRuleVersion()))
                .setComparisonStatusCode(source.getComparisonStatusCode())
                .setComparisonProven(source.getComparisonProven())
                .setClaimKind(source.getClaimKind())
                .setEconomicComponent(source.getEconomicComponent())
                .setDirection(source.getDirection())
                .setNormalizationVersion(source.getNormalizationVersion())
                .setEvidenceRefs(parseRefs(source.getEvidenceRefs()));
    }

    private NormalizedComparisonFactInput normalizedFact(NormalizedComparisonFactInput source) {
        AssertUtils.notNull(source, "归一化比较事实不能为空");
        validateIdentity(source.getSourceFactRef(), "sourceFactRef");
        validateIdentity(source.getComparisonIdentity(), "comparisonIdentity");
        AssertUtils.notNull(source.getAmount(), "事实金额不能为空");
        AssertUtils.isTrue(source.getAmount() > 0, "事实金额必须大于零");
        AssertUtils.notNull(source.getCurrency(), "事实币种不能为空");
        validateRule(source.getComparisonRuleRef());
        AssertUtils.hasText(source.getComparisonStatusCode(), "comparisonStatusCode 不能为空");
        AssertUtils.notNull(source.getComparisonProven(), "comparisonProven 不能为空");
        AssertUtils.hasText(source.getClaimKind(), "claimKind 不能为空");
        AssertUtils.hasText(source.getEconomicComponent(), "economicComponent 不能为空");
        AssertUtils.hasText(source.getDirection(), "direction 不能为空");
        AssertUtils.hasText(source.getNormalizationVersion(), "normalizationVersion 不能为空");
        AssertUtils.notNull(source.getEvidenceRefs(), "事实 evidenceRefs 不能为空");
        return new NormalizedComparisonFactInput()
                .setSourceFactRef(identity(source.getSourceFactRef().getOwnerNamespace(), source.getSourceFactRef().getValue()))
                .setComparisonIdentity(identity(source.getComparisonIdentity().getOwnerNamespace(), source.getComparisonIdentity().getValue()))
                .setAmount(source.getAmount())
                .setCurrency(source.getCurrency())
                .setComparisonRuleRef(rule(source.getComparisonRuleRef().getNamespace(),
                        source.getComparisonRuleRef().getIdentity(), source.getComparisonRuleRef().getVersion()))
                .setComparisonStatusCode(source.getComparisonStatusCode().trim())
                .setComparisonProven(source.getComparisonProven())
                .setClaimKind(source.getClaimKind().trim())
                .setEconomicComponent(source.getEconomicComponent().trim())
                .setDirection(source.getDirection().trim())
                .setNormalizationVersion(source.getNormalizationVersion().trim())
                .setEvidenceRefs(normalizedEvidenceRefs(source.getEvidenceRefs()));
    }

    private TreeMap<String, Object> semanticFact(NormalizedComparisonFactInput fact) {
        TreeMap<String, Object> value = new TreeMap<>();
        value.put("sourceFactRef", stableIdentityKey(fact.getSourceFactRef()));
        value.put("comparisonIdentity", stableIdentityKey(fact.getComparisonIdentity()));
        value.put("amount", fact.getAmount());
        value.put("currency", fact.getCurrency());
        value.put("rule", fact.getComparisonRuleRef().getNamespace() + ":"
                + fact.getComparisonRuleRef().getIdentity() + ":" + fact.getComparisonRuleRef().getVersion());
        value.put("comparisonStatusCode", fact.getComparisonStatusCode());
        value.put("comparisonProven", fact.getComparisonProven());
        value.put("claimKind", fact.getClaimKind());
        value.put("economicComponent", fact.getEconomicComponent());
        value.put("direction", fact.getDirection());
        value.put("normalizationVersion", fact.getNormalizationVersion());
        return value;
    }

    private String snapshotDigest(RecordReconciliationSourceSnapshotRequest request, String semanticDigest) {
        TreeMap<String, Object> value = new TreeMap<>();
        value.put("tenantId", request.getTenantId());
        value.put("batchSn", request.getReconciliationBatchSn().trim());
        value.put("sourceRole", request.getSourceRole());
        value.put("sourceNamespace", request.getSourceNamespace().trim());
        value.put("snapshotIdentity", stableIdentityKey(request.getSnapshotIdentity()));
        value.put("snapshotVersion", request.getSnapshotVersion().trim());
        value.put("coverageComplete", request.getCoverage().getComplete());
        value.put("coverageWatermark", normalizedOptionalText(request.getCoverage().getWatermark()));
        value.put("coverageMemberCount", request.getCoverage().getMemberCount());
        value.put("semanticDigest", semanticDigest);
        return FundsStableHashSupport.sha256Json(value);
    }

    private StableIdentity identity(String ownerNamespace, String value) {
        return new StableIdentity().setOwnerNamespace(ownerNamespace.trim()).setValue(value.trim());
    }

    private ComparisonRuleRef rule(String namespace, String identity, String version) {
        return new ComparisonRuleRef()
                .setNamespace(namespace.trim())
                .setIdentity(identity.trim())
                .setVersion(version.trim());
    }

    private String stableIdentityKey(StableIdentity identity) {
        return identity.getOwnerNamespace() + ":" + identity.getValue();
    }

    private List<String> normalizedEvidenceRefs(List<String> refs) {
        AssertUtils.notNull(refs, "来源证据引用不能为空");
        AssertUtils.isTrue(refs.size() <= RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_COUNT,
                "来源证据引用数量不能超过 {}", RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_COUNT);
        refs.forEach(ref -> {
            AssertUtils.hasText(ref, "来源证据引用不能为空");
            AssertUtils.isTrue(ref.trim().length() <= RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_LENGTH,
                    "来源证据引用长度不能超过 {}", RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_LENGTH);
        });
        return refs.stream().map(String::trim).distinct().sorted().toList();
    }

    private List<String> parseRefs(String refs) {
        return StringUtils.hasText(refs) ? List.copyOf(WindJson.parseArray(refs, String.class)) : List.of();
    }

    private String normalizedOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isValidZoneId(String timezoneId) {
        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (DateTimeException exception) {
            return false;
        }
    }
}
