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
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
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
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final String LEDGER_TRANSACTION_DIGEST_DOMAIN = "ledger.persisted-transaction.v1";

    private static final String LEDGER_POSTING_PLAN_DIGEST_DOMAIN = "ledger.persisted-plan.v1";

    private static final String LEDGER_ENTRY_DIGEST_DOMAIN = "ledger.persisted-entry.v1";

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
            LedgerPostingPlan.Fields.sn,
            LedgerPostingPlan.Fields.tenantId,
            LedgerPostingPlan.Fields.ledgerTransactionSn,
            LedgerPostingPlan.Fields.fundsTransactionSn,
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
            LedgerTransaction.Fields.sn,
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
        LedgerTransaction ledgerTransaction = materializeLedgerTransaction(transaction);
        LedgerTransactionPostResult existingResult = resolveExistingLedgerTransaction(ledgerTransaction, transaction);
        if (existingResult != null) {
            log.info("账本交易幂等复用，tenantId={}, ledgerTransactionSn={}, fundsTransactionSn={}, "
                            + "businessScene={}, businessSn={}",
                    transaction.getTenantId(), transaction.getSn(), transaction.getFundsTransactionSn(),
                    transaction.getBusinessScene(), transaction.getBusinessSn());
            return existingResult;
        }
        LedgerAggregate aggregate = materializeLedgerAggregate(ledgerTransaction, transaction, null);
        try {
            ledgerTransactionMapper.insertSelective(aggregate.transaction());
        } catch (DuplicateKeyException exception) {
            LedgerTransactionPostResult retryResult = resolveExistingLedgerTransaction(ledgerTransaction, transaction);
            if (retryResult != null) {
                log.info("账本交易并发幂等复用，tenantId={}, ledgerTransactionSn={}, fundsTransactionSn={}, "
                                + "businessScene={}, businessSn={}",
                        transaction.getTenantId(), transaction.getSn(), transaction.getFundsTransactionSn(),
                        transaction.getBusinessScene(), transaction.getBusinessSn());
                return retryResult;
            }
            throw exception;
        }
        AssertUtils.notNull(aggregate.transaction().getId(), "创建账户账本交易失败");
        for (LedgerPostingPlan postingPlan : aggregate.postingPlans()) {
            ledgerPostingPlanMapper.insertSelective(postingPlan);
            AssertUtils.notNull(postingPlan.getId(), "创建账户账本记账计划失败");
        }
        for (LedgerEntry entry : aggregate.entries()) {
            ledgerEntryMapper.insertSelective(entry);
            AssertUtils.notNull(entry.getId(), "创建账户账本条目失败");
        }
        verifiedLedgerAggregate(aggregate.transaction());
        log.info("账本交易事实写入完成，等待事务提交，tenantId={}, ledgerTransactionSn={}, fundsTransactionSn={}, "
                        + "businessScene={}, businessSn={}, planCount={}, entryCount={}, amount={}, currency={}",
                transaction.getTenantId(), transaction.getSn(), transaction.getFundsTransactionSn(),
                transaction.getBusinessScene(), transaction.getBusinessSn(), aggregate.postingPlans().size(),
                aggregate.entries().size(), transaction.getAmount().getAmount(), transaction.getCurrency());
        return toPostResult(aggregate.transaction().getId(), true);
    }

    private LedgerTransaction materializeLedgerTransaction(LedgerTransactionSpec transaction) {
        LedgerTransaction entity = LedgerConverter.INSTANCE.convertToLedgerTransaction(transaction);
        entity.setInstructionType(defaultIfBlank(entity.getInstructionType(), ""));
        entity.setTransactionTime(entity.getTransactionTime().truncatedTo(ChronoUnit.SECONDS));
        entity.setDebitAmount(transaction.getTotalDebitAmount().getAmount());
        entity.setCreditAmount(transaction.getTotalCreditAmount().getAmount());
        entity.setContextVariables(WindJson.toJsonString(transaction.getContextVariables()));
        return entity;
    }

    private LedgerAggregate materializeLedgerAggregate(LedgerTransaction transaction,
                                                        LedgerTransactionSpec transactionSpec,
                                                        @Nullable LedgerAggregate existingAggregate) {
        Map<String, List<String>> existingEntrySnsByPlan = existingEntrySnsByPlan(existingAggregate);
        if (existingAggregate != null) {
            AssertUtils.isTrue(existingAggregate.postingPlans().size() == transactionSpec.getPostingPlans().size(),
                    "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", transaction.getSn());
        }
        List<LedgerPostingPlan> postingPlans = new ArrayList<>();
        List<LedgerEntry> entries = new ArrayList<>();
        for (LedgerPostingPlanSpec planSpec : transactionSpec.getPostingPlans()) {
            LedgerPostingPlan postingPlan = materializePostingPlan(transaction, planSpec);
            List<String> existingEntrySns = existingAggregate == null
                    ? List.of()
                    : existingEntrySnsByPlan.get(planSpec.getPlanId());
            if (existingAggregate != null) {
                AssertUtils.notNull(existingEntrySns,
                        "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", transaction.getSn());
            }
            int entryIndex = 0;
            for (LedgerPostingPhaseSpec phase : planSpec.getPostingPhases()) {
                for (LedgerEntrySpec entrySpec : phase.getEntries()) {
                    String entrySn = existingAggregate == null
                            ? TemporalSequenceFactory.hourNext(LEDGER_ENTRY_SEQUENCE_TYPE)
                            : replayEntrySn(existingEntrySns, entryIndex, transaction.getSn());
                    LedgerEntry entry = materializeLedgerEntry(
                            transaction, planSpec, phase, postingPlan.getSn(), entrySpec, entrySn);
                    entry.setSha256(FundsStableHashSupport.sha256CanonicalJson(
                            LEDGER_ENTRY_DIGEST_DOMAIN, ledgerEntryDigestFacts(entry)));
                    entries.add(entry);
                    entryIndex++;
                }
            }
            if (existingAggregate != null) {
                AssertUtils.isTrue(entryIndex == existingEntrySns.size(),
                        "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", transaction.getSn());
            }
            postingPlan.setSha256(FundsStableHashSupport.sha256CanonicalJson(
                    LEDGER_POSTING_PLAN_DIGEST_DOMAIN, postingPlanDigestFacts(postingPlan)));
            postingPlans.add(postingPlan);
        }
        transaction.setSha256(FundsStableHashSupport.sha256CanonicalJson(
                LEDGER_TRANSACTION_DIGEST_DOMAIN,
                ledgerAggregateDigestFacts(transaction, postingPlans, entries)));
        return new LedgerAggregate(transaction, postingPlans, entries);
    }

    private Map<String, List<String>> existingEntrySnsByPlan(@Nullable LedgerAggregate existingAggregate) {
        if (existingAggregate == null) {
            return Map.of();
        }
        return existingAggregate.entries().stream()
                .collect(Collectors.groupingBy(
                        LedgerEntry::getPostingPlanSn,
                        TreeMap::new,
                        Collectors.mapping(LedgerEntry::getSn, Collectors.toList())));
    }

    private String replayEntrySn(List<String> existingEntrySns, int entryIndex, String ledgerTransactionSn) {
        AssertUtils.isTrue(entryIndex < existingEntrySns.size(),
                "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", ledgerTransactionSn);
        return existingEntrySns.get(entryIndex);
    }

    private Map<String, Object> ledgerTransactionDigestFacts(LedgerTransaction entity) {
        Map<String, Object> transactionFacts = new TreeMap<>();
        transactionFacts.put("sn", entity.getSn());
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
        transactionFacts.put("transactionTime", entity.getTransactionTime().truncatedTo(ChronoUnit.SECONDS));
        transactionFacts.put("referenceLedgerTransactionSn", entity.getReferenceLedgerTransactionSn());
        assertDigestFields(LEDGER_TRANSACTION_SHA256_FIELDS, transactionFacts, "transaction", entity.getSn());
        return transactionFacts;
    }

    private Map<String, Object> postingPlanDigestFacts(LedgerPostingPlan plan) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", plan.getSn());
        facts.put("tenantId", plan.getTenantId());
        facts.put("ledgerTransactionSn", plan.getLedgerTransactionSn());
        facts.put("fundsTransactionSn", plan.getFundsTransactionSn());
        facts.put("routeLegId", plan.getRouteLegId());
        facts.put("intent", plan.getIntent());
        facts.put("postingScope", plan.getPostingScope());
        facts.put("balanceEffectType", plan.getBalanceEffectType());
        facts.put("phaseCode", plan.getPhaseCode());
        facts.put("amount", plan.getAmount());
        facts.put("currency", plan.getCurrency());
        facts.put("debitAmount", plan.getDebitAmount());
        facts.put("creditAmount", plan.getCreditAmount());
        assertDigestFields(LEDGER_POSTING_PLAN_SHA256_FIELDS, facts, "posting plan", plan.getSn());
        return facts;
    }

    private Map<String, Object> ledgerEntryDigestFacts(LedgerEntry entry) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", entry.getSn());
        facts.put("tenantId", entry.getTenantId());
        facts.put("ledgerTransactionSn", entry.getLedgerTransactionSn());
        facts.put("postingPlanSn", entry.getPostingPlanSn());
        facts.put("fundsTransactionSn", entry.getFundsTransactionSn());
        facts.put("ledgerId", entry.getLedgerId());
        facts.put("periodType", entry.getPeriodType());
        facts.put("periodId", entry.getPeriodId());
        facts.put("subjectId", entry.getSubjectId());
        facts.put("subjectType", entry.getSubjectType());
        facts.put("ledgerSubjectCode", entry.getLedgerSubjectCode());
        facts.put("ledgerSubjectCategory", entry.getLedgerSubjectCategory());
        facts.put("entrySide", entry.getEntrySide());
        facts.put("postingRole", entry.getPostingRole());
        facts.put("balanceConstraintType", entry.getBalanceConstraintType());
        facts.put("intent", entry.getIntent());
        facts.put("postingScope", entry.getPostingScope());
        facts.put("balanceEffectType", entry.getBalanceEffectType());
        facts.put("phaseCode", entry.getPhaseCode());
        facts.put("businessScene", entry.getBusinessScene());
        facts.put("businessSn", entry.getBusinessSn());
        facts.put("amount", entry.getAmount());
        facts.put("currency", entry.getCurrency());
        facts.put("originalAmount", entry.getOriginalAmount());
        facts.put("originalCurrency", entry.getOriginalCurrency());
        facts.put("exchangeRate", entry.getExchangeRate());
        facts.put("transactionTime", entry.getTransactionTime().truncatedTo(ChronoUnit.SECONDS));
        assertDigestFields(LEDGER_ENTRY_SHA256_FIELDS, facts, "ledger entry", entry.getSn());
        return facts;
    }

    private Map<String, Object> ledgerAggregateDigestFacts(LedgerTransaction transaction,
                                                           List<LedgerPostingPlan> postingPlans,
                                                           List<LedgerEntry> entries) {
        Map<String, List<LedgerEntry>> entriesByPlan = entries.stream()
                .sorted(Comparator.comparing(LedgerEntry::getSn))
                .collect(Collectors.groupingBy(
                        LedgerEntry::getPostingPlanSn,
                        TreeMap::new,
                        Collectors.toList()));
        List<Map<String, Object>> planAggregates = postingPlans.stream()
                .sorted(Comparator.comparing(LedgerPostingPlan::getSn))
                .map(plan -> {
                    Map<String, Object> aggregate = new TreeMap<>();
                    aggregate.put("plan", postingPlanDigestFacts(plan));
                    aggregate.put("entries", entriesByPlan.getOrDefault(plan.getSn(), List.of())
                            .stream()
                            .map(this::ledgerEntryDigestFacts)
                            .toList());
                    return aggregate;
                })
                .toList();
        Map<String, Object> facts = new TreeMap<>();
        facts.put("transaction", ledgerTransactionDigestFacts(transaction));
        facts.put("postingPlans", planAggregates);
        return facts;
    }

    private void assertDigestFields(List<String> expectedFields,
                                    Map<String, Object> facts,
                                    String layer,
                                    String sn) {
        AssertUtils.isTrue(facts.keySet().equals(Set.copyOf(expectedFields)),
                "持久化 {} 摘要字段集不一致，sn = {}", layer, sn);
    }

    private LedgerTransactionPostResult resolveExistingLedgerTransaction(LedgerTransaction requested,
                                                                          LedgerTransactionSpec transaction) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(requested.getSn()));
        LedgerTransaction existing = ledgerTransactionMapper.selectOneByQuery(wrapper);
        if (existing == null) {
            return null;
        }
        LedgerAggregate existingAggregate = verifiedLedgerAggregate(existing);
        LedgerAggregate replayAggregate = materializeLedgerAggregate(requested, transaction, existingAggregate);
        AssertUtils.isTrue(existing.getSha256().equals(replayAggregate.transaction().getSha256()),
                "账本交易已存在但摘要不一致，ledgerTransactionSn = {}", requested.getSn());
        return toPostResult(existing.getId(), false);
    }

    private LedgerTransactionPostResult toPostResult(Long ledgerTransactionId, boolean newlyPosted) {
        return new LedgerTransactionPostResult()
                .setLedgerTransactionId(ledgerTransactionId)
                .setNewlyPosted(newlyPosted);
    }

    private LedgerPostingPlan materializePostingPlan(LedgerTransaction transaction, LedgerPostingPlanSpec plan) {
        Money amount = plan.getAmount();
        String phaseCode = resolvePhaseCode(plan);
        LedgerPostingPlan entity = new LedgerPostingPlan();
        entity.setSn(plan.getPlanId());
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
        return entity;
    }

    private String resolvePhaseCode(LedgerPostingPlanSpec plan) {
        return plan.getPostingPhases()
                .stream()
                .map(LedgerPostingPhaseSpec::getPhaseCode)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private LedgerEntry materializeLedgerEntry(LedgerTransaction transaction,
                                               LedgerPostingPlanSpec plan,
                                               LedgerPostingPhaseSpec phase,
                                               String postingPlanSn,
                                               LedgerEntrySpec entry,
                                               String entrySn) {
        LedgerEntry ledgerEntry = LedgerConverter.INSTANCE.convertToLedgerEntry(entry);
        ledgerEntry.setSn(entrySn);
        ledgerEntry.setTenantId(transaction.getTenantId());
        ledgerEntry.setLedgerTransactionSn(transaction.getSn());
        ledgerEntry.setPostingPlanSn(postingPlanSn);
        ledgerEntry.setFundsTransactionSn(transaction.getFundsTransactionSn());
        ledgerEntry.setLedgerId(defaultIfNull(ledgerEntry.getLedgerId(), 0L));
        ledgerEntry.setTransactionTime(ledgerEntry.getTransactionTime().truncatedTo(ChronoUnit.SECONDS));
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
        return ledgerEntry;
    }

    private LedgerAggregate verifiedLedgerAggregate(LedgerTransaction transaction) {
        List<LedgerPostingPlan> postingPlans = findLedgerPostingPlans(
                transaction.getTenantId(), transaction.getSn());
        List<LedgerEntry> entries = findLedgerEntries(transaction.getTenantId(), transaction.getSn());
        Map<String, LedgerPostingPlan> postingPlansBySn = new TreeMap<>();
        for (LedgerPostingPlan postingPlan : postingPlans) {
            assertPostingPlanParent(transaction, postingPlan);
            AssertUtils.isTrue(postingPlansBySn.put(postingPlan.getSn(), postingPlan) == null,
                    "持久化 posting plan 身份重复，sn = {}", postingPlan.getSn());
            assertStoredDigest(
                    postingPlan.getSha256(),
                    LEDGER_POSTING_PLAN_DIGEST_DOMAIN,
                    postingPlanDigestFacts(postingPlan),
                    "posting plan",
                    postingPlan.getSn());
        }
        for (LedgerEntry entry : entries) {
            assertLedgerEntryParent(transaction, postingPlansBySn, entry);
            assertStoredDigest(
                    entry.getSha256(),
                    LEDGER_ENTRY_DIGEST_DOMAIN,
                    ledgerEntryDigestFacts(entry),
                    "ledger entry",
                    entry.getSn());
        }
        assertStoredDigest(
                transaction.getSha256(),
                LEDGER_TRANSACTION_DIGEST_DOMAIN,
                ledgerAggregateDigestFacts(transaction, postingPlans, entries),
                "transaction",
                transaction.getSn());
        return new LedgerAggregate(transaction, postingPlans, entries);
    }

    private void assertPostingPlanParent(LedgerTransaction transaction, LedgerPostingPlan postingPlan) {
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), postingPlan.getTenantId())
                        && Objects.equals(transaction.getSn(), postingPlan.getLedgerTransactionSn())
                        && Objects.equals(transaction.getFundsTransactionSn(), postingPlan.getFundsTransactionSn()),
                "持久化 posting plan 父引用不一致，sn = {}", postingPlan.getSn());
    }

    private void assertLedgerEntryParent(LedgerTransaction transaction,
                                         Map<String, LedgerPostingPlan> postingPlansBySn,
                                         LedgerEntry entry) {
        AssertUtils.isTrue(entry.getPostingPlanSn() != null
                        && postingPlansBySn.containsKey(entry.getPostingPlanSn())
                        && Objects.equals(transaction.getTenantId(), entry.getTenantId())
                        && Objects.equals(transaction.getSn(), entry.getLedgerTransactionSn())
                        && Objects.equals(transaction.getFundsTransactionSn(), entry.getFundsTransactionSn()),
                "持久化 ledger entry 父引用不一致，sn = {}", entry.getSn());
    }

    private void assertStoredDigest(String storedDigest,
                                    String domain,
                                    Map<String, Object> facts,
                                    String layer,
                                    String sn) {
        AssertUtils.isTrue(Objects.equals(storedDigest, FundsStableHashSupport.sha256CanonicalJson(domain, facts)),
                "持久化 {} 摘要不一致，sn = {}", layer, sn);
    }

    private List<LedgerPostingPlan> findLedgerPostingPlans(Long tenantId, String ledgerTransactionSn) {
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        return ledgerPostingPlanMapper.selectListByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ledgerTransactionSn.eq(ledgerTransactionSn))
                .orderBy(ref.sn.asc()));
    }

    private List<LedgerEntry> findLedgerEntries(Long tenantId, String ledgerTransactionSn) {
        LedgerEntryNameRefs ref = LedgerEntryNameRefs.ledgerEntry;
        return ledgerEntryMapper.selectListByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ledgerTransactionSn.eq(ledgerTransactionSn))
                .orderBy(ref.sn.asc()));
    }

    private LedgerAggregate verifiedLedgerAggregateBySn(Long tenantId, String ledgerTransactionSn) {
        LedgerTransaction transaction = findLedgerTransactionBySn(tenantId, ledgerTransactionSn);
        AssertUtils.notNull(transaction,
                "账户账本交易不存在，tenantId = {}, sn = {}", tenantId, ledgerTransactionSn);
        return verifiedLedgerAggregate(transaction);
    }

    private @Nullable LedgerTransaction findLedgerTransactionBySn(Long tenantId, String sn) {
        LedgerTransactionNameRefs ref = LedgerTransactionNameRefs.ledgerTransaction;
        return ledgerTransactionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(sn)));
    }

    private LedgerEntry verifiedLedgerEntry(LedgerEntry entry) {
        LedgerAggregate aggregate = verifiedLedgerAggregateBySn(
                entry.getTenantId(), entry.getLedgerTransactionSn());
        assertAggregateContainsEntry(aggregate, entry);
        return entry;
    }

    private void assertAggregateContainsEntry(LedgerAggregate aggregate, LedgerEntry entry) {
        AssertUtils.isTrue(aggregate.entries().stream().anyMatch(persisted ->
                        Objects.equals(persisted.getId(), entry.getId())
                                && Objects.equals(persisted.getSn(), entry.getSn())),
                "持久化 ledger entry 父引用不一致，sn = {}", entry.getSn());
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
    public LedgerTransactionDTO getLedgerTransactionBySn(@NonNull Long tenantId, @NonNull String sn) {
        AssertUtils.notNull(tenantId, "账本交易查询 tenantId 不能为空");
        AssertUtils.hasText(sn, "账本交易流水号不能为空");
        return LedgerConverter.INSTANCE.convertToAccountLedgerTransactionDTO(
                verifiedLedgerAggregateBySn(tenantId, sn).transaction());
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

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
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
        return LedgerConverter.INSTANCE.convertToLedgerEntryDTO(verifiedLedgerEntry(result));
    }

    @Override
    public boolean existsPostingPlan(@NonNull Long tenantId,
                                     @NonNull String postingPlanSn,
                                     @NonNull String ledgerTransactionSn) {
        AssertUtils.notNull(tenantId, "记账计划查询 tenantId 不能为空");
        AssertUtils.hasText(postingPlanSn, "记账计划流水号不能为空");
        AssertUtils.hasText(ledgerTransactionSn, "账本交易流水号不能为空");
        LedgerPostingPlanNameRefs ref = LedgerPostingPlanNameRefs.ledgerPostingPlan;
        LedgerPostingPlan postingPlan = ledgerPostingPlanMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(postingPlanSn))
                .and(ref.ledgerTransactionSn.eq(ledgerTransactionSn)));
        if (postingPlan == null) {
            return false;
        }
        LedgerAggregate aggregate = verifiedLedgerAggregateBySn(tenantId, ledgerTransactionSn);
        return aggregate.postingPlans().stream()
                .anyMatch(persisted -> Objects.equals(persisted.getId(), postingPlan.getId())
                        && Objects.equals(persisted.getSn(), postingPlanSn));
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

        Map<String, LedgerAggregate> verifiedAggregates = new HashMap<>();
        return MybatisQueryHelper.<LedgerEntry, LedgerEntryDTO>query(queryWrapper)
                .counter(ledgerEntryMapper::selectCountByQuery)
                .resultQueryFunc(ledgerEntryMapper::selectListByQuery)
                .converter(entry -> {
                    String aggregateKey = entry.getTenantId() + ":" + entry.getLedgerTransactionSn();
                    LedgerAggregate aggregate = verifiedAggregates.computeIfAbsent(
                            aggregateKey,
                            ignored -> verifiedLedgerAggregateBySn(
                                    entry.getTenantId(), entry.getLedgerTransactionSn()));
                    assertAggregateContainsEntry(aggregate, entry);
                    return LedgerConverter.INSTANCE.convertToLedgerEntryDTO(entry);
                })
                .query(options);
    }

    /**
     * Ledger 持久化聚合的内部校验载体。
     *
     * @param transaction  账本交易根事实
     * @param postingPlans 记账计划事实
     * @param entries      账本分录事实
     * @author wuxp
     * @since 2026-08-23
     */
    private record LedgerAggregate(LedgerTransaction transaction,
                                   List<LedgerPostingPlan> postingPlans,
                                   List<LedgerEntry> entries) {

        private LedgerAggregate {
            postingPlans = List.copyOf(postingPlans);
            entries = List.copyOf(entries);
        }
    }
}
