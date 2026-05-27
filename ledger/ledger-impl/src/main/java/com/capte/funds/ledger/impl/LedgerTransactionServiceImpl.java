package com.capte.funds.ledger.impl;

import com.alibaba.fastjson2.JSON;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.capte.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import com.capte.funds.ledger.dto.LedgerTransactionPostResult;
import com.capte.funds.ledger.mapstruct.LedgerConverter;
import com.capte.funds.ledger.query.LedgerEntryQuery;
import com.capte.funds.ledger.query.LedgerTransactionQuery;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.capte.funds.ledger.service.LedgerTransactionService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.WindDateFormater;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.util.WindObjectDigestUtils;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerReconcileStatus;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 账户账本交易服务实现类
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Service
@Slf4j
@AllArgsConstructor
public class LedgerTransactionServiceImpl implements LedgerTransactionService {

    private static final WindSequenceType LEDGER_ENTRY_SEQUENCE_TYPE = WindSequenceType.immutable(
            "LEDGER_ENTRY", "LGE", 6);

    private static final List<String> LEDGER_ENTRY_SHA256_FIELDS = List.of(
            LedgerEntry.Fields.tenantId,
            LedgerEntry.Fields.subjectId,
            LedgerEntry.Fields.subjectType,
            LedgerEntry.Fields.ledgerSubjectCode,
            LedgerEntry.Fields.ledgerSubjectCategory,
            LedgerEntry.Fields.entrySide,
            LedgerEntry.Fields.intent,
            LedgerEntry.Fields.postingScope,
            LedgerEntry.Fields.balanceEffectType,
            LedgerEntry.Fields.phaseCode,
            LedgerEntry.Fields.businessScene,
            LedgerEntry.Fields.businessSn,
            LedgerEntry.Fields.amount,
            LedgerEntry.Fields.currency,
            LedgerEntry.Fields.originalAmount,
            LedgerEntry.Fields.originalCurrency,
            LedgerEntry.Fields.exchangeRate,
            LedgerEntry.Fields.transactionTime
    );

    private static final List<String> LEDGER_POSTING_PLAN_SHA256_FIELDS = List.of(
            LedgerPostingPlan.Fields.tenantId,
            LedgerPostingPlan.Fields.routeLegId,
            LedgerPostingPlan.Fields.intent,
            LedgerPostingPlan.Fields.postingScope,
            LedgerPostingPlan.Fields.balanceEffectType,
            LedgerPostingPlan.Fields.phaseCode,
            LedgerPostingPlan.Fields.amount,
            LedgerPostingPlan.Fields.currency,
            LedgerPostingPlan.Fields.debitAmount,
            LedgerPostingPlan.Fields.creditAmount
    );

    private static final List<String> LEDGER_TRANSACTION_SHA256_FIELDS = List.of(
            LedgerTransaction.Fields.tenantId,
            LedgerTransaction.Fields.instructionType,
            LedgerTransaction.Fields.eventType,
            LedgerTransaction.Fields.transactionType,
            LedgerTransaction.Fields.businessScene,
            LedgerTransaction.Fields.businessSn,
            LedgerTransaction.Fields.amount,
            LedgerTransaction.Fields.currency,
            LedgerTransaction.Fields.originalAmount,
            LedgerTransaction.Fields.originalCurrency,
            LedgerTransaction.Fields.exchangeRate,
            LedgerTransaction.Fields.debitAmount,
            LedgerTransaction.Fields.creditAmount,
            LedgerTransaction.Fields.transactionTime,
            LedgerTransaction.Fields.referenceLedgerTransactionSn
    );

    private final LedgerTransactionMapper ledgerTransactionMapper;

    private final LedgerPostingPlanMapper ledgerPostingPlanMapper;

