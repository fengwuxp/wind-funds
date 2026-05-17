package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.dto.LedgerTransactionCreateResult;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerTransactionServiceImplTests extends LedgerTransactionServiceImplTestSupport {

    /**
     * 场景：账本交易落库时需要保存 route leg 级追踪信息。
     * 输入：posting plan 携带 routeLegId、replayRefLegId 和 replayPolicy。
     * 输出：账本交易、posting plan 和 entry 持久化模型。
     * 预期：plan 级上下文覆盖 transaction 级同名上下文，entry 继承核心账务元数据。
     * 红线：route leg 追踪信息不得丢失，也不得被 transaction 级默认值误覆盖。
     */
    @Test
    void testCreateLedgerTransactionShouldPersistPostingPlanAndEntryMetadata() {
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
        LedgerPostingPlanSpec plan = postingPlanWithRouteLeg(phase, "LEG_001", Map.of(
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

        LedgerTransactionCreateResult result = service.createLedgerTransaction(transaction);

        assertThat(result.getLedgerTransactionId()).isEqualTo(insertedTransaction.get().getId());
        assertThat(result.isCreated()).isTrue();
        assertThat(insertedTransaction.get().getEventType()).isEqualTo(FundsTransactionEventType.TOPUP.name());
        assertThat(insertedPlan.get().getSn()).isEqualTo(plan.getPlanId());
        assertThat(insertedPlan.get().getTenantId()).isEqualTo(1L);
        assertThat(insertedPlan.get().getIntent()).isEqualTo(LedgerPostingIntentType.TRANSFER.name());
        assertThat(insertedPlan.get().getRouteLegId()).isEqualTo("LEG_001");
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
}
