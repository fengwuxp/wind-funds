package com.wind.funds.reconciliation.application.clearing.impl;

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
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
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

    private final ClearingSplittableDetailMapper clearingSplittableDetailMapper;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final LedgerTransactionService ledgerTransactionService;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClearingSplittableDetailDTO identifySplittableDetail(IdentifyClearingSplittableDetailRequest request,
                                                               WindOperator operator) {
        validateRequest(request, operator);
        FundsTransactionDTO transaction = fundsTransactionQueryService
                .queryFundsTransaction(request.getFundsTransactionSn())
                .orElse(null);
        AssertUtils.notNull(transaction, "可清分来源资金交易不存在，fundsTransactionSn = {}",
                request.getFundsTransactionSn());
        FundsTransactionDetailDTO detail = requiredDetail(request);
        LedgerEntryDTO entry = ledgerTransactionService.getLedgerEntryBySn(
                request.getTenantId(), request.getLedgerEntrySn());
        LedgerTransactionDTO ledgerTransaction = ledgerTransactionService.getLedgerTransactionBySn(
                request.getTenantId(), entry.getLedgerTransactionSn());
        ReconciliationGateDecisionDTO reconciliationDecision = checkReconciliationGate(request, operator);
        ClearingSplittableDetail candidate = toCandidate(request, operator, transaction, detail, entry,
                ledgerTransaction, reconciliationDecision);
        if (candidate.getExclusionReason() == ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED) {
            log.info("可清分明细被对账 Gate 临时阻断，tenantId = {}, fundsTransactionSn = {}, ledgerEntrySn = {}",
                    request.getTenantId(), request.getFundsTransactionSn(), request.getLedgerEntrySn());
            return ClearingSplittableDetailConverter.INSTANCE.toDTO(candidate);
        }

        ClearingSplittableDetail existing = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                request.getTenantId(), request.getLedgerEntrySn());
        if (existing != null) {
            return reuseExisting(existing, candidate);
        }
        try {
            clearingSplittableDetailMapper.insertSelective(candidate);
        } catch (DuplicateKeyException exception) {
            ClearingSplittableDetail winner = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                    request.getTenantId(), request.getLedgerEntrySn());
            AssertUtils.notNull(winner, "可清分明细唯一键冲突后未找到幂等结果");
            return reuseExisting(winner, candidate);
        }
        AssertUtils.notNull(candidate.getId(), "创建可清分明细准入结果失败");
        ClearingSplittableDetail saved = clearingSplittableDetailMapper.selectByLedgerEntrySn(
                request.getTenantId(), request.getLedgerEntrySn());
        log.info("可清分明细准入完成，tenantId = {}, fundsTransactionSn = {}, ledgerEntrySn = {}, status = {}, exclusionReason = {}",
                request.getTenantId(), request.getFundsTransactionSn(), request.getLedgerEntrySn(),
                candidate.getAdmissionResult(), candidate.getExclusionReason());
        return ClearingSplittableDetailConverter.INSTANCE.toDTO(saved);
    }

    private FundsTransactionDetailDTO requiredDetail(IdentifyClearingSplittableDetailRequest request) {
        FundsTransactionDetailDTO result = fundsTransactionQueryService
                .queryFundsTransactionDetails(request.getFundsTransactionSn())
                .stream()
                .filter(detail -> request.getFundsTransactionDetailSn().equals(detail.getSn()))
                .findFirst()
                .orElse(null);
        AssertUtils.notNull(result, "可清分来源资金交易明细不存在，fundsTransactionDetailSn = {}",
                request.getFundsTransactionDetailSn());
        return result;
    }

    private ReconciliationGateDecisionDTO checkReconciliationGate(IdentifyClearingSplittableDetailRequest request,
                                                                  WindOperator operator) {
        return reconciliationGateApplicationService.checkGate(new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setStageRef(new GateStageRef()
                        .setStageKind("CLEARING_SPLITTABLE_IDENTIFY")
                        .setStageIdentity(new StableIdentity()
                                .setOwnerNamespace("funds-transaction-detail")
                                .setValue(request.getFundsTransactionDetailSn()))), operator);
    }

    private ClearingSplittableDetail toCandidate(IdentifyClearingSplittableDetailRequest request,
                                                 WindOperator operator,
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
        result.setSourceDigest(sourceDigest(request, transaction, detail, entry, ledgerTransaction,
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
                                FundsTransactionDTO transaction,
                                FundsTransactionDetailDTO detail,
                                LedgerEntryDTO entry,
                                LedgerTransactionDTO ledgerTransaction,
                                ReconciliationGateDecisionDTO reconciliationDecision) {
        TreeMap<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", request.getTenantId());
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
        return ClearingSplittableDetailConverter.INSTANCE.toDTO(existing);
    }

    private void validateRequest(IdentifyClearingSplittableDetailRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "可清分明细识别请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "可清分明细租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "可清分明细 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getFundsTransactionSn(), "来源资金交易流水号不能为空");
        AssertUtils.hasText(request.getFundsTransactionDetailSn(), "来源资金交易明细流水号不能为空");
        AssertUtils.hasText(request.getLedgerEntrySn(), "来源账本分录流水号不能为空");
        AssertUtils.hasText(request.getBusinessLine(), "业务线不能为空");
        AssertUtils.hasText(request.getSplitPeriod(), "清分周期不能为空");
        AssertUtils.hasText(request.getSplitRuleCode(), "清分规则编码不能为空");
        AssertUtils.hasText(request.getSplitRuleVersion(), "清分规则版本不能为空");
        AssertUtils.notNull(operator, "可清分明细识别操作人不能为空");
    }

    private long defaultAmount(Long value) {
        return value == null ? 0L : value;
    }
}
