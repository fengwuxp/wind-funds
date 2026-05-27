package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 直接交易业务流测试。
 */
class FundsDirectTransactionFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后向普通收款方付款，随后收款方发起部分退款。
     * 输入：充值 100、付款 70、部分退款 30。
     * 输出：付款方 AVAILABLE、收款方 SETTLEMENT、平台 CASH/PREPAYMENT 余额快照。
     * 预期：充值、付款、退款均生成可追溯账务事实，付款进入请求指定收款桶，部分退款只回补退款金额。
     * 红线：普通支付不得默认套用商户清算路径；普通退款不得影响平台现金和预收款口径。
     */
    @Test
    void testTopupPayThenPartialRefundShouldPostLedgerFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("ordinary_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));

        topup(payer, 100L, "DIRECT_PAY_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_PAY_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "DIRECT_PAY_REFUND_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name());

        LedgerTransaction payTransaction = ledgerTransactionByBusinessSn("DIRECT_PAY_REFUND_PAY");
        assertThat(entriesOf(payTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(payTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("DIRECT_PAY_REFUND_REFUND");
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_PAY", 2, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_REFUND_REFUND", 2, 2);
    }

    /**
     * 场景：直接退款出资方余额不足。
     * 输入：付款方充值 100、向收款方付款 70，随后收款方尝试退款 80。
     * 输出：退款失败；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：退款出资方余额不足时记录 FAILED 资金交易事实，不生成账务事实。
     * 红线：退款失败不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testRefundWithInsufficientPayerBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_low_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_INSUFFICIENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_INSUFFICIENT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 80L,
                "DIRECT_REFUND_INSUFFICIENT_REFUND"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterRejectedRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRejectedRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_INSUFFICIENT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_INSUFFICIENT_PAY", 2, 2);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_REFUND_INSUFFICIENT_REFUND");
    }

    /**
     * 场景：同一资金账户向自己发起系统内转账。
     * 输入：充值 100 后，付款方和收款方均为同一账户，转账 10。
     * 输出：请求被拒绝；余额、资金交易事实和账务事实保持充值后的状态。
     * 预期：系统内转账必须是跨主体价值转移，同主体转账不能生成 route、posting 或 ledger entry。
     * 红线：不能用一借一贷自循环掩盖无业务意义的资金事实。
     */
    @Test
    void testSameAccountTransferShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        topup(account, 100L, "DIRECT_SAME_ACCOUNT_TRANSFER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(account, account, 10L, "DIRECT_SAME_ACCOUNT_TRANSFER"))
                .hasMessageContaining("付款账号和收款账户不能一致");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_TRANSFER_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_SAME_ACCOUNT_TRANSFER");
    }

    /**
     * 场景：USD 资金账户发起 CNY 系统内转账。
     * 输入：付款方充值 50 USD，随后向收款方转账 10 CNY。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：系统内转账只接受付款方账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：系统内转账不得隐式换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTransferWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("transfer_currency_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_TRANSFER_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_CURRENCY")
                .setDescription("transfer with different currency"), WindOperator.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_CURRENCY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_CURRENCY");
    }

    /**
     * 场景：USD 资金账户收到 CNY 充值请求。
     * 输入：用户资金账户为 USD，外部通道充值请求金额为 10 CNY。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：充值只接受目标账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：充值不得静默换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTopupWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_topup_currency",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_CURRENCY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_CURRENCY")
                .setDescription("topup with different currency"), WindOperator.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_CURRENCY");
    }

    /**
     * 场景：直接充值和系统内转账请求把敏感账户值放入扩展上下文。
     * 输入：充值 contextVariables 含嵌套 IBAN 值；有效充值后，转账 contextVariables 含嵌套 IBAN 值。
     * 输出：两次请求均被拒绝；账户和平台余额保持最近一次成功事实后的状态。
     * 预期：直接交易各入口在构造指令前统一阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：IBAN、完整账户号等敏感值不得通过普通交易上下文落库。
     */
    @Test
    void testTopupAndTransferWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("ctx_transfer_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(payer)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_sensitive_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(50L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("topup with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedTopup = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        topup(payer, 50L, "DIRECT_TRANSFER_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRejectedTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("transfer with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_SENSITIVE_CONTEXT_IBAN_VALUE");
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TRANSFER_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：付款方可用余额不足时发起系统内转账。
     * 输入：付款方未充值，向收款方转账 10。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额均不变化。
     * 预期：转账必须受付款方 AVAILABLE 余额约束，余额不足时记录 FAILED 资金交易事实。
     * 红线：转账余额不足不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testTransferWithInsufficientBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("transfer_low_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> transfer(payer, payee, 10L, "DIRECT_TRANSFER_INSUFFICIENT"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterRejectedTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_TRANSFER_INSUFFICIENT");
    }

    /**
     * 场景：付款方可用余额不足时发起直接付款。
     * 输入：付款方未充值，向普通收款方付款 10。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额均不变化。
     * 预期：余额不足失败必须记录 FAILED 资金交易事实，不生成 posted ledger transaction。
     * 红线：余额不足不能留下半截 posting、ledger entry 或余额投影。
     */
    @Test
    void testPayWithInsufficientBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("insufficient_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_INSUFFICIENT_PAY"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot after = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_INSUFFICIENT_PAY");
    }

    /**
     * 场景：USD 资金账户发起 CNY 直接付款。
     * 输入：付款方充值 50 USD，随后向普通收款方付款 10 CNY。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：直接交易只接受账户同币种金额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：直接付款不得隐式换汇，不得留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testPayWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("different_currency_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CurrencyIsoCode.CNY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_CURRENCY_PAY")
                .setDescription("pay with different currency"), WindOperator.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_CURRENCY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_CURRENCY_PAY");
    }

    /**
     * 场景：直接付款请求把通道 token secret 放入扩展上下文。
     * 输入：付款方充值 50 后，付款请求的 contextVariables 含嵌套 secretKey 字段。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：请求扩展上下文不得进入资金交易事实、交易明细、route snapshot 或账务事实。
     * 红线：完整卡号、CVV、密钥和 token secret 不得通过普通交易上下文落库。
     */
    @Test
    void testPayWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("sensitive_context_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("secretKey", "secret-value"))))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT")
                .setDescription("pay with sensitive context"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("pay with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT");
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：直接付款缺少收款主体。
     * 输入：付款方充值 50 后，提交 payeeId 为空的直接付款。
     * 输出：请求被拒绝；付款方和平台账户余额保持充值后的状态。
     * 预期：直接付款必须先解析到最终可记账收款主体，缺主体不能进入 route 和 ledger。
     * 红线：缺主体不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testPayWithoutPayeeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_MISSING_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_MISSING_PAYEE")
                .setDescription("pay without payee"), WindOperator.system()))
                .hasMessageContaining("直接付款收款主体不能为空");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE");
    }

    /**
     * 场景：直接付款缺少收款账目。
     * 输入：付款方充值 50 后，提交 payeeLedgerCode 为空的直接付款。
     * 输出：请求被拒绝；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：直接付款必须明确最终入账的收款余额桶，缺账目不能进入 route 和 ledger。
     * 红线：缺收款账目不能生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayWithoutPayeeLedgerCodeShouldRejectAndLeaveNoLedgerSideEffects()
            throws ReflectiveOperationException {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("miss_ledger_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_MISSING_PAYEE_LEDGER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        FundsTransactionPayRequest request = new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_MISSING_PAYEE_LEDGER")
                .setDescription("pay without payee ledger code");
        var payeeLedgerCode = FundsTransactionPayRequest.class.getDeclaredField("payeeLedgerCode");
        payeeLedgerCode.setAccessible(true);
        payeeLedgerCode.set(request, null);

        assertThatThrownBy(() -> directTransactionService.pay(request, WindOperator.system()))
                .hasMessageContaining("直接付款收款账目不能为空");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE_LEDGER_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_PAYEE_LEDGER");
    }

    /**
     * 场景：直接付款收款主体存在，但目标收款账目未建账。
     * 输入：付款方充值 50，收款资金账户存在但没有 SETTLEMENT 账本。
     * 输出：付款失败；付款方、收款方和平台账户余额保持充值后的状态。
     * 预期：账务计划缺目标账本时标记 FAILED 资金交易事实，不自动建账、不展示交易成功。
     * 红线：缺账本不能留下 posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testPayWithoutPayeeLedgerShouldRejectAndRollbackTransactionFacts() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("no_payee_ledger");
        ensureFundingAccount(payee);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_MISSING_LEDGER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 10L,
                "DIRECT_PAY_MISSING_LEDGER"))
                .hasMessageContaining("账本不存在或不唯一");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertThat(balance(payee).getBalanceBuckets()).doesNotContainKey(LedgerSubjectCode.SETTLEMENT);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_MISSING_LEDGER_TOPUP", 3, 4);
        assertFailedFundsTransactionWithoutLedgerFacts("DIRECT_PAY_MISSING_LEDGER");
    }

    /**
     * 场景：直接付款把外部账户作为收款主体。
     * 输入：付款方充值 50 后，提交外部银行账户作为 payeeId。
     * 输出：请求被拒绝；付款方和平台账户余额保持充值后的状态。
     * 预期：外部账户只能作为出入金引用或快照，不能成为 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId externalPayee = FundsAccountId.immutable("external_bank_payee",
                DefaultFundsAccountType.EXTERNAL_BANK);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 50L, "DIRECT_PAY_EXTERNAL_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(externalPayee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE")
                .setDescription("pay to external payee"), WindOperator.system()))
                .hasMessageContaining("直接付款收款主体不能是外部账户");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payer, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYEE");
    }

    /**
     * 场景：直接付款把外部账户作为付款主体。
     * 输入：外部银行账户作为 accountId，向普通收款方付款 10。
     * 输出：请求被拒绝；收款方和平台账户余额均不变化。
     * 预期：外部账户只能作为出入金引用或快照，不能成为直接付款的 ledger subject。
     * 红线：外部账户不得生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testPayFromExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId externalPayer = FundsAccountId.immutable("external_bank_payer",
                DefaultFundsAccountType.EXTERNAL_BANK);
        FundsAccountId payee = fundingAccount("external_payer_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(externalPayer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_PAY_EXTERNAL_PAYER")
                .setDescription("pay from external payer"), WindOperator.system()))
                .hasMessageContaining("直接付款账户不能是外部账户");

        BalanceSnapshot afterRejectedPay = snapshot(balances(payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedPay,
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_PAY_EXTERNAL_PAYER");
    }

    /**
     * 场景：直接充值使用相同业务流水重复通知，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_ONLY`，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次充值后的状态。
     * 预期：充值幂等必须由 `tenantId + businessScene + businessSn + requestHash` 共同保护。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperator.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperator.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_idempotent_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_ONLY_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(41L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY")
                .setDescription("idempotent topup"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTopup, afterConflict,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_ONLY", 3, 4);
    }

    /**
     * 场景：直接付款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100，付款 40 使用业务流水 `DIRECT_IDEMPOTENT_PAY`，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次付款后的状态。
     * 预期：`tenantId + businessScene + businessSn + requestHash` 共同保护直接交易幂等。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("direct_idempotent_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY");
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();

        String retryPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY");

        assertThat(retryPaySn).isEqualTo(firstPaySn);
        BalanceSnapshot afterRetryPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstPay, afterRetryPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);
        assertThatThrownBy(() -> pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 41L,
                "DIRECT_IDEMPOTENT_PAY"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryPay, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY", 2, 2);
    }

    /**
     * 场景：直接付款使用相同业务流水重复提交，但第二次把付款方和收款方都换成新的主体。
     * 输入：两个付款方各充值 100，第一次付款方 A 向收款方 A 支付 40，随后同业务流水改为付款方 B 向收款方 B 支付 40。
     * 输出：第二次请求被幂等摘要拒绝；付款方 B、收款方 B 和既有交易事实均不变化。
     * 预期：同业务流水的幂等保护必须覆盖参与主体，不只覆盖金额。
     * 红线：同业务流水不同参与方不得新增 detail、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentParticipantsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId anotherPayer = fundingAccount("idem_payer2");
        FundsAccountId payee = fundingAccount("idem_payee1");
        FundsAccountId anotherPayee = fundingAccount("idem_payee2");
        ensureLedger(anotherPayer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherPayer, LedgerSubjectCode.FROZEN);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(anotherPayee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeFirstTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_PARTICIPANT_TOPUP");
        BalanceSnapshot afterFirstTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFirstTopup, afterFirstTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        topup(anotherPayer, 100L, "DIRECT_IDEMPOTENT_PARTICIPANT_ANOTHER_TOPUP");
        BalanceSnapshot afterSecondTopup = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterSecondTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY_PARTICIPANT");
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterSecondTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> pay(anotherPayer, anotherPayee, LedgerSubjectCode.SETTLEMENT, 40L,
                "DIRECT_IDEMPOTENT_PAY_PARTICIPANT"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, anotherPayer, payee, anotherPayee,
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstPay, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherPayer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(anotherPayee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstPayFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(anotherPayer), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(anotherPayee), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PARTICIPANT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PARTICIPANT_ANOTHER_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_PARTICIPANT", 2, 2);
    }

    /**
     * 场景：系统内转账使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、转账 40，随后同流水同金额重试，再同流水改金额为 41。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次转账后的状态。
     * 预期：系统内转账幂等必须由业务键和请求摘要共同保护。
     * 红线：同业务流水不同请求不得新增交易、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTransferSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_transfer_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_TRANSFER_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperator.system());
        BalanceSnapshot afterFirstTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTransferFacts = ledgerFactSnapshot();

        String retryTransferSn = directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperator.system());

        assertThat(retryTransferSn).isEqualTo(firstTransferSn);
        BalanceSnapshot afterRetryTransfer = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTransfer, afterRetryTransfer,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);
        assertThatThrownBy(() -> directTransactionService.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(41L, CURRENCY)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("DIRECT_IDEMPOTENT_TRANSFER")
                .setDescription("idempotent transfer"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTransfer, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTransferFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TRANSFER.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER_TOPUP", 3, 4);
        assertThat(fundsTransactionDetails(firstTransferSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TRANSFER", 2, 2);
    }

    /**
     * 场景：直接退款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、付款 70、退款 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次退款后的状态。
     * 预期：退款幂等必须同时保护退款到账账户、退款出资账户、出资账目、金额和 route replay 摘要。
     * 红线：同业务流水不同退款请求不得重复返还、超额扣减或污染账务事实。
     */
    @Test
    void testDirectRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_refund_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_IDEMPOTENT_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(Money.immutable(30L, CURRENCY))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperator.system());
        BalanceSnapshot afterFirstRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFirstRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();

        String retryRefundSn = directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(Money.immutable(30L, CURRENCY))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperator.system());

        assertThat(retryRefundSn).isEqualTo(firstRefundSn);
        BalanceSnapshot afterRetryRefund = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstRefund, afterRetryRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(Money.immutable(31L, CURRENCY))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_IDEMPOTENT_REFUND")
                .setDescription("idempotent refund"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryRefund, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND_PAY", 2, 2);
        assertThat(fundsTransactionDetails(firstRefundSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_REFUND", 2, 2);
    }
}
