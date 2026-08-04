package com.wind.funds.reconciliation.application.run.impl;

import com.wind.jackson.WindJson;
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
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.mapstruct.ReconciliationRunResultConverter;
import com.wind.funds.reconciliation.mapstruct.ReconciliationMatchResultConverter;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.support.ReconciliationDigestSupport;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * 对账运行结果应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationRunResultApplicationServiceImpl
        implements ReconciliationRunResultApplicationService {

    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

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
    public ReconciliationRunResultDTO recordRunResult(RecordReconciliationRunResultRequest request,
                                                       WindOperator operator) {
        validateRequest(request, operator);
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), request.getReconciliationBatchSn());
        AssertUtils.notNull(batch, "对账批次不存在，reconciliationBatchSn = {}", request.getReconciliationBatchSn());
        AssertUtils.isTrue(batch.getStatus() == ReconciliationBatchStatus.DATA_READY
                        || batch.getStatus() == ReconciliationBatchStatus.COMPLETED,
                "对账批次来源尚未冻结完整，reconciliationBatchSn = {}, status = {}",
                batch.getSn(), batch.getStatus());
        SourceSet sourceSet = loadSourceSet(batch);
        String runResultSn = TemporalSequenceFactory.hourNext(RUN_RESULT_SEQUENCE_TYPE);
        List<ReconciliationMatchResult> matchResults = toMatchResults(request, operator, runResultSn);
        assertNoDuplicateMatchResult(matchResults);
        assertCompleteCoverage(sourceSet, matchResults);
        List<String> evidenceRefs = sourceSet.evidenceRefs();
        ReconciliationRunResult candidate = toEntity(
                request, batch, operator, evidenceRefs, sourceSet, matchResults, runResultSn);
        ReconciliationRunResult existing = reconciliationRunResultMapper.selectByBatch(
                request.getTenantId(), request.getReconciliationBatchSn());
        if (existing != null) {
            AssertUtils.isTrue(batch.getStatus() == ReconciliationBatchStatus.COMPLETED
                            && Objects.equals(batch.getRunResultSn(), existing.getSn()),
                    "对账批次与运行结果绑定不一致，reconciliationBatchSn = {}", batch.getSn());
            return reuseExisting(existing, candidate);
        }
        AssertUtils.isTrue(batch.getStatus() == ReconciliationBatchStatus.DATA_READY,
                "已完成对账批次缺少运行结果，reconciliationBatchSn = {}", batch.getSn());
        reconciliationRunResultMapper.insertSelective(candidate);
        AssertUtils.notNull(candidate.getId(), "记录对账运行结果失败");
        matchResults.forEach(this::persistMatchResult);
        AssertUtils.isTrue(reconciliationBatchMapper.complete(
                        batch.getTenantId(), batch.getSn(), candidate.getSn()) == 1,
                "完成对账批次失败，reconciliationBatchSn = {}", batch.getSn());
        ReconciliationRunResult saved = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), candidate.getSn());
        AssertUtils.notNull(saved, "记录对账运行结果后未找到持久化事实");
        log.info("对账运行结果记录完成，tenantId = {}, reconciliationBatchSn = {}, "
                        + "reconciliationScopeRef = {}, gateObjectType = {}, gateObjectSn = {}, status = {}",
                batch.getTenantId(), batch.getSn(), batch.getReconciliationScopeRef(),
                batch.getGateObjectType(), batch.getGateObjectSn(), candidate.getStatus());
        return ReconciliationRunResultConverter.INSTANCE.toDTO(saved);
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

    private void validateQueryIdentity(Long tenantId, String runResultSn) {
        AssertUtils.notNull(tenantId, "对账运行结果查询租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "对账运行结果查询 tenantId 与当前租户不一致");
        AssertUtils.hasText(runResultSn, "对账运行结果流水号不能为空");
    }

    private SourceSet loadSourceSet(ReconciliationBatch batch) {
        SourceSnapshotFacts reference = loadSourceSnapshot(batch, ReconciliationSourceRole.REFERENCE);
        SourceSnapshotFacts comparison = loadSourceSnapshot(batch, ReconciliationSourceRole.COMPARISON);
        AssertUtils.isTrue(!reference.items().isEmpty() || !comparison.items().isEmpty(),
                "对账批次两侧来源不能同时为空，reconciliationBatchSn = {}", batch.getSn());
        LinkedHashSet<String> evidenceRefs = new LinkedHashSet<>();
        evidenceRefs.addAll(parseEvidenceRefs(reference.snapshot().getEvidenceRefs()));
        evidenceRefs.addAll(parseEvidenceRefs(comparison.snapshot().getEvidenceRefs()));
        return new SourceSet(reference, comparison, evidenceRefs.stream().sorted().toList());
    }

    private SourceSnapshotFacts loadSourceSnapshot(ReconciliationBatch batch, ReconciliationSourceRole sourceRole) {
        ReconciliationSourceSnapshot snapshot = reconciliationSourceSnapshotMapper.selectByBatchAndRole(
                batch.getTenantId(), batch.getSn(), sourceRole.name());
        AssertUtils.notNull(snapshot, "对账批次缺少{}来源快照，reconciliationBatchSn = {}",
                sourceRole.getDesc(), batch.getSn());
        List<ReconciliationSourceItem> items = reconciliationSourceItemMapper.selectBySnapshot(
                batch.getTenantId(), snapshot.getSn());
        AssertUtils.isTrue(items.size() == snapshot.getRecordCount(),
                "对账来源快照成员数不一致，sourceSnapshotSn = {}", snapshot.getSn());
        AssertUtils.isTrue(items.stream().allMatch(item -> isSha256(item.getContentDigest())),
                "对账来源成员内容摘要无效，sourceSnapshotSn = {}", snapshot.getSn());
        String sourceDigest = ReconciliationDigestSupport.sourceDigest(
                snapshot.getSourceRole(), snapshot.getSourceType(),
                sourceContentDigests(items));
        AssertUtils.isTrue(Objects.equals(sourceDigest, snapshot.getSourceDigest()),
                "对账来源快照摘要不一致，sourceSnapshotSn = {}", snapshot.getSn());
        return new SourceSnapshotFacts(snapshot, items);
    }

    private ReconciliationRunResult toEntity(RecordReconciliationRunResultRequest request,
                                              ReconciliationBatch batch,
                                              WindOperator operator,
                                              List<String> evidenceRefs,
                                              SourceSet sourceSet,
                                              List<ReconciliationMatchResult> matchResults,
                                              String runResultSn) {
        int matchedCount = (int) matchResults.stream().filter(this::isMatched).count();
        int differenceCount = matchResults.size() - matchedCount;
        ReconciliationRunResultStatus status = differenceCount == 0
                ? ReconciliationRunResultStatus.BALANCED
                : ReconciliationRunResultStatus.DIFFERENCE_FOUND;
        String sourceDigest = sourceDigest(sourceSet);
        List<String> matchDigests = matchResults.stream()
                .map(ReconciliationMatchResult::getMatchDigest)
                .sorted()
                .toList();
        ReconciliationRunResult result = new ReconciliationRunResult();
        result.setSn(runResultSn);
        result.setTenantId(request.getTenantId());
        result.setReconciliationBatchSn(batch.getSn());
        result.setReconciliationScopeRef(batch.getReconciliationScopeRef());
        result.setGateObjectType(batch.getGateObjectType());
        result.setGateObjectSn(batch.getGateObjectSn());
        result.setStatus(status);
        result.setRuleVersion(batch.getRuleVersion());
        result.setReferenceSourceDigest(sourceSet.reference().snapshot().getSourceDigest());
        result.setComparisonSourceDigest(sourceSet.comparison().snapshot().getSourceDigest());
        result.setSourceDigest(sourceDigest);
        result.setTotalCount(matchResults.size());
        result.setMatchedCount(matchedCount);
        result.setDifferenceCount(differenceCount);
        result.setEvidenceRefs(WindJson.toJsonString(evidenceRefs));
        result.setResultDigest(resultDigest(batch, evidenceRefs, sourceSet, sourceDigest, status,
                matchedCount, differenceCount, matchDigests));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private List<ReconciliationMatchResult> toMatchResults(RecordReconciliationRunResultRequest request,
                                                           WindOperator operator,
                                                           String runResultSn) {
        return request.getMatchResults().stream()
                .map(item -> toMatchResult(request, item, operator, runResultSn))
                .sorted(Comparator.comparing(ReconciliationMatchResult::getMatchDigest))
                .toList();
    }

    private ReconciliationMatchResult toMatchResult(RecordReconciliationRunResultRequest request,
                                                     ReconciliationMatchResultItem item,
                                                     WindOperator operator,
                                                     String runResultSn) {
        ReconciliationMatchResult result = new ReconciliationMatchResult();
        result.setSn(TemporalSequenceFactory.hourNext(MATCH_RESULT_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setReconciliationRunResultSn(runResultSn);
        result.setReconciliationBatchSn(request.getReconciliationBatchSn());
        result.setReferenceSourceRef(normalizedOptionalText(item.getReferenceSourceRef()));
        result.setComparisonSourceRef(normalizedOptionalText(item.getComparisonSourceRef()));
        result.setSourceQuality(item.getSourceQuality());
        result.setMatchStrength(item.getMatchStrength());
        result.setDifferenceType(item.getDifferenceType());
        result.setSeverity(item.getSeverity());
        result.setCurrency(item.getCurrency());
        result.setDifferenceAmount(item.getDifferenceAmount());
        result.setEvidenceRef(item.getEvidenceRef().trim());
        result.setCreatedBy(operator.getOperatorAsText());
        result.setMatchIdentityDigest(matchIdentityDigest(result));
        result.setMatchDigest(matchDigest(result));
        return result;
    }

    private void persistMatchResult(ReconciliationMatchResult matchResult) {
        reconciliationMatchResultMapper.insertSelective(matchResult);
        AssertUtils.notNull(matchResult.getId(), "记录对账逐笔匹配结果失败");
    }

    private void assertCompleteCoverage(SourceSet sourceSet, List<ReconciliationMatchResult> matchResults) {
        Set<String> expectedReferenceRefs = sourceSet.reference().sourceItemRefs();
        Set<String> expectedComparisonRefs = sourceSet.comparison().sourceItemRefs();
        Set<String> actualReferenceRefs = matchResults.stream()
                .map(ReconciliationMatchResult::getReferenceSourceRef)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> actualComparisonRefs = matchResults.stream()
                .map(ReconciliationMatchResult::getComparisonSourceRef)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
        AssertUtils.isTrue(expectedReferenceRefs.containsAll(actualReferenceRefs),
                "对账匹配结果包含未冻结的基准侧来源引用");
        AssertUtils.isTrue(expectedComparisonRefs.containsAll(actualComparisonRefs),
                "对账匹配结果包含未冻结的核对侧来源引用");
        AssertUtils.isTrue(actualReferenceRefs.containsAll(expectedReferenceRefs),
                "对账匹配结果未覆盖全部基准侧来源事实");
        AssertUtils.isTrue(actualComparisonRefs.containsAll(expectedComparisonRefs),
                "对账匹配结果未覆盖全部核对侧来源事实");
    }

    private String matchIdentityDigest(ReconciliationMatchResult result) {
        TreeMap<String, Object> identityFacts = new TreeMap<>();
        identityFacts.put("referenceSourceRef", result.getReferenceSourceRef());
        identityFacts.put("comparisonSourceRef", result.getComparisonSourceRef());
        return FundsStableHashSupport.sha256Json(identityFacts);
    }

    private String matchDigest(ReconciliationMatchResult result) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("referenceSourceRef", result.getReferenceSourceRef());
        facts.put("comparisonSourceRef", result.getComparisonSourceRef());
        facts.put("sourceQuality", result.getSourceQuality());
        facts.put("matchStrength", result.getMatchStrength());
        facts.put("differenceType", result.getDifferenceType());
        facts.put("severity", result.getSeverity());
        facts.put("currency", result.getCurrency());
        facts.put("differenceAmount", result.getDifferenceAmount());
        facts.put("evidenceRef", result.getEvidenceRef());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private String sourceDigest(SourceSet sourceSet) {
        TreeMap<String, Object> sourceFacts = new TreeMap<>();
        sourceFacts.put("referenceSourceDigest", sourceSet.reference().snapshot().getSourceDigest());
        sourceFacts.put("comparisonSourceDigest", sourceSet.comparison().snapshot().getSourceDigest());
        return FundsStableHashSupport.sha256Json(sourceFacts);
    }

    private String resultDigest(ReconciliationBatch batch,
                                List<String> evidenceRefs,
                                SourceSet sourceSet,
                                String sourceDigest,
                                ReconciliationRunResultStatus status,
                                int matchedCount,
                                int differenceCount,
                                List<String> matchDigests) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", batch.getTenantId());
        facts.put("reconciliationBatchSn", batch.getSn());
        facts.put("batchDigest", batch.getBatchDigest());
        facts.put("reconciliationScopeRef", batch.getReconciliationScopeRef());
        facts.put("gateObjectType", batch.getGateObjectType());
        facts.put("gateObjectSn", batch.getGateObjectSn());
        facts.put("status", status);
        facts.put("ruleVersion", batch.getRuleVersion());
        facts.put("referenceSourceDigest", sourceSet.reference().snapshot().getSourceDigest());
        facts.put("comparisonSourceDigest", sourceSet.comparison().snapshot().getSourceDigest());
        facts.put("sourceDigest", sourceDigest);
        facts.put("totalCount", matchDigests.size());
        facts.put("matchedCount", matchedCount);
        facts.put("differenceCount", differenceCount);
        facts.put("matchDigests", matchDigests);
        facts.put("evidenceRefs", evidenceRefs);
        return FundsStableHashSupport.sha256Json(facts);
    }

    private ReconciliationRunResultDTO reuseExisting(ReconciliationRunResult existing,
                                                      ReconciliationRunResult candidate) {
        AssertUtils.isTrue(existing.getResultDigest().equals(candidate.getResultDigest()),
                "同一批次的对账运行结果事实不一致，reconciliationBatchSn = {}",
                existing.getReconciliationBatchSn());
        return ReconciliationRunResultConverter.INSTANCE.toDTO(existing);
    }

    private void validateRequest(RecordReconciliationRunResultRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账运行结果请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账运行结果租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账运行结果 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账运行结果批次流水号不能为空");
        AssertUtils.notNull(request.getMatchResults(), "对账运行结果逐笔匹配结果列表不能为空");
        AssertUtils.isTrue(request.getMatchResults().size()
                        <= RecordReconciliationRunResultRequest.MAX_MATCH_RESULT_COUNT,
                "对账运行结果逐笔匹配结果数量不能超过 {}",
                RecordReconciliationRunResultRequest.MAX_MATCH_RESULT_COUNT);
        request.getMatchResults().forEach(this::validateMatchResult);
        AssertUtils.notNull(operator, "对账运行结果操作人不能为空");
    }

    private void validateMatchResult(ReconciliationMatchResultItem item) {
        AssertUtils.notNull(item, "对账运行结果逐笔匹配项不能为空");
        AssertUtils.notNull(item.getSourceQuality(), "对账匹配结果来源质量不能为空");
        AssertUtils.notNull(item.getMatchStrength(), "对账匹配结果匹配强度不能为空");
        AssertUtils.hasText(item.getEvidenceRef(), "对账匹配结果证据引用不能为空");
        AssertUtils.isTrue(item.getEvidenceRef().trim().length()
                        <= ReconciliationMatchResultItem.MAX_EVIDENCE_REF_LENGTH,
                "对账匹配结果证据引用长度不能超过 {}",
                ReconciliationMatchResultItem.MAX_EVIDENCE_REF_LENGTH);
        assertOptionalSourceRefLength(item.getReferenceSourceRef(), "基准侧");
        assertOptionalSourceRefLength(item.getComparisonSourceRef(), "核对侧");
        boolean hasReferenceSource = StringUtils.hasText(item.getReferenceSourceRef());
        boolean hasComparisonSource = StringUtils.hasText(item.getComparisonSourceRef());
        AssertUtils.isTrue(hasReferenceSource || hasComparisonSource, "对账匹配结果至少需要一侧来源引用");
        if (isAutomaticMatch(item.getMatchStrength())) {
            AssertUtils.isTrue(item.getSourceQuality() == ReconciliationSourceQuality.VERIFIED,
                    "自动对平的来源必须已验证");
            AssertUtils.isTrue(hasReferenceSource && hasComparisonSource,
                    "自动对平必须同时存在基准侧和核对侧来源引用");
            AssertUtils.isTrue(item.getDifferenceType() == null && item.getSeverity() == null
                            && item.getCurrency() == null && item.getDifferenceAmount() == null,
                    "自动对平项不能包含差错字段");
            return;
        }
        AssertUtils.notNull(item.getDifferenceType(), "未自动对平项必须填写差错类型");
        AssertUtils.notNull(item.getSeverity(), "未自动对平项必须填写差错严重等级");
        validateDifferenceSourceRefs(item.getDifferenceType(), hasReferenceSource, hasComparisonSource);
        if (item.getDifferenceAmount() != null) {
            AssertUtils.isTrue(item.getDifferenceAmount() >= 0, "对账匹配结果差异金额不能小于 0");
            AssertUtils.isTrue(item.getDifferenceAmount() == 0 || item.getCurrency() != null,
                    "存在差异金额时必须填写币种");
        }
        if (item.getDifferenceType() == ReconciliationDifferenceType.AMOUNT_MISMATCH) {
            AssertUtils.isTrue(item.getCurrency() != null && item.getDifferenceAmount() != null
                            && item.getDifferenceAmount() > 0,
                    "金额差异必须填写币种和大于 0 的差异金额");
        }
    }

    private void assertOptionalSourceRefLength(String sourceRef, String sourceRole) {
        if (!StringUtils.hasText(sourceRef)) {
            return;
        }
        AssertUtils.isTrue(sourceRef.trim().length() <= ReconciliationMatchResultItem.MAX_SOURCE_REF_LENGTH,
                "对账匹配结果{}来源引用长度不能超过 {}",
                sourceRole, ReconciliationMatchResultItem.MAX_SOURCE_REF_LENGTH);
    }

    private void validateDifferenceSourceRefs(ReconciliationDifferenceType differenceType,
                                              boolean hasReferenceSource,
                                              boolean hasComparisonSource) {
        if (differenceType == ReconciliationDifferenceType.REFERENCE_MISSING) {
            AssertUtils.isTrue(!hasReferenceSource && hasComparisonSource,
                    "REFERENCE_MISSING 只能缺少基准侧来源引用");
            return;
        }
        if (differenceType == ReconciliationDifferenceType.COMPARISON_MISSING) {
            AssertUtils.isTrue(hasReferenceSource && !hasComparisonSource,
                    "COMPARISON_MISSING 只能缺少核对侧来源引用");
            return;
        }
        AssertUtils.isTrue(hasReferenceSource && hasComparisonSource,
                "非缺失类差错必须同时存在基准侧和核对侧来源引用");
    }

    private void assertNoDuplicateMatchResult(List<ReconciliationMatchResult> matchResults) {
        long distinctIdentityCount = matchResults.stream()
                .map(ReconciliationMatchResult::getMatchIdentityDigest)
                .distinct()
                .count();
        AssertUtils.isTrue(distinctIdentityCount == matchResults.size(),
                "对账运行结果不能重复使用同一基准侧和核对侧来源对");
        assertOneToOneSourceUsage(matchResults, ReconciliationMatchResult::getReferenceSourceRef,
                "同一基准侧来源引用只能参与一个匹配结果");
        assertOneToOneSourceUsage(matchResults, ReconciliationMatchResult::getComparisonSourceRef,
                "同一核对侧来源引用只能参与一个匹配结果");
    }

    private void assertOneToOneSourceUsage(List<ReconciliationMatchResult> matchResults,
                                           java.util.function.Function<ReconciliationMatchResult, String> extractor,
                                           String message) {
        long sourceCount = matchResults.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .count();
        long distinctSourceCount = matchResults.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        AssertUtils.isTrue(sourceCount == distinctSourceCount, message);
    }

    private boolean isMatched(ReconciliationMatchResult result) {
        return result.getSourceQuality() == ReconciliationSourceQuality.VERIFIED
                && isAutomaticMatch(result.getMatchStrength());
    }

    private boolean isAutomaticMatch(ReconciliationMatchStrength matchStrength) {
        return matchStrength == ReconciliationMatchStrength.EXACT_MATCH
                || matchStrength == ReconciliationMatchStrength.RULE_MATCH;
    }

    private TreeMap<String, String> sourceContentDigests(List<ReconciliationSourceItem> items) {
        TreeMap<String, String> result = new TreeMap<>();
        items.forEach(item -> result.put(item.getSourceItemRef(), item.getContentDigest()));
        return result;
    }

    private boolean isSha256(String digest) {
        return digest != null && SHA_256_PATTERN.matcher(digest).matches();
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    private String normalizedOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record SourceSnapshotFacts(ReconciliationSourceSnapshot snapshot,
                                       List<ReconciliationSourceItem> items) {

        private Set<String> sourceItemRefs() {
            return items.stream().map(ReconciliationSourceItem::getSourceItemRef)
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private record SourceSet(SourceSnapshotFacts reference,
                             SourceSnapshotFacts comparison,
                             List<String> evidenceRefs) {
    }
}
