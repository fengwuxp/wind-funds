package com.wind.funds.reconciliation.application.batch.impl;

import com.wind.jackson.WindJson;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceItem;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchLineageMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceItemMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceSnapshotMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchState;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.AbortReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationSourceItemInput;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.model.request.ReplaceReconciliationBatchRequest;
import com.wind.funds.reconciliation.support.ReconciliationDigestSupport;
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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * 对账批次应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationBatchApplicationServiceImpl implements ReconciliationBatchApplicationService {

    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private static final WindSequenceType BATCH_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_BATCH", "RCB", 6);

    private static final WindSequenceType SOURCE_SNAPSHOT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_SOURCE_SNAPSHOT", "RSS", 6);

    private static final WindSequenceType SOURCE_ITEM_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_SOURCE_ITEM", "RSI", 6);

    private final ReconciliationBatchMapper reconciliationBatchMapper;

    private final ReconciliationBatchLineageMapper reconciliationBatchLineageMapper;

    private final ReconciliationSourceSnapshotMapper reconciliationSourceSnapshotMapper;

    private final ReconciliationSourceItemMapper reconciliationSourceItemMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

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
        validatePreviousBatchIdentity(candidate);
        ReconciliationBatch existingLineageBatch = claimBatchLineage(candidate);
        if (existingLineageBatch != null) {
            return toBatchDTO(existingLineageBatch);
        }
        ReconciliationBatch existingRerun = validatePreviousBatchAndSelectRerun(candidate);
        if (existingRerun != null) {
            AssertUtils.isTrue(Objects.equals(existingRerun.getBatchDigest(), candidate.getBatchDigest()),
                    "同一上一批次只允许创建一个直接重跑批次，previousBatchSn = {}",
                    candidate.getPreviousBatchSn());
            return toBatchDTO(existingRerun);
        }
        try {
            reconciliationBatchMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ReconciliationBatch winner = reconciliationBatchMapper.selectByDigestForUpdate(
                    candidate.getTenantId(), candidate.getBatchDigest());
            if (winner != null) {
                return toBatchDTO(winner);
            }
            assertPreviousBatchHasNoRerun(candidate);
            throw exception;
        }
        AssertUtils.notNull(candidate.getId(), "创建对账批次失败");
        advanceBatchLineage(candidate);
        ReconciliationBatch saved = reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn());
        AssertUtils.notNull(saved, "创建对账批次后未找到持久化事实");
        log.info("对账批次创建完成，tenantId = {}, sn = {}, reconciliationScopeRef = {}, "
                        + "gateObjectType = {}, gateObjectSn = {}",
                saved.getTenantId(), saved.getSn(), saved.getReconciliationScopeRef(),
                saved.getGateObjectType(), saved.getGateObjectSn());
        return toBatchDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO abortBatch(AbortReconciliationBatchRequest request, WindOperator operator) {
        validateAbortRequest(request, operator);
        ReconciliationBatch batch = lockBatchForAbort(request);
        String abortReason = request.getReason().trim();
        String abortedBy = operator.getOperatorAsText();
        if (batch.getState() == ReconciliationBatchState.ABORTED) {
            AssertUtils.isTrue(Objects.equals(batch.getAbortReason(), abortReason)
                            && Objects.equals(batch.getAbortedBy(), abortedBy),
                    "对账批次已由不同终止事实关闭，reconciliationBatchSn = {}", batch.getSn());
            return toBatchDTO(batch);
        }
        AssertUtils.isTrue(batch.getState() != ReconciliationBatchState.COMPLETED,
                "已完成对账批次不能终止，请使用 replaceBatch 创建替代批次，reconciliationBatchSn = {}",
                batch.getSn());
        LocalDateTime abortedTime = LocalDateTime.now();
        AssertUtils.isTrue(reconciliationBatchMapper.abort(batch.getTenantId(), batch.getSn(),
                        batch.getState().name(), abortedBy, abortedTime, abortReason) == 1,
                "终止对账批次失败，reconciliationBatchSn = {}", batch.getSn());
        ReconciliationBatch result = reconciliationBatchMapper.selectBySn(batch.getTenantId(), batch.getSn());
        AssertUtils.notNull(result, "终止对账批次后未找到持久化事实，reconciliationBatchSn = {}", batch.getSn());
        return toBatchDTO(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO replaceBatch(ReplaceReconciliationBatchRequest request, WindOperator operator) {
        validateReplaceRequest(request, operator);
        ReconciliationBatch snapshot = reconciliationBatchMapper.selectBySn(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(snapshot, "对账批次不存在，reconciliationBatchSn = {}",
                request.getReconciliationBatchSn());
        ReconciliationBatchLineage lineage = snapshot.getGateObjectType() == null
                ? null
                : reconciliationBatchLineageMapper.selectForUpdate(snapshot.getTenantId(),
                snapshot.getGateObjectType().name(), snapshot.getGateObjectSn());
        if (snapshot.getGateObjectType() != null) {
            AssertUtils.notNull(lineage, "Gate 对账批次血缘不存在，reconciliationBatchSn = {}", snapshot.getSn());
        }
        ReconciliationBatch replaced = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(replaced, "对账批次不存在，reconciliationBatchSn = {}",
                request.getReconciliationBatchSn());
        ReconciliationBatch existing = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        if (existing != null) {
            assertSameReplacement(existing, request);
            return toBatchDTO(existing);
        }
        AssertUtils.isTrue(replaced.getState() == ReconciliationBatchState.COMPLETED,
                "只有已完成对账批次可以替代，reconciliationBatchSn = {}, status = {}",
                replaced.getSn(), replaced.getState());
        if (lineage != null) {
            AssertUtils.isTrue(Objects.equals(lineage.getCurrentBatchSn(), replaced.getSn()),
                    "只有当前批次血缘头可以替代，reconciliationBatchSn = {}, currentBatchSn = {}",
                    replaced.getSn(), lineage.getCurrentBatchSn());
        }
        ReconciliationBatch candidate = toReplacementBatch(replaced, request, operator);
        try {
            reconciliationBatchMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ReconciliationBatch winner = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                    request.getTenantId(), request.getReconciliationBatchSn());
            if (winner != null) {
                assertSameReplacement(winner, request);
                return toBatchDTO(winner);
            }
            throw exception;
        }
        AssertUtils.notNull(candidate.getId(), "创建替代对账批次失败");
        reconciliationDifferenceMapper.invalidateByCurrentBatch(replaced.getTenantId(), replaced.getSn());
        advanceBatchLineage(candidate);
        ReconciliationBatch saved = reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn());
        AssertUtils.notNull(saved, "创建替代对账批次后未找到持久化事实");
        log.info("对账批次替代完成，tenantId = {}, replacedBatchSn = {}, replacementBatchSn = {}",
                saved.getTenantId(), replaced.getSn(), saved.getSn());
        return toBatchDTO(saved);
    }

    private ReconciliationBatch lockBatchForAbort(AbortReconciliationBatchRequest request) {
        ReconciliationBatch snapshot = reconciliationBatchMapper.selectBySn(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(snapshot, "对账批次不存在，reconciliationBatchSn = {}",
                request.getReconciliationBatchSn());
        if (snapshot.getGateObjectType() == null) {
            ReconciliationBatch result = reconciliationBatchMapper.selectBySnForUpdate(
                    request.getTenantId(), request.getReconciliationBatchSn());
            if (result.getState() != ReconciliationBatchState.ABORTED) {
                AssertUtils.isTrue(reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                                result.getTenantId(), result.getSn()) == null,
                        "只有当前批次血缘头可以终止，reconciliationBatchSn = {}", result.getSn());
            }
            return result;
        }
        ReconciliationBatchLineage lineage = reconciliationBatchLineageMapper.selectForUpdate(
                snapshot.getTenantId(), snapshot.getGateObjectType().name(), snapshot.getGateObjectSn());
        ReconciliationBatch result = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        if (result.getState() != ReconciliationBatchState.ABORTED) {
            AssertUtils.isTrue(lineage != null && Objects.equals(lineage.getCurrentBatchSn(), result.getSn()),
                    "只有当前批次血缘头可以终止，reconciliationBatchSn = {}", result.getSn());
        }
        return result;
    }

    private void validateAbortRequest(AbortReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "终止对账批次请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "终止对账批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "终止对账批次 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "终止对账批次流水号不能为空");
        AssertUtils.hasText(request.getReason(), "终止对账批次原因不能为空");
        AssertUtils.isTrue(request.getReason().trim().length() <= AbortReconciliationBatchRequest.MAX_REASON_LENGTH,
                "终止对账批次原因长度不能超过 {}", AbortReconciliationBatchRequest.MAX_REASON_LENGTH);
        AssertUtils.notNull(operator, "终止对账批次操作人不能为空");
    }

    private void validateReplaceRequest(ReplaceReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "替代对账批次请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "替代对账批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "替代对账批次 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "被替代对账批次流水号不能为空");
        AssertUtils.isTrue(request.getReconciliationBatchSn().trim().length() <= 64,
                "被替代对账批次流水号长度不能超过 64");
        AssertUtils.hasText(request.getRuleVersion(), "替代对账批次规则版本不能为空");
        AssertUtils.isTrue(request.getRuleVersion().trim().length() <= 64,
                "替代对账批次规则版本长度不能超过 64");
        AssertUtils.hasText(request.getReason(), "替代对账批次原因不能为空");
        AssertUtils.isTrue(request.getReason().trim().length() <= ReplaceReconciliationBatchRequest.MAX_REASON_LENGTH,
                "替代对账批次原因长度不能超过 {}", ReplaceReconciliationBatchRequest.MAX_REASON_LENGTH);
        AssertUtils.hasText(request.getEvidenceRef(), "替代对账批次证据引用不能为空");
        AssertUtils.isTrue(request.getEvidenceRef().trim().length()
                        <= ReplaceReconciliationBatchRequest.MAX_EVIDENCE_REF_LENGTH,
                "替代对账批次证据引用长度不能超过 {}",
                ReplaceReconciliationBatchRequest.MAX_EVIDENCE_REF_LENGTH);
        AssertUtils.notNull(operator, "替代对账批次操作人不能为空");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationSourceSnapshotDTO recordSourceSnapshot(
            RecordReconciliationSourceSnapshotRequest request,
            WindOperator operator) {
        validateSourceSnapshotRequest(request, operator);
        List<ReconciliationSourceItemInput> sourceItems = normalizedSourceItems(request.getSourceItems());
        List<String> evidenceRefs = normalizedEvidenceRefs(request.getEvidenceRefs());
        String sourceDigest = ReconciliationDigestSupport.sourceDigest(
                request.getSourceRole(), request.getSourceType(), sourceContentDigests(sourceItems));
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        ReconciliationSourceSnapshot existing = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole().name());
        if (existing != null) {
            return reuseExistingSourceSnapshot(existing, request, sourceDigest, evidenceRefs);
        }
        AssertUtils.isTrue(batch.getState() == ReconciliationBatchState.CREATED
                        || batch.getState() == ReconciliationBatchState.DATA_COLLECTING,
                "当前对账批次状态不允许新增来源快照，reconciliationBatchSn = {}, status = {}",
                batch.getSn(), batch.getState());
        ReconciliationSourceSnapshot snapshot = toSourceSnapshot(
                request, operator, sourceDigest, sourceItems.size(), evidenceRefs);
        reconciliationSourceSnapshotMapper.insertSelective(snapshot);
        AssertUtils.notNull(snapshot.getId(), "记录对账来源快照失败");
        sourceItems.forEach(sourceItem -> persistSourceItem(request, operator, snapshot.getSn(), sourceItem));
        advanceBatchSourceState(batch);
        ReconciliationSourceSnapshot saved = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole().name());
        AssertUtils.notNull(saved, "记录对账来源快照后未找到持久化事实");
        log.info("对账来源快照记录完成，tenantId = {}, reconciliationBatchSn = {}, sourceRole = {}, recordCount = {}",
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole(), sourceItems.size());
        return toSourceSnapshotDTO(saved);
    }

    private void validateCreateRequest(CreateReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建对账批次请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账批次租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账批次 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationScopeRef(), "对账批次范围引用不能为空");
        AssertUtils.isTrue(request.getReconciliationScopeRef().trim().length() <= 128,
                "对账批次范围引用长度不能超过 128");
        boolean hasGateObjectType = request.getGateObjectType() != null;
        boolean hasGateObjectSn = StringUtils.hasText(request.getGateObjectSn());
        AssertUtils.isTrue(hasGateObjectType == hasGateObjectSn,
                "对账批次准入对象类型和流水号必须同时提供或同时为空");
        AssertUtils.hasText(request.getRuleVersion(), "对账批次规则版本不能为空");
        AssertUtils.notNull(request.getWindowStart(), "对账窗口开始时间不能为空");
        AssertUtils.notNull(request.getWindowEnd(), "对账窗口结束时间不能为空");
        AssertUtils.isTrue(request.getWindowStart().isBefore(request.getWindowEnd()),
                "对账窗口开始时间必须早于结束时间");
        AssertUtils.hasText(request.getTimezoneId(), "对账窗口时区 ID 不能为空");
        AssertUtils.isTrue(isValidZoneId(request.getTimezoneId().trim()),
                "对账窗口时区 ID 无效，timezoneId = {}", request.getTimezoneId());
        AssertUtils.notNull(operator, "创建对账批次操作人不能为空");
    }

    private void validateSourceSnapshotRequest(RecordReconciliationSourceSnapshotRequest request,
                                               WindOperator operator) {
        AssertUtils.notNull(request, "记录对账来源快照请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账来源快照租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账来源快照 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账来源快照批次流水号不能为空");
        AssertUtils.notNull(request.getSourceRole(), "对账来源角色不能为空");
        AssertUtils.notNull(request.getSourceType(), "对账来源事实类型不能为空");
        AssertUtils.notNull(request.getSourceItems(), "对账来源成员列表不能为空");
        AssertUtils.isTrue(request.getSourceItems().size()
                        <= RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT,
                "对账来源成员数量不能超过 {}",
                RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT);
        AssertUtils.isTrue(request.getSourceItems().stream().allMatch(Objects::nonNull),
                "对账来源成员不能包含空值");
        AssertUtils.isTrue(request.getSourceItems().stream()
                        .map(ReconciliationSourceItemInput::getSourceItemRef)
                        .allMatch(StringUtils::hasText),
                "对账来源成员引用不能包含空值");
        AssertUtils.isTrue(request.getSourceItems().stream()
                        .map(ReconciliationSourceItemInput::getSourceItemRef)
                        .allMatch(value -> value.trim().length()
                                <= ReconciliationSourceItemInput.MAX_SOURCE_ITEM_REF_LENGTH),
                "对账来源成员引用长度不能超过 128");
        AssertUtils.isTrue(request.getSourceItems().stream()
                        .map(ReconciliationSourceItemInput::getContentDigest)
                        .allMatch(this::isSha256),
                "对账来源成员内容摘要必须是 64 位小写 SHA-256");
        AssertUtils.notEmpty(request.getEvidenceRefs(), "对账来源证据引用不能为空");
        AssertUtils.isTrue(request.getEvidenceRefs().size()
                        <= RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_COUNT,
                "对账来源证据引用数量不能超过 {}",
                RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_COUNT);
        AssertUtils.isTrue(request.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "对账来源证据引用不能包含空值");
        AssertUtils.isTrue(request.getEvidenceRefs().stream()
                        .allMatch(value -> value.trim().length()
                                <= RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_LENGTH),
                "对账来源证据引用长度不能超过 {}",
                RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_LENGTH);
        AssertUtils.notNull(operator, "记录对账来源快照操作人不能为空");
    }

    private ReconciliationBatch toBatch(CreateReconciliationBatchRequest request, WindOperator operator) {
        ReconciliationBatch result = new ReconciliationBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setReconciliationScopeRef(request.getReconciliationScopeRef().trim());
        result.setGateObjectType(request.getGateObjectType());
        result.setGateObjectSn(normalizedOptionalText(request.getGateObjectSn()));
        result.setRuleVersion(request.getRuleVersion().trim());
        result.setWindowStart(request.getWindowStart());
        result.setWindowEnd(request.getWindowEnd());
        result.setTimezoneId(request.getTimezoneId().trim());
        result.setPreviousBatchSn(normalizedOptionalText(request.getPreviousBatchSn()));
        result.setState(ReconciliationBatchState.CREATED);
        result.setBatchDigest(batchDigest(result));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private ReconciliationBatch toReplacementBatch(ReconciliationBatch replaced,
                                                    ReplaceReconciliationBatchRequest request,
                                                    WindOperator operator) {
        ReconciliationBatch result = new ReconciliationBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(replaced.getTenantId());
        result.setReconciliationScopeRef(replaced.getReconciliationScopeRef());
        result.setGateObjectType(replaced.getGateObjectType());
        result.setGateObjectSn(replaced.getGateObjectSn());
        result.setRuleVersion(request.getRuleVersion().trim());
        result.setWindowStart(replaced.getWindowStart());
        result.setWindowEnd(replaced.getWindowEnd());
        result.setTimezoneId(replaced.getTimezoneId());
        result.setPreviousBatchSn(replaced.getSn());
        result.setState(ReconciliationBatchState.CREATED);
        result.setReplacementReason(request.getReason().trim());
        result.setReplacementEvidenceRef(request.getEvidenceRef().trim());
        result.setBatchDigest(batchDigest(result));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private String batchDigest(ReconciliationBatch batch) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", batch.getTenantId());
        facts.put("reconciliationScopeRef", batch.getReconciliationScopeRef());
        facts.put("gateObjectType", batch.getGateObjectType());
        facts.put("gateObjectSn", batch.getGateObjectSn());
        facts.put("ruleVersion", batch.getRuleVersion());
        facts.put("windowStart", batch.getWindowStart());
        facts.put("windowEnd", batch.getWindowEnd());
        facts.put("timezoneId", batch.getTimezoneId());
        facts.put("previousBatchSn", batch.getPreviousBatchSn());
        facts.put("replacementReason", batch.getReplacementReason());
        facts.put("replacementEvidenceRef", batch.getReplacementEvidenceRef());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private void assertSameReplacement(ReconciliationBatch existing,
                                       ReplaceReconciliationBatchRequest request) {
        AssertUtils.isTrue(Objects.equals(existing.getPreviousBatchSn(), request.getReconciliationBatchSn().trim())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion().trim())
                        && Objects.equals(existing.getReplacementReason(), request.getReason().trim())
                        && Objects.equals(existing.getReplacementEvidenceRef(), request.getEvidenceRef().trim()),
                "对账批次已由不同替代事实处理，reconciliationBatchSn = {}",
                request.getReconciliationBatchSn());
    }

    private ReconciliationBatch validatePreviousBatchAndSelectRerun(ReconciliationBatch candidate) {
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return null;
        }
        ReconciliationBatch previous = reconciliationBatchMapper.selectBySnForUpdate(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
        AssertUtils.notNull(previous, "上一对账批次不存在，previousBatchSn = {}", candidate.getPreviousBatchSn());
        AssertUtils.isTrue(previous.getState() == ReconciliationBatchState.COMPLETED
                        || previous.getState() == ReconciliationBatchState.ABORTED,
                "只有已完成或已终止对账批次可以发起重跑，previousBatchSn = {}, status = {}",
                previous.getSn(), previous.getState());
        if (previous.getState() == ReconciliationBatchState.COMPLETED) {
            assertGateDifferencesMaterialized(previous);
        }
        AssertUtils.isTrue(Objects.equals(previous.getReconciliationScopeRef(), candidate.getReconciliationScopeRef()),
                "重跑批次对账范围必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(previous.getGateObjectType() == candidate.getGateObjectType()
                        && Objects.equals(previous.getGateObjectSn(), candidate.getGateObjectSn()),
                "重跑批次准入对象必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(Objects.equals(previous.getWindowStart(), candidate.getWindowStart())
                        && Objects.equals(previous.getWindowEnd(), candidate.getWindowEnd())
                        && Objects.equals(previous.getTimezoneId(), candidate.getTimezoneId()),
                "重跑批次对账窗口必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        return reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
    }

    private void validatePreviousBatchIdentity(ReconciliationBatch candidate) {
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return;
        }
        ReconciliationBatch previous = reconciliationBatchMapper.selectBySn(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
        AssertUtils.notNull(previous, "上一对账批次不存在，previousBatchSn = {}", candidate.getPreviousBatchSn());
        AssertUtils.isTrue(Objects.equals(previous.getReconciliationScopeRef(), candidate.getReconciliationScopeRef()),
                "重跑批次对账范围必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(previous.getGateObjectType() == candidate.getGateObjectType()
                        && Objects.equals(previous.getGateObjectSn(), candidate.getGateObjectSn()),
                "重跑批次准入对象必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(Objects.equals(previous.getWindowStart(), candidate.getWindowStart())
                        && Objects.equals(previous.getWindowEnd(), candidate.getWindowEnd())
                        && Objects.equals(previous.getTimezoneId(), candidate.getTimezoneId()),
                "重跑批次对账窗口必须与上一批次一致，previousBatchSn = {}", previous.getSn());
    }

    private ReconciliationBatch claimBatchLineage(ReconciliationBatch candidate) {
        if (candidate.getGateObjectType() == null) {
            return null;
        }
        ReconciliationBatchLineage lineage = selectBatchLineageForUpdate(candidate);
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            if (lineage == null) {
                try {
                    reconciliationBatchLineageMapper.insertSelective(toBatchLineage(candidate));
                    return null;
                } catch (DuplicateKeyException exception) {
                    lineage = selectBatchLineageForUpdate(candidate);
                    AssertUtils.notNull(lineage, "创建 Gate 对账批次血缘失败");
                }
            }
            ReconciliationBatch current = reconciliationBatchMapper.selectBySn(
                    candidate.getTenantId(), lineage.getCurrentBatchSn());
            AssertUtils.isTrue(current != null && Objects.equals(current.getBatchDigest(), candidate.getBatchDigest()),
                    "同一准入对象对账血缘已存在，currentBatchSn = {}", lineage.getCurrentBatchSn());
            return current;
        }
        AssertUtils.notNull(lineage,
                "Gate 对账重跑缺少批次血缘，previousBatchSn = {}", candidate.getPreviousBatchSn());
        if (Objects.equals(lineage.getCurrentBatchSn(), candidate.getPreviousBatchSn())) {
            return null;
        }
        ReconciliationBatch existingRerun = reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
        if (existingRerun != null) {
            AssertUtils.isTrue(Objects.equals(existingRerun.getBatchDigest(), candidate.getBatchDigest()),
                    "同一上一批次只允许创建一个直接重跑批次，previousBatchSn = {}",
                    candidate.getPreviousBatchSn());
            return existingRerun;
        }
        AssertUtils.isTrue(false,
                "上一对账批次不是当前血缘头，previousBatchSn = {}, currentBatchSn = {}",
                candidate.getPreviousBatchSn(), lineage.getCurrentBatchSn());
        return null;
    }

    private ReconciliationBatchLineage selectBatchLineageForUpdate(ReconciliationBatch candidate) {
        return reconciliationBatchLineageMapper.selectForUpdate(candidate.getTenantId(),
                candidate.getGateObjectType().name(), candidate.getGateObjectSn());
    }

    private ReconciliationBatchLineage toBatchLineage(ReconciliationBatch candidate) {
        ReconciliationBatchLineage result = new ReconciliationBatchLineage();
        result.setTenantId(candidate.getTenantId());
        result.setReconciliationScopeRef(candidate.getReconciliationScopeRef());
        result.setGateObjectType(candidate.getGateObjectType());
        result.setGateObjectSn(candidate.getGateObjectSn());
        result.setCurrentBatchSn(candidate.getSn());
        return result;
    }

    private void advanceBatchLineage(ReconciliationBatch candidate) {
        if (candidate.getGateObjectType() == null || !StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return;
        }
        int updated = reconciliationBatchLineageMapper.advance(candidate.getTenantId(),
                candidate.getGateObjectType().name(), candidate.getGateObjectSn(),
                candidate.getPreviousBatchSn(), candidate.getSn());
        AssertUtils.isTrue(updated == 1,
                "推进 Gate 对账批次血缘失败，previousBatchSn = {}, currentBatchSn = {}",
                candidate.getPreviousBatchSn(), candidate.getSn());
    }

    private void assertGateDifferencesMaterialized(ReconciliationBatch previous) {
        if (previous.getGateObjectType() == null) {
            return;
        }
        AssertUtils.hasText(previous.getRunResultSn(),
                "上一 Gate 对账批次缺少完成运行结果，previousBatchSn = {}", previous.getSn());
        ReconciliationRunResult runResult = reconciliationRunResultMapper.selectBySn(
                previous.getTenantId(), previous.getRunResultSn());
        AssertUtils.notNull(runResult,
                "上一 Gate 对账批次运行结果不存在，previousBatchSn = {}, runResultSn = {}",
                previous.getSn(), previous.getRunResultSn());
        AssertUtils.isTrue(Objects.equals(runResult.getReconciliationBatchSn(), previous.getSn()),
                "上一 Gate 对账批次运行结果归属不一致，previousBatchSn = {}, runResultSn = {}",
                previous.getSn(), previous.getRunResultSn());
        int materializedDifferenceCount = reconciliationDifferenceMapper.countByCurrentBatch(
                previous.getTenantId(), previous.getSn());
        AssertUtils.isTrue(Objects.equals(runResult.getDifferenceCount(), materializedDifferenceCount),
                "Gate 对账差异尚未全部物化，previousBatchSn = {}, expected = {}, actual = {}",
                previous.getSn(), runResult.getDifferenceCount(), materializedDifferenceCount);
        Long unreadyDifferenceId = reconciliationDifferenceMapper.selectFirstUnreadyForRerunIdForUpdate(
                previous.getTenantId(), previous.getSn());
        AssertUtils.isTrue(unreadyDifferenceId == null,
                "Gate 对账差错必须先完成处理动作再发起重跑，previousBatchSn = {}, differenceId = {}",
                previous.getSn(), unreadyDifferenceId);
    }

    private void assertPreviousBatchHasNoRerun(ReconciliationBatch candidate) {
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return;
        }
        AssertUtils.isTrue(reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                        candidate.getTenantId(), candidate.getPreviousBatchSn()) == null,
                "同一上一批次只允许创建一个直接重跑批次，previousBatchSn = {}",
                candidate.getPreviousBatchSn());
    }

    private ReconciliationSourceSnapshot toSourceSnapshot(RecordReconciliationSourceSnapshotRequest request,
                                                           WindOperator operator,
                                                           String sourceDigest,
                                                           int recordCount,
                                                           List<String> evidenceRefs) {
        ReconciliationSourceSnapshot result = new ReconciliationSourceSnapshot();
        result.setSn(TemporalSequenceFactory.hourNext(SOURCE_SNAPSHOT_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setReconciliationBatchSn(request.getReconciliationBatchSn().trim());
        result.setSourceRole(request.getSourceRole());
        result.setSourceType(request.getSourceType());
        result.setSourceDigest(sourceDigest);
        result.setRecordCount(recordCount);
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private void persistSourceItem(RecordReconciliationSourceSnapshotRequest request,
                                   WindOperator operator,
                                   String sourceSnapshotSn,
                                   ReconciliationSourceItemInput sourceItem) {
        ReconciliationSourceItem item = new ReconciliationSourceItem();
        item.setSn(TemporalSequenceFactory.hourNext(SOURCE_ITEM_SEQUENCE_TYPE));
        item.setTenantId(request.getTenantId());
        item.setSourceSnapshotSn(sourceSnapshotSn);
        item.setSourceItemRef(sourceItem.getSourceItemRef());
        item.setContentDigest(sourceItem.getContentDigest());
        item.setCreatedBy(operator.getOperatorAsText());
        reconciliationSourceItemMapper.insertSelective(item);
        AssertUtils.notNull(item.getId(), "记录对账来源成员失败");
    }

    private ReconciliationSourceSnapshotDTO reuseExistingSourceSnapshot(
            ReconciliationSourceSnapshot existing,
            RecordReconciliationSourceSnapshotRequest request,
            String sourceDigest,
            List<String> evidenceRefs) {
        AssertUtils.isTrue(existing.getSourceType() == request.getSourceType()
                        && Objects.equals(existing.getSourceDigest(), sourceDigest)
                        && Objects.equals(existing.getEvidenceRefs(), WindJson.toJsonString(evidenceRefs)),
                "同一批次和来源角色的快照事实不一致，reconciliationBatchSn = {}, sourceRole = {}",
                existing.getReconciliationBatchSn(), existing.getSourceRole());
        return toSourceSnapshotDTO(existing);
    }

    private void advanceBatchSourceState(ReconciliationBatch batch) {
        List<ReconciliationSourceSnapshot> snapshots = reconciliationSourceSnapshotMapper.selectByBatch(
                batch.getTenantId(), batch.getSn());
        if (snapshots.size() == 2) {
            int totalRecordCount = snapshots.stream()
                    .mapToInt(ReconciliationSourceSnapshot::getRecordCount)
                    .sum();
            AssertUtils.isTrue(totalRecordCount > 0,
                    "对账批次两侧来源不能同时为空，reconciliationBatchSn = {}", batch.getSn());
        }
        ReconciliationBatchState targetState = snapshots.size() == 2
                ? ReconciliationBatchState.DATA_READY
                : ReconciliationBatchState.DATA_COLLECTING;
        AssertUtils.isTrue(reconciliationBatchMapper.updateState(batch.getTenantId(), batch.getSn(),
                        batch.getState().name(), targetState.name()) == 1,
                "推进对账批次来源状态失败，reconciliationBatchSn = {}, currentStatus = {}",
                batch.getSn(), batch.getState());
    }

    private ReconciliationBatchDTO toBatchDTO(ReconciliationBatch source) {
        return new ReconciliationBatchDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setReconciliationScopeRef(source.getReconciliationScopeRef())
                .setGateObjectType(source.getGateObjectType())
                .setGateObjectSn(source.getGateObjectSn())
                .setRuleVersion(source.getRuleVersion())
                .setWindowStart(source.getWindowStart())
                .setWindowEnd(source.getWindowEnd())
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
        List<String> sourceItemRefs = reconciliationSourceItemMapper.selectBySnapshot(
                        source.getTenantId(), source.getSn()).stream()
                .map(ReconciliationSourceItem::getSourceItemRef)
                .toList();
        return new ReconciliationSourceSnapshotDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setReconciliationBatchSn(source.getReconciliationBatchSn())
                .setSourceRole(source.getSourceRole())
                .setSourceType(source.getSourceType())
                .setSourceDigest(source.getSourceDigest())
                .setRecordCount(source.getRecordCount())
                .setSourceItemRefs(sourceItemRefs)
                .setEvidenceRefs(List.copyOf(WindJson.parseArray(source.getEvidenceRefs(), String.class)))
                .setCreatedBy(source.getCreatedBy())
                .setCreatedTime(source.getGmtCreate());
    }

    private List<ReconciliationSourceItemInput> normalizedSourceItems(
            List<ReconciliationSourceItemInput> sourceItems) {
        List<ReconciliationSourceItemInput> normalized = sourceItems.stream()
                .map(sourceItem -> new ReconciliationSourceItemInput()
                        .setSourceItemRef(sourceItem.getSourceItemRef().trim())
                        .setContentDigest(sourceItem.getContentDigest()))
                .sorted((left, right) -> left.getSourceItemRef().compareTo(right.getSourceItemRef()))
                .toList();
        AssertUtils.isTrue(normalized.stream()
                        .map(ReconciliationSourceItemInput::getSourceItemRef)
                        .distinct().count() == normalized.size(),
                "对账来源成员引用不能重复");
        return normalized;
    }

    private Map<String, String> sourceContentDigests(List<ReconciliationSourceItemInput> sourceItems) {
        Map<String, String> result = new TreeMap<>();
        sourceItems.forEach(sourceItem -> result.put(sourceItem.getSourceItemRef(), sourceItem.getContentDigest()));
        return result;
    }

    private List<String> normalizedEvidenceRefs(List<String> evidenceRefs) {
        return evidenceRefs.stream().map(String::trim).distinct().sorted().toList();
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

    private boolean isSha256(String digest) {
        return digest != null && SHA_256_PATTERN.matcher(digest).matches();
    }
}