    private final LedgerEntryMapper ledgerEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull LedgerTransactionPostResult postLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
        assertNoSensitiveContextVariables(transaction);
        LedgerTransaction entity = LedgerConverter.INSTANCE.convertToLedgerTransaction(transaction);
        entity.setDebitAmount(transaction.getTotalDebitAmount().getAmount());
        entity.setCreditAmount(transaction.getTotalCreditAmount().getAmount());
        entity.setBalanced(transaction.isBalanced());
        entity.setContextVariables(JSON.toJSONString(transaction.getContextVariables()));
        entity.setSha256(WindObjectDigestUtils.sha256WithNames(entity, LEDGER_TRANSACTION_SHA256_FIELDS));
        LedgerTransactionPostResult existingResult = resolveExistingLedgerTransaction(entity);
        if (existingResult != null) {
            return existingResult;
        }
        ledgerTransactionMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户账本交易失败");
        for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
            String postingPlanSn = createPostingPlan(entity, transaction, plan);
            for (LedgerPostingPhaseSpec phase : plan.getPostingPhases()) {
                for (LedgerEntrySpec entry : phase.getEntries()) {
                    createLedgerEntry(entity, plan, phase, postingPlanSn, entry);
                }
            }
        }
        return toPostResult(entity.getId(), true);
    }

    private LedgerTransactionPostResult resolveExistingLedgerTransaction(LedgerTransaction entity) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(entity.getSn()));
        LedgerTransaction existing = ledgerTransactionMapper.selectOneByQuery(wrapper);
        if (existing == null) {
            return null;
        }
        AssertUtils.isTrue(Objects.equals(existing.getSha256(), entity.getSha256()),
                "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", entity.getSn());
        return toPostResult(existing.getId(), false);
    }

    private LedgerTransactionPostResult toPostResult(Long ledgerTransactionId, boolean newlyPosted) {
        return new LedgerTransactionPostResult()
                .setLedgerTransactionId(ledgerTransactionId)
                .setNewlyPosted(newlyPosted);
    }

    private String createPostingPlan(LedgerTransaction transaction,
                                     LedgerTransactionSpec transactionSpec,
                                     LedgerPostingPlanSpec plan) {
        String postingPlanSn = plan.getPlanId();
        Money amount = plan.getAmount();
        String phaseCode = resolvePhaseCode(plan);
        LedgerPostingPlan entity = new LedgerPostingPlan();
        entity.setSn(postingPlanSn);
        entity.setTenantId(transaction.getTenantId());
        entity.setLedgerTransactionSn(transaction.getSn());
        entity.setFundsTransactionSn(transaction.getFundsTransactionSn());
        entity.setRouteLegId(plan.getRouteLegId());
        entity.setIntent(plan.getIntent().name());
        entity.setPostingScope(resolvePostingScope(plan, phaseCode).name());
        entity.setBalanceEffectType(resolveBalanceEffectType(plan, phaseCode).name());
        entity.setPhaseCode(phaseCode);
        entity.setAmount(amount.getAmount());
        entity.setCurrency(amount.getCurrency());
        entity.setDebitAmount(plan.getTotalDebitAmount().getAmount());
        entity.setCreditAmount(plan.getTotalCreditAmount().getAmount());
        entity.setBalanced(plan.isBalanced());
        entity.setDescription(defaultIfBlank(plan.getDescription(), transaction.getDescription()));
        entity.setContextVariables(JSON.toJSONString(mergeContextVariables(transactionSpec, plan)));
        entity.setSha256(WindObjectDigestUtils.sha256WithNames(entity, LEDGER_POSTING_PLAN_SHA256_FIELDS));
        ledgerPostingPlanMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户账本记账计划失败");
        return postingPlanSn;
    }

    private Map<String, Object> mergeContextVariables(LedgerTransactionSpec transactionSpec, LedgerPostingPlanSpec plan) {
        Map<String, Object> contextVariables = new LinkedHashMap<>(transactionSpec.getContextVariables());
        contextVariables.putAll(plan.getContextVariables());
        return contextVariables;
    }

    private String resolvePhaseCode(LedgerPostingPlanSpec plan) {
        return plan.getPostingPhases()
                .stream()
                .map(LedgerPostingPhaseSpec::getPhaseCode)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void createLedgerEntry(LedgerTransaction transaction,
                                   LedgerPostingPlanSpec plan,
                                   LedgerPostingPhaseSpec phase,
                                   String postingPlanSn,
                                   LedgerEntrySpec entry) {
        LedgerEntry ledgerEntry = LedgerConverter.INSTANCE.convertToLedgerEntry(entry);
        ledgerEntry.setSn(TemporalSequenceFactory.hourNext(LEDGER_ENTRY_SEQUENCE_TYPE));
        ledgerEntry.setTenantId(transaction.getTenantId());
        ledgerEntry.setLedgerTransactionSn(transaction.getSn());
        ledgerEntry.setPostingPlanSn(postingPlanSn);
        ledgerEntry.setFundsTransactionSn(transaction.getFundsTransactionSn());
        ledgerEntry.setIntent(defaultIfNull(entry.getIntent(), plan.getIntent()).name());
        ledgerEntry.setPostingScope(defaultIfNull(
                entry.getPostingScope(), resolvePostingScope(plan, phase.getPhaseCode().name())).name());
        ledgerEntry.setBalanceEffectType(defaultIfNull(
                entry.getBalanceEffectType(), resolveBalanceEffectType(plan, phase.getPhaseCode().name())).name());
        ledgerEntry.setBalanceConstraintType(defaultIfNull(
                entry.getBalanceConstraintType(), LedgerBalanceConstraintType.PROFILE_DEFAULT).name());
        ledgerEntry.setPhaseCode(defaultIfNull(entry.getPhaseCode(), phase.getPhaseCode()).name());
        ledgerEntry.setSha256(WindObjectDigestUtils.sha256WithNames(ledgerEntry, LEDGER_ENTRY_SHA256_FIELDS));
        if (ledgerEntry.getSettlementStatus() == null) {
            ledgerEntry.setSettlementStatus(LedgerSettlementStatus.SETTLED);
        }
        if (ledgerEntry.getSettlementStatus() == LedgerSettlementStatus.SETTLED) {
            ledgerEntry.setSettlementCompletedTime(LocalDateTime.now());
        }
        ledgerEntry.setReconcileStatus(LedgerReconcileStatus.PENDING);
        ledgerEntry.setReconciliationBatch(currentReconciliationBatch());
        ledgerEntryMapper.insertSelective(ledgerEntry);
        AssertUtils.notNull(ledgerEntry.getId(), "创建账户账本条目失败");
    }

    private void assertNoSensitiveContextVariables(LedgerTransactionSpec transaction) {
        assertNoSensitiveContextVariables(transaction.getContextVariables(), "ledgerTransaction.contextVariables");
        for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
            assertNoSensitiveContextVariables(plan.getContextVariables(), "ledgerPostingPlan.contextVariables");
            for (LedgerPostingPhaseSpec phase : plan.getPostingPhases()) {
                for (LedgerEntrySpec entry : phase.getEntries()) {
                    assertNoSensitiveContextVariables(entry.getContextVariables(), "ledgerEntry.contextVariables");
                }
            }
        }
    }

    private void assertNoSensitiveContextVariables(Map<String, Object> contextVariables, String fieldName) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                "{} must not contain sensitive fields", fieldName);
    }


    @Override
    public void updateLedgerTransaction(@NonNull UpdateLedgerTransactionRequest request) {
        LedgerTransaction entity = findAccountLedgerTransaction(request.getId());
        entity.setStatus(request.getStatus());
        entity.setDescription(request.getDescription());
        if (request.getContextVariable() != null) {
            assertNoSensitiveContextVariables(request.getContextVariable(), "ledgerTransaction.contextVariables");
            entity.setContextVariables(JSON.toJSONString(request.getContextVariable()));
        }
        AssertUtils.isTrue(ledgerTransactionMapper.update(entity) == 1, "更新账户账本交易信息失败");
    }


    @Override
    public void deleteLedgerTransactionByIds(@NonNull Long... ids) {
        AssertUtils.notEmpty(ids, "argument ids must not empty");
        int total = ledgerTransactionMapper.deleteBatchByIds(List.of(ids));
        AssertUtils.isTrue(total == ids.length, "删除账户账本交易失败");
    }

    @Override
    @NonNull
    public LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
        return LedgerConverter.INSTANCE.convertToAccountLedgerTransactionDTO(findAccountLedgerTransaction(id));
    }

    @Override
    public @NonNull WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(
            @NonNull LedgerTransactionQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        LedgerTransactionNameRefs ledgerTransaction = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper queryWrapper = MybatisQueryHelper.from(options).select()
                .from(ledgerTransaction)
                .where(ledgerTransaction.sn.eq(query.getSn()))
                .and(ledgerTransaction.tenantId.eq(query.getTenantId()))
                .and(ledgerTransaction.fundsTransactionSn.eq(query.getFundsTransactionSn()))
                .and(ledgerTransaction.eventType.eq(query.getEventType()))
                .and(ledgerTransaction.status.eq(query.getStatus()))
                .and(ledgerTransaction.currency.eq(query.getCurrency()))
                .and(ledgerTransaction.businessSn.eq(query.getBusinessSn()))
                .and(ledgerTransaction.businessScene.eq(query.getBusinessScene()))
                .and(ledgerTransaction.referenceLedgerTransactionSn.eq(query.getReferenceLedgerTransactionSn()))
                .and(ledgerTransaction.transactionTime.ge(query.getTransactionTimeMin()))
                .and(ledgerTransaction.transactionTime.le(query.getTransactionTimeMax()))
                .and(ledgerTransaction.gmtCreate.ge(query.getGmtCreateMin()))
                .and(ledgerTransaction.gmtCreate.le(query.getGmtCreateMax()))
                .and(ledgerTransaction.gmtModified.ge(query.getGmtModifiedMin()))
                .and(ledgerTransaction.gmtModified.le(query.getGmtModifiedMax()));

        return MybatisQueryHelper.<LedgerTransaction, LedgerTransactionDTO>query(queryWrapper)
                .counter(ledgerTransactionMapper::selectCountByQuery)
                .resultQueryFunc(ledgerTransactionMapper::selectListByQuery)
                .converter(LedgerConverter.INSTANCE::convertToAccountLedgerTransactionDTO)
                .query(options);
    }


    private LedgerTransaction findAccountLedgerTransaction(Long id) {
        LedgerTransaction result = ledgerTransactionMapper.selectOneById(id);
        AssertUtils.notNull(result, "账户账本交易不存在");
        return result;
    }

    private LedgerPostingScope resolvePostingScope(LedgerPostingPlanSpec plan, String phaseCode) {
        if (plan.getPostingScope() != null) {
            return plan.getPostingScope();
        }
        return switch (plan.getIntent()) {
            case TOPUP, WITHDRAWAL -> LedgerPostingScope.PLATFORM_EXTERNAL;
            case FEE, FEE_REFUND, FEE_REVERSAL -> LedgerPostingScope.FEE;
            case ADJUSTMENT -> LedgerPostingScope.ADJUSTMENT;
            case HOLD -> resolveHoldPostingScope(phaseCode);
            case AUTHORIZATION, AUTHORIZATION_REVERSAL -> LedgerPostingScope.CONTROL_HOLD;
            case AUTHORIZATION_SETTLEMENT -> LedgerPostingScope.CONTROL_CONSUME;
            default -> LedgerPostingScope.BETWEEN_SUBJECTS;
        };
    }

    private LedgerPostingScope resolveHoldPostingScope(String phaseCode) {
        if (LedgerPhaseCode.FREEZE.name().equals(phaseCode) || LedgerPhaseCode.UNFREEZE.name().equals(phaseCode)) {
            return LedgerPostingScope.WITHIN_SUBJECT;
        }
        return LedgerPostingScope.CONTROL_HOLD;
    }

    private LedgerBalanceEffectType resolveBalanceEffectType(LedgerPostingPlanSpec plan, String phaseCode) {
        if (plan.getBalanceEffectType() != null) {
            return plan.getBalanceEffectType();
        }
        return switch (plan.getIntent()) {
            case TOPUP -> LedgerBalanceEffectType.INCREASE;
            case WITHDRAWAL -> LedgerBalanceEffectType.DECREASE;
            case FEE -> LedgerBalanceEffectType.CONSUME;
            case FEE_REFUND -> LedgerBalanceEffectType.RESTORE;
            case FEE_REVERSAL, REVERSAL, AUTHORIZATION_REVERSAL -> LedgerBalanceEffectType.RELEASE;
            case ADJUSTMENT -> LedgerBalanceEffectType.INCREASE;
            case HOLD -> resolveHoldBalanceEffectType(phaseCode);
            case AUTHORIZATION -> LedgerBalanceEffectType.HOLD;
            case AUTHORIZATION_SETTLEMENT -> LedgerBalanceEffectType.CONSUME;
            case REFUND -> LedgerBalanceEffectType.RESTORE;
            default -> LedgerBalanceEffectType.CONSUME;
        };
    }

    private LedgerBalanceEffectType resolveHoldBalanceEffectType(String phaseCode) {
        if (LedgerPhaseCode.FREEZE.name().equals(phaseCode)) {
            return LedgerBalanceEffectType.HOLD;
        }
        if (LedgerPhaseCode.UNFREEZE.name().equals(phaseCode)) {
            return LedgerBalanceEffectType.RELEASE;
        }
        return LedgerBalanceEffectType.HOLD;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String currentReconciliationBatch() {
        return WindDateFormater.YYYY_MM_DD_HH.format(LocalDateTime.now());
    }

    @Override
    public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
        return LedgerConverter.INSTANCE.convertToLedgerEntryDTO(findAccountLedgerEntry(id));
    }

    @Override
    public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
            @NonNull LedgerEntryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        LedgerEntryNameRefs ledgerEntry = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper queryWrapper = MybatisQueryHelper.from(options).select()
                .from(ledgerEntry)
                .where(ledgerEntry.subjectId.eq(query.getSubjectId()))
                .and(ledgerEntry.subjectType.eq(query.getSubjectType()))
                .and(ledgerEntry.ledgerSubjectCode.eq(query.getLedgerSubjectCode()))
                .and(ledgerEntry.ledgerSubjectCategory.eq(query.getLedgerSubjectCategory()))
                .and(ledgerEntry.ledgerTransactionSn.eq(query.getLedgerTransactionSn()))
                .and(ledgerEntry.entrySide.eq(query.getEntryType()))
                .and(ledgerEntry.businessScene.eq(query.getBusinessScene()))
                .and(ledgerEntry.businessSn.eq(query.getBusinessSn()))
                .and(ledgerEntry.currency.eq(query.getCurrency()))
                .and(ledgerEntry.originalCurrency.eq(query.getOriginalCurrency()))
                .and(ledgerEntry.settlementStatus.eq(query.getSettlementStatus()))
                .and(ledgerEntry.settlementPeriod.eq(query.getSettlementPeriod()))
                .and(ledgerEntry.settlementCompletedTime.ge(query.getSettlementCompletedTimeMin()))
                .and(ledgerEntry.settlementCompletedTime.le(query.getSettlementCompletedTimeMax()))
                .and(ledgerEntry.reconcileStatus.eq(query.getReconcileStatus()))
                .and(ledgerEntry.reconciliationBatch.eq(query.getReconciliationBatch()))
                .and(ledgerEntry.reconciliationCompletedTime.ge(query.getReconciliationCompletedTimeMin()))
                .and(ledgerEntry.reconciliationCompletedTime.le(query.getReconciliationCompletedTimeMax()))
                .and(ledgerEntry.gmtCreate.ge(query.getGmtCreateMin()))
                .and(ledgerEntry.gmtCreate.le(query.getGmtCreateMax()))
                .and(ledgerEntry.gmtModified.ge(query.getGmtModifiedMin()))
                .and(ledgerEntry.gmtModified.le(query.getGmtModifiedMax()));

        return MybatisQueryHelper.<LedgerEntry, LedgerEntryDTO>query(queryWrapper)
                .counter(ledgerEntryMapper::selectCountByQuery)
                .resultQueryFunc(ledgerEntryMapper::selectListByQuery)
                .converter(LedgerConverter.INSTANCE::convertToLedgerEntryDTO)
                .query(options);
    }


    private LedgerEntry findAccountLedgerEntry(Long id) {
        LedgerEntry result = ledgerEntryMapper.selectOneById(id);
        AssertUtils.notNull(result, "账户账本条目不存在");
        return result;
    }
}
