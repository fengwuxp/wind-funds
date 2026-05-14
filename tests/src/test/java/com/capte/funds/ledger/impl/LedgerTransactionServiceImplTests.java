package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.mybatisflex.core.BaseMapper;
import com.wind.common.util.WindObjectDigestUtils;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerReconcileStatus;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerTransactionServiceImplTests {

    @Test
    void createLedgerTransactionShouldPersistPostingPlanAndEntryMetadata() {
        AtomicReference<LedgerTransaction> insertedTransaction = new AtomicReference<>();
        AtomicReference<LedgerPostingPlan> insertedPlan = new AtomicReference<>();
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> {
                    LedgerTransaction transaction = (LedgerTransaction) entity;
                    transaction.setId(idSequence.incrementAndGet());
                    insertedTransaction.set(transaction);
                }),
                mapper(LedgerPostingPlanMapper.class, entity -> {
                    LedgerPostingPlan plan = (LedgerPostingPlan) entity;
                    plan.setId(idSequence.incrementAndGet());
                    insertedPlan.set(plan);
                }),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerPostingPlanSpec plan = postingPlanWithContext(phase, Map.of(
                "routeLegId", "LEG_001",
                "replayRefLegId", "SOURCE_LEG_001",
                "replayPolicy", "REPLAY_ONCE"
        ));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(plan))
                .contextVariables(Map.of(
                        "requestSource", "wallet-web",
                        "routeLegId", "TRANSACTION_LEVEL_LEG"
                ))
                .build();

        Long id = service.createLedgerTransaction(transaction);

        assertThat(id).isEqualTo(insertedTransaction.get().getId());
        assertThat(insertedTransaction.get().getEventType()).isEqualTo(FundsTransactionEventType.TOPUP.name());
        assertThat(insertedPlan.get().getSn()).isEqualTo(plan.getPlanId());
        assertThat(insertedPlan.get().getTenantId()).isEqualTo(1L);
        assertThat(insertedPlan.get().getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER.name());
        assertThat(insertedPlan.get().getPhaseCode()).isEqualTo(LedgerPhaseCode.TRANSFER.name());
        assertThat(insertedPlan.get().getPostingScope()).isEqualTo(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        assertThat(insertedPlan.get().getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME.name());
        assertThat(insertedPlan.get().getBalanced()).isTrue();
        assertThat(insertedPlan.get().getContextVariables())
                .contains("\"requestSource\":\"wallet-web\"")
                .contains("\"routeLegId\":\"LEG_001\"")
                .contains("\"replayRefLegId\":\"SOURCE_LEG_001\"")
                .contains("\"replayPolicy\":\"REPLAY_ONCE\"");
        assertThat(insertedPlan.get().getContextVariables()).doesNotContain("TRANSACTION_LEVEL_LEG");
        assertThat(insertedEntries).hasSize(2);
        assertThat(insertedEntries).extracting(LedgerEntry::getPostingPlanSn).containsOnly(plan.getPlanId());
        assertThat(insertedEntries).extracting(LedgerEntry::getTenantId).containsOnly(1L);
        assertThat(insertedEntries).extracting(LedgerEntry::getSubjectId).containsExactly("user_001", "user_002");
        assertThat(insertedEntries).extracting(LedgerEntry::getSubjectType)
                .containsOnly(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getIntent)
                .containsOnly(LedgerPostingIntentType.TRANSFER.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getPhaseCode)
                .containsOnly(LedgerPhaseCode.TRANSFER.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getPostingScope)
                .containsOnly(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getBalanceEffectType)
                .containsOnly(LedgerBalanceEffectType.CONSUME.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getBalanceConstraintType)
                .containsOnly(LedgerBalanceConstraintType.PROFILE_DEFAULT.name());
        assertThat(insertedEntries).extracting(LedgerEntry::getSn).doesNotHaveDuplicates();
        assertThat(insertedEntries).extracting(LedgerEntry::getSha256).allSatisfy(hash -> assertThat(hash).isNotNull());
    }

    /**
     * 场景：同一业务事实可能因为重放、迁移或修复重新生成账本交易流水。
     * 输入：保存账本交易后修改 transactionSn、fundsTransactionSn 和审计时间。
     * 输出：交易级 sha256 摘要。
     * 预期：摘要只绑定稳定业务事实字段，不随本次持久化流水或审计字段变化。
     */
    @Test
    void testTransactionSha256ShouldUseStableFields() {
        AtomicReference<LedgerTransaction> insertedTransaction = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> {
                    LedgerTransaction transaction = (LedgerTransaction) entity;
                    transaction.setId(idSequence.incrementAndGet());
                    insertedTransaction.set(transaction);
                }),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> ((LedgerEntry) entity)
                        .setId(idSequence.incrementAndGet()))
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .fundsTransactionSn("FUNDS_TXN_0001")
                .referenceLedgerTransactionSn("REFERENCE_LEDGER_TXN_0001")
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(110L, CurrencyIsoCode.EUR))
                .exchangeRate(new BigDecimal("1.10"))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerTransaction inserted = insertedTransaction.get();
        String originalHash = inserted.getSha256();
        inserted.setSn("LEDGER_TXN_REPLAY_0002");
        inserted.setFundsTransactionSn("FUNDS_TXN_REPLAY_0002");
        inserted.setId(999L);
        inserted.setGmtCreate(LocalDateTime.of(2026, 5, 8, 11, 0));
        inserted.setGmtModified(LocalDateTime.of(2026, 5, 8, 11, 30));

        assertThat(stableTransactionHash(inserted)).isEqualTo(originalHash);
    }

    @Test
    void testTransactionSha256ShouldIncludeReferenceLedgerTransactionSn() {
        AtomicReference<LedgerTransaction> insertedTransaction = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> {
                    LedgerTransaction transaction = (LedgerTransaction) entity;
                    transaction.setId(idSequence.incrementAndGet());
                    insertedTransaction.set(transaction);
                }),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> ((LedgerEntry) entity)
                        .setId(idSequence.incrementAndGet()))
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .fundsTransactionSn("FUNDS_TXN_0001")
                .referenceLedgerTransactionSn("REFERENCE_LEDGER_TXN_0001")
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerTransaction inserted = insertedTransaction.get();
        String originalHash = inserted.getSha256();
        inserted.setReferenceLedgerTransactionSn("REFERENCE_LEDGER_TXN_0002");

        assertThat(stableTransactionHash(inserted)).isNotEqualTo(originalHash);
    }

    /**
     * 场景：同一 RouteLeg 可能在不同账本交易流水下重新形成记账计划。
     * 输入：保存账本交易后修改 PostingPlan 的 sn、ledgerTransactionSn、fundsTransactionSn 和审计时间。
     * 输出：记账计划级 sha256 摘要。
     * 预期：摘要只绑定稳定计划语义，不随本次持久化流水变化。
     */
    @Test
    void testPostingPlanSha256ShouldUseStableFields() {
        AtomicReference<LedgerPostingPlan> insertedPlan = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> {
                    LedgerPostingPlan plan = (LedgerPostingPlan) entity;
                    plan.setId(idSequence.incrementAndGet());
                    insertedPlan.set(plan);
                }),
                mapper(LedgerEntryMapper.class, entity -> ((LedgerEntry) entity)
                        .setId(idSequence.incrementAndGet()))
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .fundsTransactionSn("FUNDS_TXN_0001")
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerPostingPlan plan = insertedPlan.get();
        String originalHash = plan.getSha256();
        plan.setSn("TRANSFER_LEDGER_TXN_REPLAY_0002");
        plan.setLedgerTransactionSn("LEDGER_TXN_REPLAY_0002");
        plan.setFundsTransactionSn("FUNDS_TXN_REPLAY_0002");
        plan.setId(999L);
        plan.setGmtCreate(LocalDateTime.of(2026, 5, 8, 11, 0));
        plan.setGmtModified(LocalDateTime.of(2026, 5, 8, 11, 30));

        assertThat(stablePostingPlanHash(plan)).isEqualTo(originalHash);
    }

    @Test
    void testPostingPlanSha256ShouldIncludeScopeAndEffect() {
        AtomicReference<LedgerPostingPlan> insertedPlan = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> {
                    LedgerPostingPlan plan = (LedgerPostingPlan) entity;
                    plan.setId(idSequence.incrementAndGet());
                    insertedPlan.set(plan);
                }),
                mapper(LedgerEntryMapper.class, entity -> ((LedgerEntry) entity)
                        .setId(idSequence.incrementAndGet()))
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .fundsTransactionSn("FUNDS_TXN_0001")
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerPostingPlan plan = insertedPlan.get();
        String originalHash = plan.getSha256();
        plan.setPostingScope(LedgerPostingScope.WITHIN_SUBJECT.name());
        assertThat(stablePostingPlanHash(plan)).isNotEqualTo(originalHash);

        plan.setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        plan.setBalanceEffectType(LedgerBalanceEffectType.RESTORE.name());
        assertThat(stablePostingPlanHash(plan)).isNotEqualTo(originalHash);
    }

    /**
     * 场景：账本分录摘要需要覆盖汇率，防止跨币种事实在重建时丢失汇率差异。
     * 输入：包含原币 EUR 和汇率 1.10 的账本分录。
     * 输出：修改汇率前后的分录级 sha256 摘要。
     * 预期：汇率变化会改变摘要，说明汇率属于稳定业务事实的一部分。
     */
    @Test
    void testEntrySha256ShouldIncludeExchangeRate() {
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(110L, CurrencyIsoCode.EUR))
                .exchangeRate(new BigDecimal("1.10"))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerEntry firstEntry = insertedEntries.getFirst();
        String originalHash = firstEntry.getSha256();
        firstEntry.setExchangeRate(new BigDecimal("1.20"));

        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);
    }

    @Test
    void testEntrySha256ShouldIncludeAccountingSemantics() {
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerEntry firstEntry = insertedEntries.getFirst();
        String originalHash = firstEntry.getSha256();
        firstEntry.setLedgerSubjectCategory(LedgerSubjectCategory.ASSET);
        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);

        firstEntry.setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY);
        firstEntry.setIntent(LedgerPostingIntentType.FEE.name());
        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);

        firstEntry.setIntent(LedgerPostingIntentType.TRANSFER.name());
        firstEntry.setPostingScope(LedgerPostingScope.WITHIN_SUBJECT.name());
        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);

        firstEntry.setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        firstEntry.setBalanceEffectType(LedgerBalanceEffectType.RESTORE.name());
        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);

        firstEntry.setBalanceEffectType(LedgerBalanceEffectType.CONSUME.name());
        firstEntry.setPhaseCode(LedgerPhaseCode.FEE.name());
        assertThat(stableEntryHash(firstEntry)).isNotEqualTo(originalHash);
    }

    @Test
    void testEntrySha256ShouldUseStableFields() {
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> ((LedgerTransaction) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerPostingPlanMapper.class, entity -> ((LedgerPostingPlan) entity)
                        .setId(idSequence.incrementAndGet())),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_0001")
                .tenantId(1L)
                .eventType(FundsTransactionEventType.TOPUP)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .businessScene("TRANSFER_TEST")
                .businessSn("BUSINESS_SN_0001")
                .transactionTime(LocalDateTime.of(2026, 5, 7, 10, 0))
                .postingPlans(List.of(postingPlanWithContext(phase, Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerEntry firstEntry = insertedEntries.getFirst();
        String originalHash = firstEntry.getSha256();
        firstEntry.setSn("LGE999999");
        firstEntry.setId(999L);
        firstEntry.setGmtCreate(LocalDateTime.of(2026, 5, 8, 11, 0));
        firstEntry.setGmtModified(LocalDateTime.of(2026, 5, 8, 11, 30));
        firstEntry.setSettlementStatus(LedgerSettlementStatus.FAILED);
        firstEntry.setSettlementCompletedTime(LocalDateTime.of(2026, 5, 8, 12, 0));
        firstEntry.setReconcileStatus(LedgerReconcileStatus.MATCHED);
        firstEntry.setReconciliationBatch("RECON_202605080001");
        assertThat(stableEntryHash(firstEntry)).isEqualTo(originalHash);
    }

    private static LedgerPostingPlanSpec postingPlanWithContext(LedgerPostingPhaseSpec phase,
                                                                Map<String, Object> contextVariables) {
        LedgerPostingPlanSpec delegate = LedgerTransactionSpecFactory.postingPlan(
                LedgerPostingIntentType.TRANSFER, "LEDGER_TXN_0001", List.of(phase));
        return new LedgerPostingPlanSpec() {

            @Override
            public String getPlanId() {
                return delegate.getPlanId();
            }

            @Override
            public String getLedgerTransactionSn() {
                return delegate.getLedgerTransactionSn();
            }

            @Override
            public LedgerPostingIntentType getIntent() {
                return delegate.getIntent();
            }

            @Override
            public LedgerPostingScope getPostingScope() {
                return delegate.getPostingScope();
            }

            @Override
            public LedgerBalanceEffectType getBalanceEffectType() {
                return delegate.getBalanceEffectType();
            }

            @Override
            public List<LedgerPostingPhaseSpec> getPostingPhases() {
                return delegate.getPostingPhases();
            }

            @Override
            public Map<String, Object> getContextVariables() {
                return contextVariables;
            }
        };
    }

    private static LedgerEntrySpec entry(String subjectId, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                subjectId,
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                "LEDGER_TXN_0001",
                "TRANSFER_TEST",
                "BUSINESS_SN_0001",
                100L,
                CurrencyIsoCode.USD,
                LocalDateTime.of(2026, 5, 7, 10, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setContextVariables(Map.of());
    }

    private static String stableEntryHash(LedgerEntry entry) {
        return WindObjectDigestUtils.sha256WithNames(entry, List.of(
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
        ));
    }

    private static String stablePostingPlanHash(LedgerPostingPlan plan) {
        return WindObjectDigestUtils.sha256WithNames(plan, List.of(
                LedgerPostingPlan.Fields.tenantId,
                LedgerPostingPlan.Fields.intent,
                LedgerPostingPlan.Fields.postingScope,
                LedgerPostingPlan.Fields.balanceEffectType,
                LedgerPostingPlan.Fields.phaseCode,
                LedgerPostingPlan.Fields.amount,
                LedgerPostingPlan.Fields.currency,
                LedgerPostingPlan.Fields.debitAmount,
                LedgerPostingPlan.Fields.creditAmount
        ));
    }

    private static String stableTransactionHash(LedgerTransaction transaction) {
        return WindObjectDigestUtils.sha256WithNames(transaction, List.of(
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
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T extends BaseMapper<?>> T mapper(Class<T> mapperType, Consumer<Object> insertHandler) {
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return invokeObjectMethod(proxy, method, args);
                    }
                    if ("insertSelective".equals(method.getName())) {
                        insertHandler.accept(args[0]);
                        return 1;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "Proxy(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}
