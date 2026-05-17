package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.ledger.dal.mapper.LedgerEntryMapper;
import com.capte.funds.ledger.dal.mapper.LedgerPostingPlanMapper;
import com.capte.funds.ledger.dal.mapper.LedgerTransactionMapper;
import com.capte.funds.ledger.dto.LedgerTransactionPostResult;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerTransactionServiceIdempotencyTests extends LedgerTransactionServiceImplTestSupport {

    /**
     * 场景：账本交易入账后收到同一业务事实的幂等重放。
     * 输入：相同 ledger transaction sn、相同稳定摘要和相同 posting plan。
     * 输出：已有账本交易 ID 和 newlyPosted=false。
     * 预期：不重复写入账本交易、posting plan 或 entry。
     * 红线：幂等重放不得制造重复账本事实或重复余额影响。
     */
    @Test
    void testPostLedgerTransactionShouldReturnExistingIdWhenSameSnAndSha256() {
        AtomicReference<LedgerTransaction> existingTransaction = new AtomicReference<>();
        List<LedgerTransaction> insertedTransactions = new ArrayList<>();
        List<LedgerPostingPlan> insertedPlans = new ArrayList<>();
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> {
                    LedgerTransaction transaction = (LedgerTransaction) entity;
                    transaction.setId(idSequence.incrementAndGet());
                    existingTransaction.set(transaction);
                    insertedTransactions.add(transaction);
                }, existingTransaction::get),
                mapper(LedgerPostingPlanMapper.class, entity -> {
                    LedgerPostingPlan plan = (LedgerPostingPlan) entity;
                    plan.setId(idSequence.incrementAndGet());
                    insertedPlans.add(plan);
                }),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_DUPLICATE")
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

        LedgerTransactionPostResult firstResult = service.postLedgerTransaction(transaction);
        insertedTransactions.clear();
        insertedPlans.clear();
        insertedEntries.clear();
        LedgerTransactionPostResult replayResult = service.postLedgerTransaction(transaction);

        assertThat(firstResult.isNewlyPosted()).isTrue();
        assertThat(replayResult.getLedgerTransactionId()).isEqualTo(firstResult.getLedgerTransactionId());
        assertThat(replayResult.isNewlyPosted()).isFalse();
        assertThat(insertedTransactions).isEmpty();
        assertThat(insertedPlans).isEmpty();
        assertThat(insertedEntries).isEmpty();
    }

    /**
     * 场景：相同账本交易流水号被不同业务事实复用。
     * 输入：数据库已有 ledger transaction sn，但新请求稳定摘要不同。
     * 输出：摘要冲突异常。
     * 预期：拒绝本次创建，且不写入 transaction、posting plan 或 entry。
     * 红线：同一账本交易流水号不得指向两份不同资金事实。
     */
    @Test
    void testCreateLedgerTransactionShouldRejectDuplicateSnWhenSha256Conflicts() {
        LedgerTransaction existingTransaction = new LedgerTransaction();
        existingTransaction.setId(101L);
        existingTransaction.setSn("LEDGER_TXN_DUPLICATE");
        existingTransaction.setSha256("different-ledger-transaction-sha256");
        List<LedgerTransaction> insertedTransactions = new ArrayList<>();
        List<LedgerPostingPlan> insertedPlans = new ArrayList<>();
        List<LedgerEntry> insertedEntries = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(100L);
        LedgerTransactionServiceImpl service = new LedgerTransactionServiceImpl(
                mapper(LedgerTransactionMapper.class, entity -> {
                    LedgerTransaction transaction = (LedgerTransaction) entity;
                    transaction.setId(idSequence.incrementAndGet());
                    insertedTransactions.add(transaction);
                }, () -> existingTransaction),
                mapper(LedgerPostingPlanMapper.class, entity -> {
                    LedgerPostingPlan plan = (LedgerPostingPlan) entity;
                    plan.setId(idSequence.incrementAndGet());
                    insertedPlans.add(plan);
                }),
                mapper(LedgerEntryMapper.class, entity -> {
                    LedgerEntry entry = (LedgerEntry) entity;
                    entry.setId(idSequence.incrementAndGet());
                    insertedEntries.add(entry);
                })
        );
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(LedgerPhaseCode.TRANSFER,
                List.of(entry("user_001", EntrySide.DEBIT), entry("user_002", EntrySide.CREDIT)));
        LedgerTransactionSpec transaction = LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn("LEDGER_TXN_DUPLICATE")
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

        assertThatThrownBy(() -> service.createLedgerTransaction(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易已存在但摘要不一致")
                .hasMessageContaining("LEDGER_TXN_DUPLICATE");
        assertThat(insertedTransactions).isEmpty();
        assertThat(insertedPlans).isEmpty();
        assertThat(insertedEntries).isEmpty();
    }
}
