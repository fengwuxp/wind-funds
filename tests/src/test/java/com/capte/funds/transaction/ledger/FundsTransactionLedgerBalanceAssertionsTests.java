package com.capte.funds.transaction.ledger;

import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsTransactionTestSupport;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertEntriesContain;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertPostingBalanced;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.entry;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsTransactionLedgerBalanceAssertionsTests {

    private static final Long TENANT_ID = 1L;

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 5, 14, 10, 0, 0);

    /**
     * 场景：钱包转账完成后需要同时验证账本分录和余额快照。
     * 输入：付款方 -400、收款方 +400 的 AVAILABLE 余额变化。
     * 输出：账本交易平衡、借贷分录存在、余额桶变化符合预期。
     * 预期：所有资金变化都能被分录和余额断言同时解释。
     */
    @Test
    void testTransferLedgerPostingShouldMatchBalanceDeltas() {
        FundsAccountId payer = fundingAccount("funding_payer");
        FundsAccountId payee = fundingAccount("funding_payee");
        LedgerTransactionSpec transaction = transferTransaction(payer, payee, 400L);

        BalanceSnapshot before = snapshot(
                balance(payer, LedgerSubjectCode.AVAILABLE, 1_000L),
                balance(payee, LedgerSubjectCode.AVAILABLE, 200L)
        );
        BalanceSnapshot after = snapshot(
                balance(payer, LedgerSubjectCode.AVAILABLE, 600L),
                balance(payee, LedgerSubjectCode.AVAILABLE, 600L)
        );

        assertPostingBalanced(transaction);
        assertEntriesContain(transaction,
                entry(payer, LedgerSubjectCode.AVAILABLE, EntrySide.DEBIT, 400L, CURRENCY),
                entry(payee, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, 400L, CURRENCY));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, -400L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 400L, CURRENCY));
    }

    /**
     * 场景：单个余额桶需要明确断言初始化状态、币种和金额。
     * 输入：已建账的 AVAILABLE 余额桶。
     * 输出：余额桶金额断言。
     * 预期：测试能区分“未建账”和“已建账但余额为 0”。
     */
    @Test
    void testBucketAssertionShouldCheckInitializedBalance() {
        FundsAccountId accountId = fundingAccount("funding_001");
        FundsSubjectBalanceDTO balance = balance(accountId, LedgerSubjectCode.AVAILABLE, 0L);

        assertBucket(balance, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
    }

    /**
     * 场景：余额变化与业务预期不一致。
     * 输入：付款方实际只减少 300，但预期减少 400。
     * 输出：断言失败。
     * 预期：测试能捕获遗漏入账或重复入账导致的金额偏差。
     */
    @Test
    void testBalanceDeltaAssertionShouldRejectUnexpectedDelta() {
        FundsAccountId payer = fundingAccount("funding_payer");
        BalanceSnapshot before = snapshot(balance(payer, LedgerSubjectCode.AVAILABLE, 1_000L));
        BalanceSnapshot after = snapshot(balance(payer, LedgerSubjectCode.AVAILABLE, 700L));

        assertThatThrownBy(() -> assertBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, -400L, CURRENCY)))
                .isInstanceOf(AssertionError.class);
    }

    private static LedgerTransactionSpec transferTransaction(FundsAccountId payer,
                                                             FundsAccountId payee,
                                                             long amount) {
        String ledgerTransactionSn = "LE202605140001";
        LedgerEntrySpec debitEntry = ledgerEntry(payer, EntrySide.DEBIT, ledgerTransactionSn, amount);
        LedgerEntrySpec creditEntry = ledgerEntry(payee, EntrySide.CREDIT, ledgerTransactionSn, amount);
        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(
                LedgerPhaseCode.TRANSFER, List.of(debitEntry, creditEntry));
        LedgerPostingPlanSpec plan = LedgerTransactionSpecFactory.postingPlan(
                LedgerPostingIntentType.TRANSFER, ledgerTransactionSn, List.of(phase));
        return LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn(ledgerTransactionSn)
                .tenantId(TENANT_ID)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .status(LedgerTransactionStatus.POSTED)
                .amount(Money.immutable(amount, CURRENCY))
                .businessScene("WALLET_TRANSFER")
                .businessSn("TRANSFER202605140001")
                .transactionTime(TRANSACTION_TIME)
                .postingPlans(List.of(plan))
                .contextVariables(Map.of())
                .build();
    }

    private static LedgerEntrySpec ledgerEntry(FundsAccountId accountId,
                                               EntrySide entrySide,
                                               String ledgerTransactionSn,
                                               long amount) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                accountId.id(),
                accountId.type(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                ledgerTransactionSn,
                "WALLET_TRANSFER",
                "TRANSFER202605140001",
                amount,
                CURRENCY,
                TRANSACTION_TIME);
    }

    private static FundsSubjectBalanceDTO balance(FundsAccountId accountId,
                                                  LedgerSubjectCode ledgerSubjectCode,
                                                  long amount) {
        return new FundsSubjectBalanceDTO()
                .setId(1L)
                .setTenantId(TENANT_ID)
                .setSubjectRef(accountId)
                .setCurrency(CURRENCY)
                .setInitialized(true)
                .setBalanceBuckets(Map.of(ledgerSubjectCode, LedgerBalanceBucket.builder()
                        .accountCode(ledgerSubjectCode)
                        .balance(Money.immutable(amount, CURRENCY))
                        .periodType(AccountBalancePeriodType.LIFETIME)
                        .periodId(AccountBalancePeriodType.LIFETIME.name())
                        .activeTime(TRANSACTION_TIME.minusDays(1))
                        .build()));
    }

    private static FundsAccountId fundingAccount(String id) {
        return FundsAccountId.immutable(id, FundsSubjectType.FUNDING_ACCOUNT.name());
    }
}
