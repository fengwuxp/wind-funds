package com.wind.funds.reconciliation.application.clearing.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.wind.funds.reconciliation.application.clearing.ClearingSplittableDetailApplicationService;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import com.wind.funds.reconciliation.dal.mapper.ClearingSplittableDetailMapper;
import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.mapstruct.ClearingSplittableDetailConverter;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.funds.transaction.model.dto.FundsActionRecordedEvidenceDTO;
import com.wind.funds.transaction.services.FundsActionRecordedEvidenceQueryService;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.jackson.WindJson;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import tools.jackson.databind.JsonNode;

/**
 * 可清分明细准入应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ClearingSplittableDetailApplicationServiceImpl
        implements ClearingSplittableDetailApplicationService {

    private static final WindSequenceType SPLITTABLE_DETAIL_SEQUENCE_TYPE =
            WindSequenceType.immutable("CLEARING_SPLITTABLE_DETAIL", "CSD", 6);

    private static final String RECORDED_REFERENCE_DIGEST_DOMAIN = "transaction.action.recorded-reference";

    private final ClearingSplittableDetailMapper clearingSplittableDetailMapper;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final FundsActionRecordedEvidenceQueryService fundsActionRecordedEvidenceQueryService;

    private final LedgerTransactionService ledgerTransactionService;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplittableDetailDTO identifySplittableDetail(IdentifyClearingSplittableDetailRequest request,
                                                               WindOperator operator) {
        validateRequest(request, operator);
        FundsActionRecordedEvidenceDTO evidence = requiredRecordedEvidence(request);
        FundsTransactionDTO transaction = fundsTransactionQueryService
                .findFundsTransactionBySn(request.getTenantId(), evidence.getActionFact().getIntentRef())
                .orElse(null);
        AssertUtils.notNull(transaction, "可清分来源资金交易不存在，fundsTransactionSn = {}",
                evidence.getActionFact().getIntentRef());
        LedgerTransactionDTO ledgerTransaction = ledgerTransactionService.getLedgerTransactionBySn(
                request.getTenantId(), evidence.getRecordedLedgerTransactionSn());
        List<LedgerEntryDTO> ledgerEntries = exactLedgerEntries(
                request.getTenantId(), evidence.getRecordedLedgerTransactionSn());
        AssertUtils.isTrue(recordedReferenceDigestMatches(evidence),
                "可清分来源 ActionFact recorded digest 不一致");
        ResolvedClearingSource source = resolveClearingSource(evidence, transaction, ledgerEntries);
        ReconciliationGateDecisionDTO reconciliationDecision = checkReconciliationGate(request, operator);
        ClearingSplittableDetail candidate = toCandidate(request, operator, evidence, transaction,
                source.detail(), source.entry(), ledgerTransaction, reconciliationDecision);
        if (candidate.getExclusionReason() == ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED) {
            log.info("可清分明细被对账 Gate 临时阻断，tenantId = {}, fundsTransactionSn = {}, ledgerEntrySn = {}",
                    request.getTenantId(), transaction.getSn(), source.entry().getSn());
            return ClearingSplittableDetailConverter.INSTANCE.convertToClearingSplittableDetailDTO(candidate);
        }

        ClearingSplittableDetail existing = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                request.getTenantId(), source.entry().getSn());
        if (existing != null) {
            return reuseExisting(existing, candidate);
        }
        try {
            clearingSplittableDetailMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ClearingSplittableDetail winner = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                    request.getTenantId(), source.entry().getSn());
            AssertUtils.notNull(winner, "可清分明细唯一键冲突后未找到幂等结果");
            return reuseExisting(winner, candidate);
        }
        AssertUtils.notNull(candidate.getId(), "创建可清分明细准入结果失败");
        ClearingSplittableDetail saved = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                request.getTenantId(), source.entry().getSn());
        log.info("可清分明细准入完成，tenantId = {}, fundsTransactionSn = {}, ledgerEntrySn = {}, admissionResult = {}, exclusionReason = {}",
                request.getTenantId(), transaction.getSn(), source.entry().getSn(),
                candidate.getAdmissionResult(), candidate.getExclusionReason());
        return ClearingSplittableDetailConverter.INSTANCE.convertToClearingSplittableDetailDTO(saved);
    }

    private FundsActionRecordedEvidenceDTO requiredRecordedEvidence(IdentifyClearingSplittableDetailRequest request) {
        FundsActionFactRef actionFactRef = new FundsActionFactRef(
                request.getTenantId(), request.getSourceActionFactRef().getValue());
        FundsActionRecordedEvidenceDTO result = fundsActionRecordedEvidenceQueryService
                .findRecordedEvidence(actionFactRef)
                .orElse(null);
        AssertUtils.notNull(result, "可清分来源资金动作不存在、不支持或记录不完整，sourceActionFactRef = {}",
                request.getSourceActionFactRef().getValue());
        return result;
    }

    private List<LedgerEntryDTO> exactLedgerEntries(Long tenantId, String ledgerTransactionSn) {
        var countPage = ledgerTransactionService.queryLedgerEntries(
                new com.wind.funds.ledger.query.LedgerEntryQuery()
                        .setTenantId(tenantId)
                        .setLedgerTransactionSn(ledgerTransactionSn),
                DefaultPageQueryOptions.defaults(1));
        long count = countPage.getTotal();
        AssertUtils.isTrue(count > 0 && count <= Integer.MAX_VALUE,
                "可清分来源账本分录数量非法，ledgerTransactionSn = {}, count = {}",
                ledgerTransactionSn, count);
        var exactPage = ledgerTransactionService.queryLedgerEntries(
                new com.wind.funds.ledger.query.LedgerEntryQuery()
                        .setTenantId(tenantId)
                        .setLedgerTransactionSn(ledgerTransactionSn),
                DefaultPageQueryOptions.defaults(Math.toIntExact(count)));
        AssertUtils.isTrue(exactPage.getTotal() == count && exactPage.getRecords().size() == count,
                "可清分来源账本分录读取不完整，ledgerTransactionSn = {}, expected = {}, actual = {}",
                ledgerTransactionSn, count, exactPage.getRecords().size());
        return List.copyOf(exactPage.getRecords());
    }

    private boolean recordedReferenceDigestMatches(FundsActionRecordedEvidenceDTO evidence) {
        if (evidence.getRecordedReferenceDigest() == null
                || !"SHA-256".equals(evidence.getRecordedReferenceDigest().getAlgorithm())
                || !"transaction.action.recorded-reference.v1"
                .equals(evidence.getRecordedReferenceDigest().getCoveredFieldsVersion())) {
            return false;
        }
        Map<String, Object> values = new TreeMap<>();
        values.put("actionSemanticDigest", evidence.getActionFact().getSemanticDigest().getValue());
        values.put("recordedLedgerTransactionSn", evidence.getRecordedLedgerTransactionSn());
        values.put("matchedSiblings", evidence.getMatchedSiblings().stream().map(sibling -> Map.of(
                "detailSn", sibling.getDetailSn(),
                "participantRole", sibling.getParticipantRole().name(),
                "subjectId", sibling.getSubjectId(),
                "subjectType", sibling.getSubjectType(),
                "amount", sibling.getMoney().getAmount(),
                "currency", sibling.getMoney().getCurrency().name(),
                "recordedLedgerTransactionSn", sibling.getRecordedLedgerTransactionSn())).toList());
        return FundsStableHashSupport.sha256CanonicalJson(RECORDED_REFERENCE_DIGEST_DOMAIN, values)
                .equals(evidence.getRecordedReferenceDigest().getValue());
    }

    private ResolvedClearingSource resolveClearingSource(
            FundsActionRecordedEvidenceDTO evidence,
            FundsTransactionDTO transaction,
            List<LedgerEntryDTO> ledgerEntries) {
        AssertUtils.equals(transaction.getTenantId(), evidence.getActionFact().getIdentity().getTenantId(),
                "可清分来源 ActionFact 租户不一致");
        AssertUtils.isTrue(StringUtils.hasText(evidence.getRecordedLedgerTransactionSn())
                        && evidence.getMatchedSiblings().size() >= 2
                        && evidence.getMatchedSiblings().size() <= 3
                        && evidence.getMatchedSiblings().stream()
                        .allMatch(sibling -> evidence.getRecordedLedgerTransactionSn()
                                .equals(sibling.getRecordedLedgerTransactionSn())),
                "可清分来源 ActionFact recorded sibling 不完整");
        AssertUtils.isTrue(evidence.getRecordedReferenceDigest() != null
                        && "SHA-256".equals(evidence.getRecordedReferenceDigest().getAlgorithm())
                        && StringUtils.hasText(evidence.getRecordedReferenceDigest().getValue())
                        && "transaction.action.recorded-reference.v1"
                        .equals(evidence.getRecordedReferenceDigest().getCoveredFieldsVersion()),
                "可清分来源 ActionFact recorded digest 无效");
        AssertUtils.isTrue(evidence.getMatchedSiblings().stream()
                        .filter(sibling -> sibling.getParticipantRole()
                                == com.wind.funds.route.enums.RouteParticipantRole.PAYER)
                        .count() == 1
                        && evidence.getMatchedSiblings().stream()
                        .filter(sibling -> sibling.getParticipantRole()
                                == com.wind.funds.route.enums.RouteParticipantRole.PAYEE)
                        .count() == 1
                        && evidence.getMatchedSiblings().stream()
                        .filter(sibling -> sibling.getParticipantRole()
                                == com.wind.funds.route.enums.RouteParticipantRole.FEE_RECEIVER)
                        .count() <= 1,
                "可清分来源 ActionFact sibling 角色不完整");

        Set<String> matchedEntrySns = new java.util.HashSet<>();
        LedgerEntryDTO clearingEntry = null;
        FundsActionRecordedEvidenceDTO.RecordedSiblingRef clearingSibling = null;
        for (FundsActionRecordedEvidenceDTO.RecordedSiblingRef sibling : evidence.getMatchedSiblings()) {
            List<LedgerEntryDTO> matches = ledgerEntries.stream()
                    .filter(entry -> siblingMatchesEntry(sibling, transaction, entry))
                    .toList();
            AssertUtils.isTrue(matches.size() == 1 && matchedEntrySns.add(matches.getFirst().getSn()),
                    "可清分来源 sibling 无唯一账本分录，detailSn = {}", sibling.getDetailSn());
            LedgerEntryDTO entry = matches.getFirst();
            AssertUtils.isTrue(StringUtils.hasText(entry.getPostingPlanSn())
                            && ledgerTransactionService.existsPostingPlan(
                            transaction.getTenantId(), entry.getPostingPlanSn(), entry.getLedgerTransactionSn()),
                    "可清分来源 sibling 记账计划不完整，detailSn = {}", sibling.getDetailSn());
            if (sibling.getParticipantRole() == com.wind.funds.route.enums.RouteParticipantRole.PAYEE
                    && isClearingCredit(entry)) {
                AssertUtils.isTrue(clearingEntry == null, "可清分来源存在多个 PAYEE/CLEARING credit");
                clearingSibling = sibling;
                clearingEntry = entry;
            }
        }
        AssertUtils.notNull(clearingSibling, "可清分来源缺少唯一 PAYEE/CLEARING credit");
        FundsTransactionDetailDTO detail = requiredDetail(
                transaction.getTenantId(), transaction.getSn(), clearingSibling.getDetailSn());
        return new ResolvedClearingSource(detail, clearingEntry);
    }

    private boolean siblingMatchesEntry(FundsActionRecordedEvidenceDTO.RecordedSiblingRef sibling,
                                        FundsTransactionDTO transaction,
                                        LedgerEntryDTO entry) {
        boolean expectedSide = sibling.getParticipantRole()
                == com.wind.funds.route.enums.RouteParticipantRole.PAYER
                ? entry.getEntryType() == EntrySide.DEBIT
                : entry.getEntryType() == EntrySide.CREDIT;
        return expectedSide
                && Objects.equals(entry.getTenantId(), transaction.getTenantId())
                && Objects.equals(entry.getFundsTransactionSn(), transaction.getSn())
                && Objects.equals(entry.getLedgerTransactionSn(), sibling.getRecordedLedgerTransactionSn())
                && Objects.equals(entry.getSubjectId(), sibling.getSubjectId())
                && Objects.equals(entry.getSubjectType(), sibling.getSubjectType())
                && Objects.equals(entry.getAmount(), sibling.getMoney());
    }

    private boolean isClearingCredit(LedgerEntryDTO entry) {
        return entry.getLedgerSubjectCode() == LedgerSubjectCode.CLEARING
                && entry.getEntryType() == EntrySide.CREDIT
                && entry.getPostingRole() == LedgerPostingRole.DETAIL
                && (entry.getBalanceEffectType() == LedgerBalanceEffectType.INCREASE
                || entry.getBalanceEffectType() == LedgerBalanceEffectType.CONSUME);
    }

    private FundsTransactionDetailDTO requiredDetail(Long tenantId, String transactionSn, String detailSn) {
        FundsTransactionDetailDTO result = fundsTransactionQueryService
                .queryFundsTransactionDetails(tenantId, transactionSn)
                .stream()
                .filter(detail -> detailSn.equals(detail.getSn()))
                .findFirst()
                .orElse(null);
        AssertUtils.notNull(result, "可清分来源资金交易明细不存在，fundsTransactionDetailSn = {}",
                detailSn);
        return result;
    }

    private ReconciliationGateDecisionDTO checkReconciliationGate(IdentifyClearingSplittableDetailRequest request,
                                                                  WindOperator operator) {
        return reconciliationGateApplicationService.checkGate(new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setStageRef(new GateStageRef()
                        .setStageKind("CLEARING_SPLITTABLE_IDENTIFY")
                        .setStageIdentity(request.getSourceActionFactRef())), operator);
    }

    private ClearingSplittableDetail toCandidate(IdentifyClearingSplittableDetailRequest request,
                                                 WindOperator operator,
                                                 FundsActionRecordedEvidenceDTO evidence,
                                                 FundsTransactionDTO transaction,
                                                 FundsTransactionDetailDTO detail,
                                                 LedgerEntryDTO entry,
                                                 LedgerTransactionDTO ledgerTransaction,
                                                 ReconciliationGateDecisionDTO reconciliationDecision) {
        ClearingSplittableExclusionReason exclusionReason = resolveExclusionReason(request, transaction, detail,
                entry, ledgerTransaction, reconciliationDecision);
        ClearingSplittableDetail result = new ClearingSplittableDetail();
        if (exclusionReason != ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED) {
            result.setSn(TemporalSequenceFactory.hourNext(SPLITTABLE_DETAIL_SEQUENCE_TYPE));
        }
        result.setTenantId(request.getTenantId());
        result.setFundsTransactionSn(transaction.getSn());
        result.setFundsTransactionDetailSn(detail.getSn());
        result.setLedgerTransactionSn(entry.getLedgerTransactionSn());
        result.setPostingPlanSn(entry.getPostingPlanSn());
        result.setLedgerEntrySn(entry.getSn());
        result.setSubjectType(entry.getSubjectType());
        result.setSubjectId(entry.getSubjectId());
        result.setCurrency(entry.getAmount().getCurrency());
        result.setAmount(entry.getAmount().getAmount());
        result.setRefundAmount(defaultAmount(transaction.getRefundedAmount()));
        result.setBusinessLine(request.getBusinessLine());
        result.setSplitPeriod(request.getSplitPeriod());
        result.setSplitRuleCode(request.getSplitRuleCode());
        result.setSplitRuleVersion(request.getSplitRuleVersion());
        result.setAdmissionResult(exclusionReason == null
                ? ClearingSplittableAdmissionResult.SPLIT_READY
                : ClearingSplittableAdmissionResult.EXCLUDED);
        result.setExclusionReason(exclusionReason);
        result.setReconciliationDecisionResult(reconciliationDecision.getDecisionResult());
        if (reconciliationDecision.isPassed()) {
            AssertUtils.notEmpty(reconciliationDecision.getEvidenceRefs(), "Gate 通过时必须持有消费证据");
            result.setGateEvidenceRef(reconciliationDecision.getEvidenceRefs().getFirst());
        }
        result.setReconciliationEvidenceRefs(WindJson.toJsonString(reconciliationDecision.getEvidenceRefs()));
        if (StringUtils.hasText(transaction.getRouteSnapshot())) {
            result.setRouteSnapshotDigest(FundsStableHashSupport.sha256Json(
                    Map.of("routeSnapshot", transaction.getRouteSnapshot())));
        }
        result.setSourceDigest(sourceDigest(request, evidence, transaction, detail, entry, ledgerTransaction,
                reconciliationDecision));
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private ClearingSplittableExclusionReason resolveExclusionReason(
            IdentifyClearingSplittableDetailRequest request,
            FundsTransactionDTO transaction,
            FundsTransactionDetailDTO detail,
            LedgerEntryDTO entry,
            LedgerTransactionDTO ledgerTransaction,
            ReconciliationGateDecisionDTO reconciliationDecision) {
        if (!reconciliationDecision.isPassed()) {
            return ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED;
        }
        long refundedAmount = defaultAmount(transaction.getRefundedAmount());
        boolean partiallyRefunded = refundedAmount > 0
                && transaction.getState() == FundsTransactionState.OPEN;
        if (transaction.getState() != FundsTransactionState.CLOSED && !partiallyRefunded) {
            return ClearingSplittableExclusionReason.TRANSACTION_NOT_ELIGIBLE;
        }
        if (detail.getState() != FundsTransactionDetailState.SUCCEEDED) {
            return ClearingSplittableExclusionReason.TRANSACTION_DETAIL_NOT_SUCCEEDED;
        }
        if (!hasCompleteRouteSnapshot(transaction.getRouteSnapshot())
                || !StringUtils.hasText(entry.getPostingPlanSn())
                || !ledgerTransactionService.existsPostingPlan(request.getTenantId(), entry.getPostingPlanSn(),
                entry.getLedgerTransactionSn())) {
            return ClearingSplittableExclusionReason.SOURCE_FACT_INCOMPLETE;
        }
        if (!sourceFactsMatch(request, transaction, detail, entry, ledgerTransaction)) {
            return ClearingSplittableExclusionReason.SOURCE_FACT_MISMATCH;
        }
        if (entry.getLedgerSubjectCode() != LedgerSubjectCode.CLEARING) {
            return ClearingSplittableExclusionReason.LEDGER_ENTRY_NOT_CLEARING;
        }
        if (!isClearingInflowFact(transaction, detail, entry, ledgerTransaction)) {
            return ClearingSplittableExclusionReason.LEDGER_ENTRY_NOT_CLEARING_INFLOW;
        }
        if (refundedAmount > 0) {
            return ClearingSplittableExclusionReason.REFUND_EXISTS;
        }
        return null;
    }

    private boolean hasCompleteRouteSnapshot(String routeSnapshot) {
        if (!StringUtils.hasText(routeSnapshot)) {
            return false;
        }
        try {
            JsonNode snapshot = WindJson.parseObject(routeSnapshot, JsonNode.class);
            return snapshot != null
                    && StringUtils.hasText(snapshot.path("routeCode").asString())
                    && StringUtils.hasText(snapshot.path("routeVersion").asString())
                    && !snapshot.path("legs").isEmpty();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean sourceFactsMatch(IdentifyClearingSplittableDetailRequest request,
                                     FundsTransactionDTO transaction,
                                     FundsTransactionDetailDTO detail,
                                     LedgerEntryDTO entry,
                                     LedgerTransactionDTO ledgerTransaction) {
        return Objects.equals(request.getTenantId(), transaction.getTenantId())
                && Objects.equals(request.getTenantId(), detail.getTenantId())
                && Objects.equals(request.getTenantId(), entry.getTenantId())
                && Objects.equals(request.getTenantId(), ledgerTransaction.getTenantId())
                && Objects.equals(transaction.getSn(), detail.getTransactionSn())
                && Objects.equals(transaction.getSn(), entry.getFundsTransactionSn())
                && Objects.equals(transaction.getSn(), ledgerTransaction.getFundsTransactionSn())
                && transaction.getTransactionType() == detail.getTransactionType()
                && transaction.getTransactionType() == ledgerTransaction.getTransactionType()
                && detail.getEventType() == ledgerTransaction.getEventType()
                && Objects.equals(detail.getLedgerTransactionSn(), entry.getLedgerTransactionSn())
                && Objects.equals(entry.getLedgerTransactionSn(), ledgerTransaction.getSn())
                && Objects.equals(detail.getSubjectType(), entry.getSubjectType())
                && Objects.equals(detail.getSubjectId(), entry.getSubjectId())
                && Objects.equals(detail.getCurrency(), entry.getAmount().getCurrency())
                && Objects.equals(detail.getAmount(), entry.getAmount().getAmount())
                && Objects.equals(transaction.getBusinessScene(), detail.getBusinessScene())
                && Objects.equals(transaction.getBusinessSn(), detail.getBusinessSn())
                && Objects.equals(ledgerTransaction.getBusinessScene(), entry.getBusinessScene())
                && Objects.equals(ledgerTransaction.getBusinessSn(), entry.getBusinessSn());
    }

    private boolean isClearingInflowFact(FundsTransactionDTO transaction,
                                         FundsTransactionDetailDTO detail,
                                         LedgerEntryDTO entry,
                                         LedgerTransactionDTO ledgerTransaction) {
        return transaction.getTransactionType() == DefaultFundsTransactionType.PAY
                && detail.getTransactionType() == DefaultFundsTransactionType.PAY
                && ledgerTransaction.getTransactionType() == DefaultFundsTransactionType.PAY
                && transaction.getTransactionMode() == FundsTransactionMode.DIRECT
                && detail.getEventType() == FundsTransactionEventType.PAY
                && detail.getFundsEffectType() == FundsEffectType.DIRECT
                && ledgerTransaction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && entry.getLedgerSubjectCategory() == LedgerSubjectCategory.CLEARING
                && entry.getEntryType() == EntrySide.CREDIT
                && entry.getPostingRole() == LedgerPostingRole.DETAIL
                && (entry.getBalanceEffectType() == LedgerBalanceEffectType.INCREASE
                || entry.getBalanceEffectType() == LedgerBalanceEffectType.CONSUME);
    }

    private String sourceDigest(IdentifyClearingSplittableDetailRequest request,
                                FundsActionRecordedEvidenceDTO evidence,
                                FundsTransactionDTO transaction,
                                FundsTransactionDetailDTO detail,
                                LedgerEntryDTO entry,
                                LedgerTransactionDTO ledgerTransaction,
                                ReconciliationGateDecisionDTO reconciliationDecision) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", request.getTenantId());
        facts.put("sourceActionFactRef", request.getSourceActionFactRef());
        facts.put("recordedReferenceDigest", evidence.getRecordedReferenceDigest());
        facts.put("recordedSiblings", evidence.getMatchedSiblings());
        facts.put("fundsTransactionSn", transaction.getSn());
        facts.put("fundsTransactionStatus", transaction.getState());
        facts.put("fundsTransactionMode", transaction.getTransactionMode());
        facts.put("fundsTransactionType", transaction.getTransactionType());
        facts.put("fundsTransactionBusinessScene", transaction.getBusinessScene());
        facts.put("fundsTransactionBusinessSn", transaction.getBusinessSn());
        facts.put("fundsTransactionDetailSn", detail.getSn());
        facts.put("fundsTransactionDetailTransactionSn", detail.getTransactionSn());
        facts.put("fundsTransactionDetailStatus", detail.getState());
        facts.put("fundsTransactionDetailType", detail.getTransactionType());
        facts.put("fundsTransactionDetailEventType", detail.getEventType());
        facts.put("fundsTransactionDetailEffectType", detail.getFundsEffectType());
        facts.put("fundsTransactionDetailBusinessScene", detail.getBusinessScene());
        facts.put("fundsTransactionDetailBusinessSn", detail.getBusinessSn());
        facts.put("fundsTransactionDetailSubjectType", detail.getSubjectType());
        facts.put("fundsTransactionDetailSubjectId", detail.getSubjectId());
        facts.put("fundsTransactionDetailAmount", detail.getAmount());
        facts.put("fundsTransactionDetailCurrency", detail.getCurrency());
        facts.put("fundsTransactionDetailLedgerTransactionSn", detail.getLedgerTransactionSn());
        facts.put("ledgerTransactionSn", ledgerTransaction.getSn());
        facts.put("ledgerTransactionFundsTransactionSn", ledgerTransaction.getFundsTransactionSn());
        facts.put("ledgerTransactionInstructionType", ledgerTransaction.getInstructionType());
        facts.put("ledgerTransactionType", ledgerTransaction.getTransactionType());
        facts.put("ledgerTransactionEventType", ledgerTransaction.getEventType());
        facts.put("ledgerTransactionBusinessScene", ledgerTransaction.getBusinessScene());
        facts.put("ledgerTransactionBusinessSn", ledgerTransaction.getBusinessSn());
        facts.put("ledgerTransactionDigest", ledgerTransaction.getSha256());
        facts.put("postingPlanSn", entry.getPostingPlanSn());
        facts.put("ledgerEntrySn", entry.getSn());
        facts.put("ledgerEntryFundsTransactionSn", entry.getFundsTransactionSn());
        facts.put("ledgerEntryLedgerTransactionSn", entry.getLedgerTransactionSn());
        facts.put("ledgerEntryBusinessScene", entry.getBusinessScene());
        facts.put("ledgerEntryBusinessSn", entry.getBusinessSn());
        facts.put("ledgerEntryDigest", entry.getSha256());
        facts.put("ledgerSubjectCode", entry.getLedgerSubjectCode());
        facts.put("ledgerSubjectCategory", entry.getLedgerSubjectCategory());
        facts.put("entryType", entry.getEntryType());
        facts.put("postingRole", entry.getPostingRole());
        facts.put("balanceEffectType", entry.getBalanceEffectType());
        facts.put("phaseCode", entry.getPhaseCode());
        facts.put("intent", entry.getIntent());
        facts.put("routeSnapshot", transaction.getRouteSnapshot());
        facts.put("subjectType", entry.getSubjectType());
        facts.put("subjectId", entry.getSubjectId());
        facts.put("amount", entry.getAmount().getAmount());
        facts.put("currency", entry.getAmount().getCurrency());
        facts.put("refundedAmount", defaultAmount(transaction.getRefundedAmount()));
        facts.put("businessLine", request.getBusinessLine());
        facts.put("splitPeriod", request.getSplitPeriod());
        facts.put("splitRuleCode", request.getSplitRuleCode());
        facts.put("splitRuleVersion", request.getSplitRuleVersion());
        facts.put("reconciliationDecisionResult", reconciliationDecision.getDecisionResult());
        facts.put("gateStageRef", reconciliationDecision.getStageRef());
        facts.put("reconciliationEvidenceRefs", List.copyOf(reconciliationDecision.getEvidenceRefs()));
        return FundsStableHashSupport.sha256Json(facts);
    }

    private ClearingSplittableDetailDTO reuseExisting(ClearingSplittableDetail existing,
                                                       ClearingSplittableDetail candidate) {
        AssertUtils.isTrue(existing.getSourceDigest().equals(candidate.getSourceDigest()),
                "同一账本分录的可清分来源事实或规则已变化，ledgerEntrySn = {}", existing.getLedgerEntrySn());
        return ClearingSplittableDetailConverter.INSTANCE.convertToClearingSplittableDetailDTO(existing);
    }

    private void validateRequest(IdentifyClearingSplittableDetailRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "可清分明细识别请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "可清分明细租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "可清分明细 tenantId 与当前租户不一致");
        AssertUtils.notNull(request.getSourceActionFactRef(), "来源资金动作事实引用不能为空");
        AssertUtils.equals("funds", request.getSourceActionFactRef().getOwnerNamespace(),
                "来源资金动作事实 ownerNamespace 必须为 funds");
        AssertUtils.hasText(request.getSourceActionFactRef().getValue(), "来源资金动作事实稳定身份不能为空");
        AssertUtils.hasText(request.getBusinessLine(), "业务线不能为空");
        AssertUtils.hasText(request.getSplitPeriod(), "清分周期不能为空");
        AssertUtils.hasText(request.getSplitRuleCode(), "清分规则编码不能为空");
        AssertUtils.hasText(request.getSplitRuleVersion(), "清分规则版本不能为空");
        AssertUtils.notNull(operator, "可清分明细识别操作人不能为空");
    }

    private long defaultAmount(Long value) {
        return value == null ? 0L : value;
    }

    private record ResolvedClearingSource(FundsTransactionDetailDTO detail, LedgerEntryDTO entry) {
    }

}
