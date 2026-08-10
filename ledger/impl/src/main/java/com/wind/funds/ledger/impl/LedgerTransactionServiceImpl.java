package com.wind.funds.ledger.impl;

import com.wind.jackson.WindJson;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.dal.entities.table.LedgerEntryNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerPostingPlanNameRefs;
import com.wind.funds.ledger.dal.entities.table.LedgerTransactionNameRefs;
import com.wind.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.wind.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.wind.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.dto.LedgerTransactionPostResult;
import com.wind.funds.ledger.LedgerTransactionCommandService;
import com.wind.funds.ledger.mapstruct.LedgerConverter;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.util.WindObjectDigestUtils;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.transaction.support.FundsInstructionContextValidator;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPhaseSpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
public class LedgerTransactionServiceImpl implements LedgerTransactionService, LedgerTransactionCommandService {

    private static final String LEDGER_TRANSACTION_DIGEST_DOMAIN = "ledger.transaction.request";

    private static final WindSequenceType LEDGER_ENTRY_SEQUENCE_TYPE = WindSequenceType.immutable(
            "LEDGER_ENTRY", "LGE", 6);

    private static final List<String> LEDGER_ENTRY_SHA256_FIELDS = List.of(
            LedgerEntry.Fields.sn,
            LedgerEntry.Fields.tenantId,
            LedgerEntry.Fields.ledgerTransactionSn,
            LedgerEntry.Fields.postingPlanSn,
            LedgerEntry.Fields.fundsTransactionSn,
            LedgerEntry.Fields.ledgerId,
            LedgerEntry.Fields.subjectId,
            LedgerEntry.Fields.subjectType,
            LedgerEntry.Fields.periodType,
            LedgerEntry.Fields.periodId,
            LedgerEntry.Fields.ledgerSubjectCode,
            LedgerEntry.Fields.ledgerSubjectCategory,
            LedgerEntry.Fields.entrySide,
            LedgerEntry.Fields.postingRole,
            LedgerEntry.Fields.balanceConstraintType,
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
            LedgerTransaction.Fields.fundsTransactionSn,
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
        entity.setContextVariables(WindJson.toJsonString(transaction.getContextVariables()));
        entity.setSha256(FundsStableHashSupport.sha256CanonicalJson(
                LEDGER_TRANSACTION_DIGEST_DOMAIN, canonicalLedgerTransactionDigestFacts(entity, transaction)));
        LedgerTransactionPostResult existingResult = resolveExistingLedgerTransaction(entity, transaction);
        if (existingResult != null) {
            return existingResult;
        }
        try {
            ledgerTransactionMapper.insertSelective(entity);
        } catch (DuplicateKeyException exception) {
            LedgerTransactionPostResult retryResult = resolveExistingLedgerTransaction(entity, transaction);
            if (retryResult != null) {
                return retryResult;
            }
            throw exception;
        }
        AssertUtils.notNull(entity.getId(), "创建账户账本交易失败");
        for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
            String postingPlanSn = createPostingPlan(entity, plan);
            for (LedgerPostingPhaseSpec phase : plan.getPostingPhases()) {
                for (LedgerEntrySpec entry : phase.getEntries()) {
                    createLedgerEntry(entity, plan, phase, postingPlanSn, entry);
                }
            }
        }
        return toPostResult(entity.getId(), true);
    }

    private Map<String, Object> legacyLedgerTransactionDigestFacts(LedgerTransaction entity,
                                                                   LedgerTransactionSpec transaction) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("transaction",
                WindObjectDigestUtils.sha256WithNames(entity, LEDGER_TRANSACTION_SHA256_FIELDS));
        facts.put("postingPlans", transaction.getPostingPlans()
                .stream()
                .map(plan -> postingPlanDigestFacts(plan, false))
                .toList());
        return facts;
    }

    private Map<String, Object> canonicalLedgerTransactionDigestFacts(LedgerTransaction entity,
                                                                      LedgerTransactionSpec transaction) {
        Map<String, Object> transactionFacts = new TreeMap<>();
        transactionFacts.put("tenantId", entity.getTenantId());
        transactionFacts.put("instructionType", entity.getInstructionType());
        transactionFacts.put("eventType", entity.getEventType());
        transactionFacts.put("fundsTransactionSn", entity.getFundsTransactionSn());
        transactionFacts.put("transactionType", entity.getTransactionType());
        transactionFacts.put("businessScene", entity.getBusinessScene());
        transactionFacts.put("businessSn", entity.getBusinessSn());
        transactionFacts.put("amount", entity.getAmount());
        transactionFacts.put("currency", entity.getCurrency());
        transactionFacts.put("originalAmount", entity.getOriginalAmount());
        transactionFacts.put("originalCurrency", entity.getOriginalCurrency());
        transactionFacts.put("exchangeRate", entity.getExchangeRate());
        transactionFacts.put("debitAmount", entity.getDebitAmount());
        transactionFacts.put("creditAmount", entity.getCreditAmount());
        transactionFacts.put("transactionTime", entity.getTransactionTime().toString());
        transactionFacts.put("referenceLedgerTransactionSn", entity.getReferenceLedgerTransactionSn());
        Map<String, Object> facts = new TreeMap<>();
        facts.put("transaction", transactionFacts);
        facts.put("postingPlans", transaction.getPostingPlans()
                .stream()
                .map(plan -> postingPlanDigestFacts(plan, true))
                .toList());
        return facts;
    }

    private Map<String, Object> postingPlanDigestFacts(LedgerPostingPlanSpec plan, boolean canonical) {
        String phaseCode = resolvePhaseCode(plan);
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", plan.getPlanId());
        facts.put("ledgerTransactionSn", plan.getLedgerTransactionSn());
        facts.put("routeLegId", plan.getRouteLegId());
        facts.put("intent", plan.getIntent().name());
        facts.put("postingScope", resolvePostingScope(plan, phaseCode).name());
        facts.put("balanceEffectType", resolveBalanceEffectType(plan, phaseCode).name());
        facts.put("phaseCode", phaseCode);
        facts.put("amount", plan.getAmount().getAmount());
        facts.put("currency", plan.getAmount().getCurrency());
        facts.put("debitAmount", plan.getTotalDebitAmount().getAmount());
        facts.put("creditAmount", plan.getTotalCreditAmount().getAmount());
        facts.put("postingPhases", plan.getPostingPhases()
                .stream()
                .map(phase -> postingPhaseDigestFacts(plan, phase, canonical))
                .toList());
        return facts;
    }

    private Map<String, Object> postingPhaseDigestFacts(LedgerPostingPlanSpec plan,
                                                        LedgerPostingPhaseSpec phase,
                                                        boolean canonical) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("phaseCode", phase.getPhaseCode().name());
        facts.put("entries", phase.getEntries()
                .stream()
                .map(entry -> ledgerEntryDigestFacts(plan, phase, entry, canonical))
                .toList());
        return facts;
    }

    private Map<String, Object> ledgerEntryDigestFacts(LedgerPostingPlanSpec plan,
                                                       LedgerPostingPhaseSpec phase,
                                                       LedgerEntrySpec entry,
                                                       boolean canonical) {
        String phaseCode = phase.getPhaseCode().name();
        Map<String, Object> facts = new TreeMap<>();
        facts.put("ledgerTransactionSn", entry.getLedgerTransactionSn());
        facts.put("ledgerId", entry.getLedgerId());
        facts.put("periodType", entry.getPeriodType().name());
        facts.put("periodId", entry.getPeriodId());
        facts.put("subjectId", entry.getSubjectId());
        facts.put("subjectType", entry.getSubjectType());
        facts.put("ledgerSubjectCode", entry.getLedgerSubjectCode().name());
        facts.put("ledgerSubjectCategory", entry.getLedgerSubjectCategory().name());
        facts.put("entrySide", entry.getEntrySide().name());
        facts.put("postingRole", entry.getPostingRole().name());
        facts.put("balanceConstraintType", defaultIfNull(
                entry.getBalanceConstraintType(), LedgerBalanceConstraintType.PROFILE_DEFAULT).name());
        facts.put("intent", defaultIfNull(entry.getIntent(), plan.getIntent()).name());
        facts.put("postingScope", defaultIfNull(
                entry.getPostingScope(), resolvePostingScope(plan, phaseCode)).name());
        facts.put("balanceEffectType", defaultIfNull(
                entry.getBalanceEffectType(), resolveBalanceEffectType(plan, phaseCode)).name());
        facts.put("phaseCode", defaultIfNull(entry.getPhaseCode(), phase.getPhaseCode()).name());
        facts.put("businessScene", entry.getBusinessScene());
        facts.put("businessSn", entry.getBusinessSn());
        facts.put("amount", entry.getAmount().getAmount());
        facts.put("currency", entry.getAmount().getCurrency());
        facts.put("originalAmount", entry.getOriginalAmount().getAmount());
        facts.put("originalCurrency", entry.getOriginalAmount().getCurrency());
        facts.put("exchangeRate", entry.getExchangeRate());
        facts.put("transactionTime", canonical ? entry.getTransactionTime().toString() : entry.getTransactionTime());
        return facts;
    }

    private LedgerTransactionPostResult resolveExistingLedgerTransaction(LedgerTransaction entity,
                                                                          LedgerTransactionSpec transaction) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(entity.getSn()));
        LedgerTransaction existing = ledgerTransactionMapper.selectOneByQuery(wrapper);
        if (existing == null) {
            return null;
        }
        AssertUtils.isTrue(FundsStableHashSupport.matchesCanonicalOrLegacyJson(
                        existing.getSha256(),
                        LEDGER_TRANSACTION_DIGEST_DOMAIN,
                        canonicalLedgerTransactionDigestFacts(entity, transaction),
                        legacyLedgerTransactionDigestFacts(entity, transaction)),
                "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", entity.getSn());
        return toPostResult(existing.getId(), false);
    }

    private LedgerTransactionPostResult toPostResult(Long ledgerTransactionId, boolean newlyPosted) {
        return new LedgerTransactionPostResult()
                .setLedgerTransactionId(ledgerTransactionId)
                .setNewlyPosted(newlyPosted);
    }

    private String createPostingPlan(LedgerTransaction transaction, LedgerPostingPlanSpec plan) {
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
        entity.setDescription(defaultIfBlank(plan.getDescription(), transaction.getDescription()));
        entity.setContextVariables(WindJson.toJsonString(plan.getContextVariables()));
        entity.setSha256(WindObjectDigestUtils.sha256WithNames(entity, LEDGER_POSTING_PLAN_SHA256_FIELDS));
        ledgerPostingPlanMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建账户账本记账计划失败");
        return postingPlanSn;
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
        AssertUtils.notNull(entry.getPostingRole(), "账本分录记账角色不能为空，ledgerTransactionSn = {}",
                transaction.getSn());
        ledgerEntry.setPostingRole(entry.getPostingRole());
        ledgerEntry.setIntent(defaultIfNull(entry.getIntent(), plan.getIntent()).name());
        ledgerEntry.setPostingScope(defaultIfNull(
                entry.getPostingScope(), resolvePostingScope(plan, phase.getPhaseCode().name())).name());
        ledgerEntry.setBalanceEffectType(defaultIfNull(
                entry.getBalanceEffectType(), resolveBalanceEffectType(plan, phase.getPhaseCode().name())).name());
        ledgerEntry.setBalanceConstraintType(defaultIfNull(
                entry.getBalanceConstraintType(), LedgerBalanceConstraintType.PROFILE_DEFAULT).name());
        ledgerEntry.setPhaseCode(defaultIfNull(entry.getPhaseCode(), phase.getPhaseCode()).name());
        ledgerEntry.setSha256(WindObjectDigestUtils.sha256WithNames(ledgerEntry, LEDGER_ENTRY_SHA256_FIELDS));
        ledgerEntryMapper.insertSelective(ledgerEntry);
        AssertUtils.notNull(ledgerEntry.getId(), "创建账户账本条目失败");
    }

    private void assertNoSensitiveContextVariables(LedgerTransactionSpec transaction) {
        assertNoSensitiveContextVariables(transaction.getContextVariables(), "ledgerTransaction.contextVariables");
        assertNoCoreBenefitContextVariables(transaction.getContextVariables(), "ledgerTransaction");
        for (LedgerPostingPlanSpec plan : transaction.getPostingPlans()) {
            assertNoSensitiveContextVariables(plan.getContextVariables(), "ledgerPostingPlan.contextVariables");
            assertNoCoreBenefitContextVariables(plan.getContextVariables(), "ledgerPostingPlan");
            for (LedgerPostingPhaseSpec phase : plan.getPostingPhases()) {
                for (LedgerEntrySpec entry : phase.getEntries()) {
                    assertNoSensitiveContextVariables(entry.getContextVariables(), "ledgerEntry.contextVariables");
                    assertNoCoreBenefitContextVariables(entry.getContextVariables(), "ledgerEntry");
                }
            }
        }
    }

    private void assertNoSensitiveContextVariables(Map<String, Object> contextVariables, String fieldName) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                "{} must not contain sensitive fields", fieldName);
    }

    private void assertNoCoreBenefitContextVariables(Map<String, Object> contextVariables, String owner) {
        FundsInstructionContextValidator.immutableInstructionContext(contextVariables, owner);
    }


    @Override
    @NonNull
    public LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
        return LedgerConverter.INSTANCE.convertToAccountLedgerTransactionDTO(findAccountLedgerTransaction(id));
    }

    @Override
    @NonNull
    public LedgerTransactionDTO getLedgerTransactionBySn(@NonNull Long tenantId, @NonNull String sn) {
        AssertUtils.notNull(tenantId, "账本交易查询 tenantId 不能为空");
        AssertUtils.hasText(sn, "账本交易流水号不能为空");
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        LedgerTransaction result = ledgerTransactionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn)));
        AssertUtils.notNull(result, "账户账本交易不存在，tenantId = {}, sn = {}", tenantId, sn);
        return LedgerConverter.INSTANCE.convertToAccountLedgerTransactionDTO(result);
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
                .and(ledgerTransaction.eventType.eq(enumName(query.getEventType())))
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
            case RELEASE -> LedgerPostingScope.CONTROL_RELEASE;
            case AUTHORIZATION, AUTHORIZATION_REVERSAL -> LedgerPostingScope.CONTROL_HOLD;
            case AUTHORIZATION_COMPLETION -> LedgerPostingScope.CONTROL_CONSUME;
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
            case RELEASE -> LedgerBalanceEffectType.RELEASE;
            case ADJUSTMENT -> LedgerBalanceEffectType.INCREASE;
            case HOLD -> resolveHoldBalanceEffectType(phaseCode);
            case AUTHORIZATION -> LedgerBalanceEffectType.HOLD;
            case AUTHORIZATION_COMPLETION -> LedgerBalanceEffectType.CONSUME;
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

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Override
    public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
        return LedgerConverter.INSTANCE.convertToLedgerEntryDTO(findAccountLedgerEntry(id));
    }

    @Override
    @NonNull
    public LedgerEntryDTO getLedgerEntryBySn(@NonNull Long tenantId, @NonNull String sn) {
        AssertUtils.notNull(tenantId, "账目分录查询 tenantId 不能为空");
        AssertUtils.hasText(sn, "账目分录流水号不能为空");
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        LedgerEntry result = ledgerEntryMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn)));
        AssertUtils.notNull(result, "账户账本条目不存在，tenantId = {}, sn = {}", tenantId, sn);
        return LedgerConverter.INSTANCE.convertToLedgerEntryDTO(result);
    }

    @Override
    public boolean existsPostingPlan(@NonNull Long tenantId,
                                     @NonNull String postingPlanSn,
                                     @NonNull String ledgerTransactionSn) {
        AssertUtils.notNull(tenantId, "记账计划查询 tenantId 不能为空");
        AssertUtils.hasText(postingPlanSn, "记账计划流水号不能为空");
        AssertUtils.hasText(ledgerTransactionSn, "账本交易流水号不能为空");
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        return ledgerPostingPlanMapper.selectCountByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(postingPlanSn))
                .and(ref.ledgerTransactionSn.eq(ledgerTransactionSn))) == 1;
    }

    @Override
    public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
            @NonNull LedgerEntryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        LedgerEntryNameRefs ledgerEntry = LedgerEntryNameRefs.ledgerEntry;
        QueryWrapper queryWrapper = MybatisQueryHelper.from(options).select()
                .from(ledgerEntry)
                .where(ledgerEntry.sn.eq(query.getSn()))
                .and(ledgerEntry.tenantId.eq(query.getTenantId()))
                .and(ledgerEntry.subjectId.eq(query.getSubjectId()))
                .and(ledgerEntry.subjectType.eq(query.getSubjectType()))
                .and(ledgerEntry.ledgerSubjectCode.eq(query.getLedgerSubjectCode()))
                .and(ledgerEntry.ledgerSubjectCategory.eq(query.getLedgerSubjectCategory()))
                .and(ledgerEntry.ledgerTransactionSn.eq(query.getLedgerTransactionSn()))
                .and(ledgerEntry.periodType.eq(query.getPeriodType()))
                .and(ledgerEntry.periodId.eq(query.getPeriodId()))
                .and(ledgerEntry.entrySide.eq(query.getEntryType()))
                .and(ledgerEntry.businessScene.eq(query.getBusinessScene()))
                .and(ledgerEntry.businessSn.eq(query.getBusinessSn()))
                .and(ledgerEntry.currency.eq(query.getCurrency()))
                .and(ledgerEntry.originalCurrency.eq(query.getOriginalCurrency()))
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
