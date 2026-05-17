package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerPostingPlanDigestContractTests extends LedgerTransactionServiceImplTestSupport {

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

    /**
     * 场景：同一账务计划语义来自不同 route leg。
     * 输入：保存带 routeLegId 的 PostingPlan 后修改 routeLegId。
     * 输出：记账计划级 sha256 摘要。
     * 预期：routeLegId 作为稳定来源 leg 引用，变化会改变摘要。
     * 红线：PostingPlan 摘要不得只依赖 JSON 上下文，导致来源 leg 被篡改后不可发现。
     */
    @Test
    void testPostingPlanSha256ShouldIncludeRouteLegId() {
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
                .postingPlans(List.of(postingPlanWithRouteLeg(phase, "LEG_001", Map.of())))
                .contextVariables(Map.of())
                .build();

        service.createLedgerTransaction(transaction);

        LedgerPostingPlan plan = insertedPlan.get();
        String originalHash = plan.getSha256();
        plan.setRouteLegId("LEG_002");

        assertThat(stablePostingPlanHash(plan)).isNotEqualTo(originalHash);
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
}
