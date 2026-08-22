package com.wind.funds.reconciliation.application.run.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.AbstractPageQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceItem;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import com.wind.funds.reconciliation.dal.entities.table.ReconciliationMatchResultNameRefs;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationMatchResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceItemMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationSourceSnapshotMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchState;
import com.wind.funds.reconciliation.enums.ReconciliationMatchResultKind;
import com.wind.funds.reconciliation.enums.ReconciliationRunOutcome;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.mapstruct.ReconciliationMatchResultConverter;
import com.wind.funds.reconciliation.mapstruct.ReconciliationRunResultConverter;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.jackson.WindJson;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 严格精确对账运行服务。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationRunResultApplicationServiceImpl
        implements ReconciliationRunResultApplicationService {

    private static final WindSequenceType RUN_RESULT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_RUN_RESULT", "RRR", 6);

    private static final WindSequenceType MATCH_RESULT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_MATCH_RESULT", "RMR", 6);

    private final ReconciliationBatchMapper reconciliationBatchMapper;

    private final ReconciliationSourceSnapshotMapper reconciliationSourceSnapshotMapper;

    private final ReconciliationSourceItemMapper reconciliationSourceItemMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationMatchResultMapper reconciliationMatchResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRunResultDTO executeStrictExact(RecordReconciliationRunResultRequest request,
                                                          WindOperator operator) {
        validateRequest(request, operator);
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn().trim());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        AssertUtils.isTrue(batch.getState() == ReconciliationBatchState.DATA_READY
                        || batch.getState() == ReconciliationBatchState.COMPLETED,
                "对账批次来源尚未冻结完整，reconciliationBatchSn = {}, status = {}",
                batch.getSn(), batch.getState());

        SourceSet sources = loadSources(batch);
        List<ReconciliationMatchResult> matches = compare(batch, sources, operator);
        ReconciliationRunResult candidate = toRunResult(batch, sources, matches, operator);
        ReconciliationRunResult existing = reconciliationRunResultMapper.selectByBatch(batch.getTenantId(), batch.getSn());
        if (existing != null) {
            AssertUtils.isTrue(batch.getState() == ReconciliationBatchState.COMPLETED
                            && Objects.equals(batch.getRunResultSn(), existing.getSn())
                            && Objects.equals(existing.getResultDigest(), candidate.getResultDigest()),
                    "同一批次的对账运行结果事实冲突，reconciliationBatchSn = {}", batch.getSn());
            return ReconciliationRunResultConverter.INSTANCE.toDTO(existing);
        }

        reconciliationRunResultMapper.insertSelective(candidate);
        AssertUtils.notNull(candidate.getId(), "记录对账运行结果失败");
        matches.forEach(match -> {
            match.setReconciliationRunResultSn(candidate.getSn());
            reconciliationMatchResultMapper.insertSelective(match);
            AssertUtils.notNull(match.getId(), "记录对账逐笔匹配结果失败");
        });
        AssertUtils.isTrue(reconciliationBatchMapper.complete(batch.getTenantId(), batch.getSn(), candidate.getSn()) == 1,
                "完成对账批次失败，reconciliationBatchSn = {}", batch.getSn());
        log.info("strict-exact 对账完成，tenantId = {}, batchSn = {}, outcome = {}, total = {}",
                batch.getTenantId(), batch.getSn(), candidate.getOutcome(), candidate.getTotalCount());
        return ReconciliationRunResultConverter.INSTANCE.toDTO(
                reconciliationRunResultMapper.selectBySn(batch.getTenantId(), candidate.getSn()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationRunResultDTO getRunResult(Long tenantId, String runResultSn) {
        validateQueryIdentity(tenantId, runResultSn);
        ReconciliationRunResult result = reconciliationRunResultMapper.selectBySn(tenantId, runResultSn);
        AssertUtils.notNull(result, "对账运行结果不存在，runResultSn = {}", runResultSn);
        return ReconciliationRunResultConverter.INSTANCE.toDTO(result);
    }

    @Override
    @Transactional(readOnly = true)
    public WindPagination<ReconciliationMatchResultDTO> queryMatchResults(
            Long tenantId,
            String runResultSn,
            AbstractPageQuery<? extends QueryOrderField> options) {
        validateQueryIdentity(tenantId, runResultSn);
        AssertUtils.notNull(options, "对账逐笔匹配结果查询选项不能为空");
        AssertUtils.isTrue(!options.shouldOrderBy(), "对账逐笔匹配结果不支持自定义排序");
        AssertUtils.notNull(reconciliationRunResultMapper.selectBySn(tenantId, runResultSn),
                "对账运行结果不存在，runResultSn = {}", runResultSn);
        ReconciliationMatchResultNameRefs matchResult =
                ReconciliationMatchResultNameRefs.reconciliationMatchResult;
        QueryWrapper queryWrapper = QueryWrapper.create().select()
                .from(matchResult)
                .where(matchResult.tenantId.eq(tenantId))
                .and(matchResult.reconciliationRunResultSn.eq(runResultSn))
                .orderBy(matchResult.id.asc());
        return MybatisQueryHelper.<ReconciliationMatchResult, ReconciliationMatchResultDTO>query(queryWrapper)
                .counter(reconciliationMatchResultMapper::selectCountByQuery)
                .resultQueryFunc(reconciliationMatchResultMapper::selectListByQuery)
                .converter(ReconciliationMatchResultConverter.INSTANCE::toDTO)
                .query(options);
    }

    private SourceSet loadSources(ReconciliationBatch batch) {
        SourceFacts reference = loadSource(batch, ReconciliationSourceRole.REFERENCE);
        SourceFacts comparison = loadSource(batch, ReconciliationSourceRole.COMPARISON);
        AssertUtils.isTrue(!reference.items().isEmpty() || !comparison.items().isEmpty(),
                "对账批次两侧来源不能同时为空，reconciliationBatchSn = {}", batch.getSn());
        return new SourceSet(reference, comparison);
    }

    private SourceFacts loadSource(ReconciliationBatch batch, ReconciliationSourceRole role) {
        ReconciliationSourceSnapshot snapshot = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                batch.getTenantId(), batch.getSn(), role.name());
        AssertUtils.notNull(snapshot, "对账批次缺少{}来源快照，reconciliationBatchSn = {}", role.getDesc(), batch.getSn());
        List<ReconciliationSourceItem> items = reconciliationSourceItemMapper.selectBySnapshot(
                batch.getTenantId(), snapshot.getSn());
        AssertUtils.isTrue(items.size() == snapshot.getCoverageMemberCount(),
                "对账来源快照成员数冲突，sourceSnapshotSn = {}", snapshot.getSn());

        List<Map<String, Object>> semanticFacts = items.stream()
                .sorted(java.util.Comparator.comparing(this::sourceFactKey))
                .<Map<String, Object>>map(this::semanticFact)
                .toList();
        String semanticDigest = FundsStableHashSupport.sha256Json(semanticFacts);
        AssertUtils.isTrue(Objects.equals(semanticDigest, snapshot.getSemanticDigest()),
                "对账来源事实冲突，sourceSnapshotSn = {}", snapshot.getSn());
        for (ReconciliationSourceItem item : items) {
            AssertUtils.isTrue(Objects.equals(FundsStableHashSupport.sha256Json(semanticFact(item)),
                            item.getSemanticDigest()),
                    "对账来源事实摘要不一致，sourceFact = {}", sourceFactKey(item));
            AssertUtils.isTrue(Objects.equals(FundsStableHashSupport.sha256Json(parseRefs(item.getEvidenceRefs())),
                            item.getEvidenceBundleDigest()),
                    "对账来源事实证据摘要不一致，sourceFact = {}", sourceFactKey(item));
        }
        AssertUtils.isTrue(Objects.equals(snapshotDigest(snapshot, semanticDigest), snapshot.getSourceDigest()),
                "对账来源快照摘要不一致，sourceSnapshotSn = {}", snapshot.getSn());
        AssertUtils.isTrue(Objects.equals(FundsStableHashSupport.sha256Json(parseRefs(snapshot.getEvidenceRefs())),
                        snapshot.getEvidenceBundleDigest()),
                "对账来源快照证据摘要不一致，sourceSnapshotSn = {}", snapshot.getSn());
        return new SourceFacts(snapshot, List.copyOf(items));
    }

    private List<ReconciliationMatchResult> compare(ReconciliationBatch batch,
                                                     SourceSet sources,
                                                     WindOperator operator) {
        Map<String, List<ReconciliationSourceItem>> references = byComparisonIdentity(sources.reference().items());
        Map<String, List<ReconciliationSourceItem>> comparisons = byComparisonIdentity(sources.comparison().items());
        return java.util.stream.Stream.concat(references.keySet().stream(), comparisons.keySet().stream())
                .distinct()
                .sorted()
                .map(key -> toMatchResult(batch, references.getOrDefault(key, List.of()),
                        comparisons.getOrDefault(key, List.of()), operator))
                .toList();
    }

    private Map<String, List<ReconciliationSourceItem>> byComparisonIdentity(List<ReconciliationSourceItem> items) {
        return items.stream().collect(Collectors.groupingBy(this::comparisonIdentityKey));
    }

    private ReconciliationMatchResult toMatchResult(ReconciliationBatch batch,
                                                     List<ReconciliationSourceItem> references,
                                                     List<ReconciliationSourceItem> comparisons,
                                                     WindOperator operator) {
        ReconciliationSourceItem reference = references.isEmpty() ? null : references.getFirst();
        ReconciliationSourceItem comparison = comparisons.isEmpty() ? null : comparisons.getFirst();
        ReconciliationMatchResultKind kind = classify(references, comparisons);
        List<String> evidenceRefs = java.util.stream.Stream.concat(references.stream(), comparisons.stream())
                .flatMap(item -> parseRefs(item.getEvidenceRefs()).stream())
                .distinct()
                .sorted()
                .toList();
        ReconciliationMatchResult result = new ReconciliationMatchResult();
        result.setSn(TemporalSequenceFactory.hourNext(MATCH_RESULT_SEQUENCE_TYPE));
        result.setTenantId(batch.getTenantId());
        result.setReconciliationBatchSn(batch.getSn());
        setFactRef(result, reference, true);
        setFactRef(result, comparison, false);
        ReconciliationSourceItem identitySource = reference == null ? comparison : reference;
        result.setComparisonOwnerNamespace(identitySource.getComparisonOwnerNamespace());
        result.setComparisonIdentityValue(identitySource.getComparisonIdentityValue());
        result.setResultKind(kind);
        if (kind == ReconciliationMatchResultKind.MONEY_MISMATCH) {
            result.setAbsoluteDifferenceCurrency(reference.getCurrency());
            result.setAbsoluteDifferenceAmount(Math.absExact(Math.subtractExact(reference.getAmount(), comparison.getAmount())));
            result.setLargerSide(reference.getAmount() > comparison.getAmount()
                    ? ReconciliationSourceRole.REFERENCE : ReconciliationSourceRole.COMPARISON);
        }
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        result.setMatchIdentityDigest(matchIdentityDigest(result));
        result.setResultDigest(matchResultDigest(result));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private ReconciliationMatchResultKind classify(List<ReconciliationSourceItem> references,
                                                    List<ReconciliationSourceItem> comparisons) {
        if (references.size() > 1 || comparisons.size() > 1) {
            return ReconciliationMatchResultKind.IDENTITY_CONFLICT;
        }
        if (references.isEmpty()) {
            return ReconciliationMatchResultKind.REFERENCE_MISSING;
        }
        if (comparisons.isEmpty()) {
            return ReconciliationMatchResultKind.COMPARISON_MISSING;
        }
        ReconciliationSourceItem reference = references.getFirst();
        ReconciliationSourceItem comparison = comparisons.getFirst();
        if (!Boolean.TRUE.equals(reference.getComparisonProven())
                || !Boolean.TRUE.equals(comparison.getComparisonProven())) {
            return ReconciliationMatchResultKind.NOT_COMPARABLE;
        }
        if (!same(reference, comparison, ReconciliationSourceItem::getRuleNamespace)
                || !same(reference, comparison, ReconciliationSourceItem::getRuleIdentity)
                || !same(reference, comparison, ReconciliationSourceItem::getRuleVersion)) {
            return ReconciliationMatchResultKind.RULE_MISMATCH;
        }
        if (reference.getCurrency() != comparison.getCurrency()) {
            return ReconciliationMatchResultKind.CURRENCY_MISMATCH;
        }
        if (!Objects.equals(reference.getAmount(), comparison.getAmount())) {
            return ReconciliationMatchResultKind.MONEY_MISMATCH;
        }
        if (!same(reference, comparison, ReconciliationSourceItem::getComparisonStatusCode)) {
            return ReconciliationMatchResultKind.STATUS_MISMATCH;
        }
        if (!same(reference, comparison, ReconciliationSourceItem::getClaimKind)
                || !same(reference, comparison, ReconciliationSourceItem::getEconomicComponent)
                || !same(reference, comparison, ReconciliationSourceItem::getDirection)
                || !same(reference, comparison, ReconciliationSourceItem::getNormalizationVersion)) {
            return ReconciliationMatchResultKind.SEMANTICS_MISMATCH;
        }
        return ReconciliationMatchResultKind.MATCHED;
    }

    private boolean same(ReconciliationSourceItem left,
                         ReconciliationSourceItem right,
                         Function<ReconciliationSourceItem, Object> extractor) {
        return Objects.equals(extractor.apply(left), extractor.apply(right));
    }

    private ReconciliationRunResult toRunResult(ReconciliationBatch batch,
                                                SourceSet sources,
                                                List<ReconciliationMatchResult> matches,
                                                WindOperator operator) {
        int matchedCount = (int) matches.stream()
                .filter(match -> match.getResultKind() == ReconciliationMatchResultKind.MATCHED)
                .count();
        int differenceCount = matches.size() - matchedCount;
        boolean coverageComplete = Boolean.TRUE.equals(sources.reference().snapshot().getCoverageComplete())
                && Boolean.TRUE.equals(sources.comparison().snapshot().getCoverageComplete());
        ReconciliationRunOutcome outcome = coverageComplete && differenceCount == 0
                ? ReconciliationRunOutcome.BALANCED : ReconciliationRunOutcome.DIFFERENCE_FOUND;
        LinkedHashSet<String> evidenceRefs = new LinkedHashSet<>();
        evidenceRefs.addAll(parseRefs(sources.reference().snapshot().getEvidenceRefs()));
        evidenceRefs.addAll(parseRefs(sources.comparison().snapshot().getEvidenceRefs()));
        matches.forEach(match -> evidenceRefs.addAll(parseRefs(match.getEvidenceRefs())));

        ReconciliationRunResult result = new ReconciliationRunResult();
        result.setSn(TemporalSequenceFactory.hourNext(RUN_RESULT_SEQUENCE_TYPE));
        result.setTenantId(batch.getTenantId());
        result.setReconciliationBatchSn(batch.getSn());
        result.setScopeOwnerNamespace(batch.getScopeOwnerNamespace());
        result.setScopeIdentityValue(batch.getScopeIdentityValue());
        result.setPairOwnerNamespace(batch.getPairOwnerNamespace());
        result.setPairIdentityValue(batch.getPairIdentityValue());
        result.setCurrency(batch.getCurrency());
        result.setOutcome(outcome);
        result.setRuleNamespace(batch.getRuleNamespace());
        result.setRuleIdentity(batch.getRuleIdentity());
        result.setRuleVersion(batch.getRuleVersion());
        result.setReferenceSnapshotSn(sources.reference().snapshot().getSn());
        result.setComparisonSnapshotSn(sources.comparison().snapshot().getSn());
        result.setReferenceSourceDigest(sources.reference().snapshot().getSourceDigest());
        result.setComparisonSourceDigest(sources.comparison().snapshot().getSourceDigest());
        result.setSourceDigest(FundsStableHashSupport.sha256Json(Map.of(
                "referenceSourceDigest", result.getReferenceSourceDigest(),
                "comparisonSourceDigest", result.getComparisonSourceDigest())));
        result.setTotalCount(matches.size());
        result.setMatchedCount(matchedCount);
        result.setDifferenceCount(differenceCount);
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs.stream().sorted().toList()));
        result.setResultDigest(runResultDigest(batch, result, matches));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private String runResultDigest(ReconciliationBatch batch,
                                   ReconciliationRunResult result,
                                   List<ReconciliationMatchResult> matches) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", batch.getTenantId());
        facts.put("batchSn", batch.getSn());
        facts.put("batchDigest", batch.getBatchDigest());
        facts.put("scope", batch.getScopeOwnerNamespace() + ":" + batch.getScopeIdentityValue());
        facts.put("pair", batch.getPairOwnerNamespace() + ":" + batch.getPairIdentityValue());
        facts.put("currency", batch.getCurrency());
        facts.put("rule", batch.getRuleNamespace() + ":" + batch.getRuleIdentity() + ":" + batch.getRuleVersion());
        facts.put("outcome", result.getOutcome());
        facts.put("referenceSourceDigest", result.getReferenceSourceDigest());
        facts.put("comparisonSourceDigest", result.getComparisonSourceDigest());
        facts.put("sourceDigest", result.getSourceDigest());
        facts.put("totalCount", result.getTotalCount());
        facts.put("matchedCount", result.getMatchedCount());
        facts.put("differenceCount", result.getDifferenceCount());
        facts.put("matchDigests", matches.stream().map(ReconciliationMatchResult::getResultDigest).sorted().toList());
        facts.put("evidenceRefs", parseRefs(result.getEvidenceRefs()));
        return FundsStableHashSupport.sha256Json(facts);
    }

    private TreeMap<String, Object> semanticFact(ReconciliationSourceItem item) {
        TreeMap<String, Object> value = new TreeMap<>();
        value.put("sourceFactRef", sourceFactKey(item));
        value.put("comparisonIdentity", comparisonIdentityKey(item));
        value.put("amount", item.getAmount());
        value.put("currency", item.getCurrency());
        value.put("rule", item.getRuleNamespace() + ":" + item.getRuleIdentity() + ":" + item.getRuleVersion());
        value.put("comparisonStatusCode", item.getComparisonStatusCode());
        value.put("comparisonProven", item.getComparisonProven());
        value.put("claimKind", item.getClaimKind());
        value.put("economicComponent", item.getEconomicComponent());
        value.put("direction", item.getDirection());
        value.put("normalizationVersion", item.getNormalizationVersion());
        return value;
    }

    private String snapshotDigest(ReconciliationSourceSnapshot snapshot, String semanticDigest) {
        TreeMap<String, Object> value = new TreeMap<>();
        value.put("tenantId", snapshot.getTenantId());
        value.put("batchSn", snapshot.getReconciliationBatchSn());
        value.put("sourceRole", snapshot.getSourceRole());
        value.put("sourceNamespace", snapshot.getSourceNamespace());
        value.put("snapshotIdentity", snapshot.getSnapshotOwnerNamespace() + ":" + snapshot.getSnapshotIdentityValue());
        value.put("snapshotVersion", snapshot.getSnapshotVersion());
        value.put("coverageComplete", snapshot.getCoverageComplete());
        value.put("coverageWatermark", snapshot.getCoverageWatermark());
        value.put("coverageMemberCount", snapshot.getCoverageMemberCount());
        value.put("semanticDigest", semanticDigest);
        return FundsStableHashSupport.sha256Json(value);
    }

    private String matchIdentityDigest(ReconciliationMatchResult result) {
        return FundsStableHashSupport.sha256Json(Map.of(
                "comparisonIdentity", result.getComparisonOwnerNamespace() + ":" + result.getComparisonIdentityValue()));
    }

    private String matchResultDigest(ReconciliationMatchResult result) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("referenceFact", nullableIdentity(result.getReferenceFactOwnerNamespace(),
                result.getReferenceFactIdentityValue()));
        facts.put("comparisonFact", nullableIdentity(result.getComparisonFactOwnerNamespace(),
                result.getComparisonFactIdentityValue()));
        facts.put("comparisonIdentity", result.getComparisonOwnerNamespace() + ":" + result.getComparisonIdentityValue());
        facts.put("resultKind", result.getResultKind());
        facts.put("absoluteDifferenceCurrency", result.getAbsoluteDifferenceCurrency());
        facts.put("absoluteDifferenceAmount", result.getAbsoluteDifferenceAmount());
        facts.put("largerSide", result.getLargerSide());
        facts.put("evidenceRefs", parseRefs(result.getEvidenceRefs()));
        return FundsStableHashSupport.sha256Json(facts);
    }

    private void setFactRef(ReconciliationMatchResult target,
                            ReconciliationSourceItem source,
                            boolean reference) {
        if (source == null) {
            return;
        }
        if (reference) {
            target.setReferenceFactOwnerNamespace(source.getSourceFactOwnerNamespace());
            target.setReferenceFactIdentityValue(source.getSourceFactIdentityValue());
        } else {
            target.setComparisonFactOwnerNamespace(source.getSourceFactOwnerNamespace());
            target.setComparisonFactIdentityValue(source.getSourceFactIdentityValue());
        }
    }

    private String sourceFactKey(ReconciliationSourceItem item) {
        return item.getSourceFactOwnerNamespace() + ":" + item.getSourceFactIdentityValue();
    }

    private String comparisonIdentityKey(ReconciliationSourceItem item) {
        return item.getComparisonOwnerNamespace() + ":" + item.getComparisonIdentityValue();
    }

    private String nullableIdentity(String ownerNamespace, String value) {
        return StringUtils.hasText(ownerNamespace) && StringUtils.hasText(value)
                ? ownerNamespace + ":" + value : null;
    }

    private List<String> parseRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    private void validateRequest(RecordReconciliationRunResultRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账运行结果请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账运行结果租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账运行结果 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账运行结果批次流水号不能为空");
        AssertUtils.notNull(operator, "对账运行结果操作人不能为空");
    }

    private void validateQueryIdentity(Long tenantId, String runResultSn) {
        AssertUtils.notNull(tenantId, "对账运行结果查询租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "对账运行结果查询 tenantId 与当前租户不一致");
        AssertUtils.hasText(runResultSn, "对账运行结果流水号不能为空");
    }

    private record SourceFacts(ReconciliationSourceSnapshot snapshot, List<ReconciliationSourceItem> items) {
    }

    private record SourceSet(SourceFacts reference, SourceFacts comparison) {
    }
}
