package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerTransactionDigestContractTests extends LedgerTransactionServiceImplTestSupport {

    /**
     * 场景：同一业务事实可能因为重放、迁移或修复重新生成账本交易流水。
     * 输入：保存账本交易后修改 transactionSn、fundsTransactionSn 和审计时间。
     * 输出：交易级 sha256 摘要。
     * 预期：摘要只绑定稳定业务事实字段，不随本次持久化流水或审计字段变化。
     * 红线：摘要不得绑定数据库自增 ID、审计时间或临时流水。
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
}
