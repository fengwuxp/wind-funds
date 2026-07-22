package com.wind.funds.reconciliation.application.batch.impl;

import com.alibaba.fastjson2.JSON;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceItem;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceItemMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceSnapshotMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.support.ReconciliationDigestSupport;
import com.wind.funds.transaction.support.FundsStableHashSupport;
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

    private final ReconciliationSourceSnapshotMapper reconciliationSourceSnapshotMapper;

    private final ReconciliationSourceItemMapper reconciliationSourceItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchDTO createBatch(CreateReconciliationBatchRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        ReconciliationBatch candidate = toBatch(request, operator);
        validatePreviousBatch(candidate);
        ReconciliationBatch existing = reconciliationBatchMapper.selectByDigest(
                candidate.getTenantId(), candidate.getBatchDigest());
        if (existing != null) {
            return toBatchDTO(existing);
        }
        try {
            reconciliationBatchMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ReconciliationBatch winner = reconciliationBatchMapper.selectByDigest(
                    candidate.getTenantId(), candidate.getBatchDigest());
            AssertUtils.notNull(winner, "对账批次唯一键冲突后未找到幂等结果");
            return toBatchDTO(winner);
        }
        AssertUtils.notNull(candidate.getId(), "创建对账批次失败");
        ReconciliationBatch saved = reconciliationBatchMapper.selectBySn(candidate.getTenantId(), candidate.getSn());
        AssertUtils.notNull(saved, "创建对账批次后未找到持久化事实");
        log.info("对账批次创建完成，tenantId = {}, sn = {}, gateObjectType = {}, gateObjectSn = {}",
                saved.getTenantId(), saved.getSn(), saved.getGateObjectType(), saved.getGateObjectSn());
        return toBatchDTO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationSourceSnapshotDTO recordSourceSnapshot(
            RecordReconciliationSourceSnapshotRequest request,
            WindOperator operator) {
        validateSourceSnapshotRequest(request, operator);
        List<String> sourceItemRefs = normalizedSourceItemRefs(request.getSourceItemRefs());
        List<String> evidenceRefs = normalizedEvidenceRefs(request.getEvidenceRefs());
        List<String> itemDigests = sourceItemRefs.stream()
                .map(sourceItemRef -> ReconciliationDigestSupport.sourceItemDigest(
                        request.getSourceRole(), request.getSourceType(), sourceItemRef))
                .toList();
        String sourceDigest = ReconciliationDigestSupport.sourceDigest(
                request.getSourceRole(), request.getSourceType(), itemDigests);
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        ReconciliationSourceSnapshot existing = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole().name());
        if (existing != null) {
            return reuseExistingSourceSnapshot(existing, request, sourceDigest, evidenceRefs);
        }
        AssertUtils.isTrue(batch.getStatus() == ReconciliationBatchStatus.CREATED
                        || batch.getStatus() == ReconciliationBatchStatus.DATA_COLLECTING,
                "当前对账批次状态不允许新增来源快照，reconciliationBatchSn = {}, status = {}",
                batch.getSn(), batch.getStatus());
        ReconciliationSourceSnapshot snapshot = toSourceSnapshot(
                request, operator, sourceDigest, sourceItemRefs.size(), evidenceRefs);
        reconciliationSourceSnapshotMapper.insertSelective(snapshot);
        AssertUtils.notNull(snapshot.getId(), "记录对账来源快照失败");
        for (int index = 0; index < sourceItemRefs.size(); index++) {
            persistSourceItem(request, operator, snapshot.getSn(), sourceItemRefs.get(index), itemDigests.get(index));
        }
        advanceBatchSourceStatus(batch);
        ReconciliationSourceSnapshot saved = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole().name());
        AssertUtils.notNull(saved, "记录对账来源快照后未找到持久化事实");
        log.info("对账来源快照记录完成，tenantId = {}, reconciliationBatchSn = {}, sourceRole = {}, recordCount = {}",
                request.getTenantId(), request.getReconciliationBatchSn(), request.getSourceRole(), sourceItemRefs.size());
        return toSourceSnapshotDTO(saved);
    }

    private void validateCreateRequest(CreateReconciliationBatchRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建对账批次请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账批次租户 ID 不能为空");
        AssertUtils.notNull(request.getGateObjectType(), "对账批次准入对象类型不能为空");
        AssertUtils.hasText(request.getGateObjectSn(), "对账批次准入对象流水号不能为空");
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
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账来源快照批次流水号不能为空");
        AssertUtils.notNull(request.getSourceRole(), "对账来源角色不能为空");
        AssertUtils.notNull(request.getSourceType(), "对账来源事实类型不能为空");
        AssertUtils.notNull(request.getSourceItemRefs(), "对账来源成员引用列表不能为空");
        AssertUtils.isTrue(request.getSourceItemRefs().stream().allMatch(StringUtils::hasText),
                "对账来源成员引用不能包含空值");
        AssertUtils.isTrue(request.getSourceItemRefs().stream().allMatch(value -> value.trim().length() <= 128),
                "对账来源成员引用长度不能超过 128");
        AssertUtils.notEmpty(request.getEvidenceRefs(), "对账来源证据引用不能为空");
        AssertUtils.isTrue(request.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "对账来源证据引用不能包含空值");
        AssertUtils.notNull(operator, "记录对账来源快照操作人不能为空");
    }

    private ReconciliationBatch toBatch(CreateReconciliationBatchRequest request, WindOperator operator) {
        ReconciliationBatch result = new ReconciliationBatch();
        result.setSn(TemporalSequenceFactory.hourNext(BATCH_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setGateObjectType(request.getGateObjectType());
        result.setGateObjectSn(request.getGateObjectSn().trim());
        result.setRuleVersion(request.getRuleVersion().trim());
        result.setWindowStart(request.getWindowStart());
        result.setWindowEnd(request.getWindowEnd());
        result.setTimezoneId(request.getTimezoneId().trim());
        result.setPreviousBatchSn(normalizedOptionalText(request.getPreviousBatchSn()));
        result.setStatus(ReconciliationBatchStatus.CREATED);
        result.setBatchDigest(batchDigest(result));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private String batchDigest(ReconciliationBatch batch) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", batch.getTenantId());
        facts.put("gateObjectType", batch.getGateObjectType());
        facts.put("gateObjectSn", batch.getGateObjectSn());
        facts.put("ruleVersion", batch.getRuleVersion());
        facts.put("windowStart", batch.getWindowStart());
        facts.put("windowEnd", batch.getWindowEnd());
        facts.put("timezoneId", batch.getTimezoneId());
        facts.put("previousBatchSn", batch.getPreviousBatchSn());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private void validatePreviousBatch(ReconciliationBatch candidate) {
        if (!StringUtils.hasText(candidate.getPreviousBatchSn())) {
            return;
        }
        ReconciliationBatch previous = reconciliationBatchMapper.selectBySn(
                candidate.getTenantId(), candidate.getPreviousBatchSn());
        AssertUtils.notNull(previous, "上一对账批次不存在，previousBatchSn = {}", candidate.getPreviousBatchSn());
        AssertUtils.isTrue(previous.getStatus() == ReconciliationBatchStatus.COMPLETED,
                "只有已完成对账批次可以发起重跑，previousBatchSn = {}, status = {}",
                previous.getSn(), previous.getStatus());
        AssertUtils.isTrue(previous.getGateObjectType() == candidate.getGateObjectType()
                        && Objects.equals(previous.getGateObjectSn(), candidate.getGateObjectSn()),
                "重跑批次准入对象必须与上一批次一致，previousBatchSn = {}", previous.getSn());
        AssertUtils.isTrue(Objects.equals(previous.getWindowStart(), candidate.getWindowStart())
                        && Objects.equals(previous.getWindowEnd(), candidate.getWindowEnd())
                        && Objects.equals(previous.getTimezoneId(), candidate.getTimezoneId()),
                "重跑批次对账窗口必须与上一批次一致，previousBatchSn = {}", previous.getSn());
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
        result.setEvidenceRefs(JSON.toJSONString(evidenceRefs));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private void persistSourceItem(RecordReconciliationSourceSnapshotRequest request,
                                   WindOperator operator,
                                   String sourceSnapshotSn,
                                   String sourceItemRef,
                                   String itemDigest) {
        ReconciliationSourceItem item = new ReconciliationSourceItem();
        item.setSn(TemporalSequenceFactory.hourNext(SOURCE_ITEM_SEQUENCE_TYPE));
        item.setTenantId(request.getTenantId());
        item.setSourceSnapshotSn(sourceSnapshotSn);
        item.setSourceItemRef(sourceItemRef);
        item.setItemDigest(itemDigest);
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
                        && Objects.equals(existing.getEvidenceRefs(), JSON.toJSONString(evidenceRefs)),
                "同一批次和来源角色的快照事实不一致，reconciliationBatchSn = {}, sourceRole = {}",
                existing.getReconciliationBatchSn(), existing.getSourceRole());
        return toSourceSnapshotDTO(existing);
    }

    private void advanceBatchSourceStatus(ReconciliationBatch batch) {
        List<ReconciliationSourceSnapshot> snapshots = reconciliationSourceSnapshotMapper.selectByBatch(
                batch.getTenantId(), batch.getSn());
        if (snapshots.size() == 2) {
            int totalRecordCount = snapshots.stream()
                    .mapToInt(ReconciliationSourceSnapshot::getRecordCount)
                    .sum();
            AssertUtils.isTrue(totalRecordCount > 0,
                    "对账批次两侧来源不能同时为空，reconciliationBatchSn = {}", batch.getSn());
        }
        ReconciliationBatchStatus targetStatus = snapshots.size() == 2
                ? ReconciliationBatchStatus.DATA_READY
                : ReconciliationBatchStatus.DATA_COLLECTING;
        AssertUtils.isTrue(reconciliationBatchMapper.updateStatus(batch.getTenantId(), batch.getSn(),
                        batch.getStatus().name(), targetStatus.name()) == 1,
                "推进对账批次来源状态失败，reconciliationBatchSn = {}, currentStatus = {}",
                batch.getSn(), batch.getStatus());
    }

    private ReconciliationBatchDTO toBatchDTO(ReconciliationBatch source) {
        return new ReconciliationBatchDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setGateObjectType(source.getGateObjectType())
                .setGateObjectSn(source.getGateObjectSn())
                .setRuleVersion(source.getRuleVersion())
                .setWindowStart(source.getWindowStart())
                .setWindowEnd(source.getWindowEnd())
                .setTimezoneId(source.getTimezoneId())
                .setPreviousBatchSn(source.getPreviousBatchSn())
                .setStatus(source.getStatus())
                .setRunResultSn(source.getRunResultSn())
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
                .setEvidenceRefs(List.copyOf(JSON.parseArray(source.getEvidenceRefs(), String.class)))
                .setCreatedBy(source.getCreatedBy())
                .setCreatedTime(source.getGmtCreate());
    }

    private List<String> normalizedSourceItemRefs(List<String> sourceItemRefs) {
        List<String> normalized = sourceItemRefs.stream().map(String::trim).sorted().toList();
        AssertUtils.isTrue(normalized.stream().distinct().count() == normalized.size(),
                "对账来源成员引用不能重复");
        return normalized;
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
}
