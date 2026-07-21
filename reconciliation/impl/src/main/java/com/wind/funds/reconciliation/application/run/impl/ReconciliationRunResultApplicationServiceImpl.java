package com.wind.funds.reconciliation.application.run.impl;

import com.alibaba.fastjson2.JSON;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationMatchResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.mapstruct.ReconciliationRunResultConverter;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
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

    private static final WindSequenceType RUN_RESULT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_RUN_RESULT", "RRR", 6);

    private static final WindSequenceType MATCH_RESULT_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_MATCH_RESULT", "RMR", 6);

    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationMatchResultMapper reconciliationMatchResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRunResultDTO recordRunResult(RecordReconciliationRunResultRequest request,
                                                       WindOperator operator) {
        validateRequest(request, operator);
        List<String> evidenceRefs = normalizedEvidenceRefs(request.getEvidenceRefs());
        String runResultSn = TemporalSequenceFactory.hourNext(RUN_RESULT_SEQUENCE_TYPE);
        List<ReconciliationMatchResult> matchResults = toMatchResults(request, operator, runResultSn);
        assertNoDuplicateMatchResult(matchResults);
        ReconciliationRunResult candidate = toEntity(request, operator, evidenceRefs, matchResults, runResultSn);
        ReconciliationRunResult existing = selectByBusinessKey(request);
        if (existing != null) {
            return reuseExisting(existing, candidate);
        }
        try {
            reconciliationRunResultMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ReconciliationRunResult winner = selectByBusinessKeyForUpdate(request);
            AssertUtils.notNull(winner, "对账运行结果唯一键冲突后未找到幂等结果");
            return reuseExisting(winner, candidate);
        }
        AssertUtils.notNull(candidate.getId(), "记录对账运行结果失败");
        matchResults.forEach(this::persistMatchResult);
        ReconciliationRunResult saved = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), candidate.getSn());
        AssertUtils.notNull(saved, "记录对账运行结果后未找到持久化事实");
        log.info("对账运行结果记录完成，tenantId = {}, reconciliationBatchSn = {}, gateObjectType = {}, gateObjectSn = {}, status = {}",
                request.getTenantId(), request.getReconciliationBatchSn(), request.getGateObjectType(),
                request.getGateObjectSn(), candidate.getStatus());
        return ReconciliationRunResultConverter.INSTANCE.toDTO(saved);
    }

    private ReconciliationRunResult selectByBusinessKey(RecordReconciliationRunResultRequest request) {
        return reconciliationRunResultMapper.selectByBusinessKey(request.getTenantId(),
                request.getReconciliationBatchSn(), request.getGateObjectType().name(), request.getGateObjectSn());
    }

    private ReconciliationRunResult selectByBusinessKeyForUpdate(RecordReconciliationRunResultRequest request) {
        return reconciliationRunResultMapper.selectByBusinessKeyForUpdate(request.getTenantId(),
                request.getReconciliationBatchSn(), request.getGateObjectType().name(), request.getGateObjectSn());
    }

    private void persistMatchResult(ReconciliationMatchResult matchResult) {
        reconciliationMatchResultMapper.insertSelective(matchResult);
        AssertUtils.notNull(matchResult.getId(), "记录对账逐笔匹配结果失败");
    }

    private ReconciliationRunResult toEntity(RecordReconciliationRunResultRequest request,
                                             WindOperator operator,
                                             List<String> evidenceRefs,
                                             List<ReconciliationMatchResult> matchResults,
                                             String runResultSn) {
        int matchedCount = (int) matchResults.stream().filter(this::isMatched).count();
        int differenceCount = matchResults.size() - matchedCount;
        ReconciliationRunResultStatus status = differenceCount == 0
                ? ReconciliationRunResultStatus.BALANCED
                : ReconciliationRunResultStatus.DIFFERENCE_FOUND;
        String sourceDigest = sourceDigest(request);
        List<String> matchDigests = matchResults.stream()
                .map(ReconciliationMatchResult::getMatchDigest)
                .sorted()
                .toList();
        ReconciliationRunResult result = new ReconciliationRunResult();
        result.setSn(runResultSn);
        result.setTenantId(request.getTenantId());
        result.setReconciliationBatchSn(request.getReconciliationBatchSn());
        result.setGateObjectType(request.getGateObjectType());
        result.setGateObjectSn(request.getGateObjectSn());
        result.setStatus(status);
        result.setRuleVersion(request.getRuleVersion());
        result.setInternalSourceDigest(request.getInternalSourceDigest());
        result.setExternalSourceDigest(request.getExternalSourceDigest());
        result.setSourceDigest(sourceDigest);
        result.setTotalCount(matchResults.size());
        result.setMatchedCount(matchedCount);
        result.setDifferenceCount(differenceCount);
        result.setEvidenceRefs(JSON.toJSONString(evidenceRefs));
        result.setResultDigest(resultDigest(request, evidenceRefs, sourceDigest, status,
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
        result.setInternalSourceRef(normalizedOptionalText(item.getInternalSourceRef()));
        result.setExternalSourceRef(normalizedOptionalText(item.getExternalSourceRef()));
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

    private String matchIdentityDigest(ReconciliationMatchResult result) {
        TreeMap<String, Object> identityFacts = new TreeMap<>();
        identityFacts.put("internalSourceRef", result.getInternalSourceRef());
        identityFacts.put("externalSourceRef", result.getExternalSourceRef());
        return FundsStableHashSupport.sha256Json(identityFacts);
    }

    private String matchDigest(ReconciliationMatchResult result) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("internalSourceRef", result.getInternalSourceRef());
        facts.put("externalSourceRef", result.getExternalSourceRef());
        facts.put("sourceQuality", result.getSourceQuality());
        facts.put("matchStrength", result.getMatchStrength());
        facts.put("differenceType", result.getDifferenceType());
        facts.put("severity", result.getSeverity());
        facts.put("currency", result.getCurrency());
        facts.put("differenceAmount", result.getDifferenceAmount());
        facts.put("evidenceRef", result.getEvidenceRef());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private String sourceDigest(RecordReconciliationRunResultRequest request) {
        TreeMap<String, Object> sourceFacts = new TreeMap<>();
        sourceFacts.put("internalSourceDigest", request.getInternalSourceDigest());
        sourceFacts.put("externalSourceDigest", request.getExternalSourceDigest());
        return FundsStableHashSupport.sha256Json(sourceFacts);
    }

    private String resultDigest(RecordReconciliationRunResultRequest request,
                                List<String> evidenceRefs,
                                String sourceDigest,
                                ReconciliationRunResultStatus status,
                                int matchedCount,
                                int differenceCount,
                                List<String> matchDigests) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", request.getTenantId());
        facts.put("reconciliationBatchSn", request.getReconciliationBatchSn());
        facts.put("gateObjectType", request.getGateObjectType());
        facts.put("gateObjectSn", request.getGateObjectSn());
        facts.put("status", status);
        facts.put("ruleVersion", request.getRuleVersion());
        facts.put("internalSourceDigest", request.getInternalSourceDigest());
        facts.put("externalSourceDigest", request.getExternalSourceDigest());
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
                "同一批次和准入对象的对账运行结果事实不一致，reconciliationBatchSn = {}, gateObjectSn = {}",
                existing.getReconciliationBatchSn(), existing.getGateObjectSn());
        return ReconciliationRunResultConverter.INSTANCE.toDTO(existing);
    }

    private void validateRequest(RecordReconciliationRunResultRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账运行结果请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账运行结果租户 ID 不能为空");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "对账运行结果批次流水号不能为空");
        AssertUtils.notNull(request.getGateObjectType(), "对账运行结果准入对象类型不能为空");
        AssertUtils.hasText(request.getGateObjectSn(), "对账运行结果准入对象流水号不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "对账运行结果规则版本不能为空");
        validateSourceDigest(request.getInternalSourceDigest(), "内部来源");
        validateSourceDigest(request.getExternalSourceDigest(), "外部来源");
        AssertUtils.notEmpty(request.getMatchResults(), "对账运行结果逐笔匹配结果不能为空");
        request.getMatchResults().forEach(this::validateMatchResult);
        AssertUtils.notEmpty(request.getEvidenceRefs(), "对账运行结果证据引用不能为空");
        AssertUtils.isTrue(request.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "对账运行结果证据引用不能包含空值");
        AssertUtils.notNull(operator, "对账运行结果操作人不能为空");
    }

    private void validateSourceDigest(String sourceDigest, String sourceName) {
        AssertUtils.isTrue(sourceDigest != null && SHA_256_PATTERN.matcher(sourceDigest).matches(),
                "对账运行结果{}摘要必须是 64 位小写 SHA-256", sourceName);
    }

    private void validateMatchResult(ReconciliationMatchResultItem item) {
        AssertUtils.notNull(item, "对账运行结果逐笔匹配项不能为空");
        AssertUtils.notNull(item.getSourceQuality(), "对账匹配结果来源质量不能为空");
        AssertUtils.notNull(item.getMatchStrength(), "对账匹配结果匹配强度不能为空");
        AssertUtils.hasText(item.getEvidenceRef(), "对账匹配结果证据引用不能为空");
        boolean hasInternalSource = StringUtils.hasText(item.getInternalSourceRef());
        boolean hasExternalSource = StringUtils.hasText(item.getExternalSourceRef());
        AssertUtils.isTrue(hasInternalSource || hasExternalSource, "对账匹配结果至少需要一侧来源引用");
        if (isAutomaticMatch(item.getMatchStrength())) {
            AssertUtils.isTrue(item.getSourceQuality() == ReconciliationSourceQuality.VERIFIED,
                    "自动对平的来源必须已验证");
            AssertUtils.isTrue(hasInternalSource && hasExternalSource, "自动对平必须同时存在内部和外部来源引用");
            AssertUtils.isTrue(item.getDifferenceType() == null && item.getSeverity() == null
                            && item.getCurrency() == null && item.getDifferenceAmount() == null,
                    "自动对平项不能包含差错字段");
            return;
        }
        AssertUtils.notNull(item.getDifferenceType(), "未自动对平项必须填写差错类型");
        AssertUtils.notNull(item.getSeverity(), "未自动对平项必须填写差错严重等级");
        validateDifferenceSourceRefs(item.getDifferenceType(), hasInternalSource, hasExternalSource);
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

    private void validateDifferenceSourceRefs(ReconciliationDifferenceType differenceType,
                                              boolean hasInternalSource,
                                              boolean hasExternalSource) {
        if (differenceType == ReconciliationDifferenceType.INTERNAL_MISSING) {
            AssertUtils.isTrue(!hasInternalSource && hasExternalSource,
                    "INTERNAL_MISSING 只能缺少内部来源引用");
            return;
        }
        if (differenceType == ReconciliationDifferenceType.EXTERNAL_MISSING) {
            AssertUtils.isTrue(hasInternalSource && !hasExternalSource,
                    "EXTERNAL_MISSING 只能缺少外部来源引用");
            return;
        }
        AssertUtils.isTrue(hasInternalSource && hasExternalSource,
                "非缺失类差错必须同时存在内部和外部来源引用");
    }

    private void assertNoDuplicateMatchResult(List<ReconciliationMatchResult> matchResults) {
        long distinctIdentityCount = matchResults.stream()
                .map(ReconciliationMatchResult::getMatchIdentityDigest)
                .distinct()
                .count();
        AssertUtils.isTrue(distinctIdentityCount == matchResults.size(),
                "对账运行结果不能重复使用同一内部和外部来源对");
    }

    private boolean isMatched(ReconciliationMatchResult result) {
        return result.getSourceQuality() == ReconciliationSourceQuality.VERIFIED
                && isAutomaticMatch(result.getMatchStrength());
    }

    private boolean isAutomaticMatch(ReconciliationMatchStrength matchStrength) {
        return matchStrength == ReconciliationMatchStrength.EXACT_MATCH
                || matchStrength == ReconciliationMatchStrength.RULE_MATCH;
    }

    private String normalizedOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> normalizedEvidenceRefs(List<String> evidenceRefs) {
        return evidenceRefs.stream().map(String::trim).distinct().sorted().toList();
    }
}
