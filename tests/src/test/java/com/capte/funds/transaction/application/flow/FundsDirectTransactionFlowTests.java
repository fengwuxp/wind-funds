package com.capte.funds.transaction.application.flow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> DIRECT_LEDGER_CONTEXT_KEYS = Set.of(
            "routeLegId", "replayRefLegId", "replayPolicy");

    private static final Set<String> DIRECT_REQUEST_CONTEXT_KEYS = Set.of(
            "accountId",
            "payerAccountId",
            "payeeAccountId",
            "payerId",
            "payeeId",
            "payerLedgerSubjectCode",
            "payeeLedgerSubjectCode",
            "channelCode",
            "externalTransactionId",
            "feeSpec");

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
     * 场景：直接退款请求把敏感账户值放入扩展上下文。
     * 输入：付款方充值 100 并向收款方付款 70 后，退款 contextVariables 含嵌套 IBAN 值。
     * 输出：退款请求被拒绝；付款方、收款方和平台账户余额保持付款后的状态。
     * 预期：退款入口在构造指令前阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：IBAN、完整账户号等敏感值不得通过退款上下文落库。
     */
    @Test
    void testRefundWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("refund_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_REFUND_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "DIRECT_REFUND_SENSITIVE_CONTEXT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refund(new FundsTransactionRefundRequest()
                .setAccountId(payer)
                .setPayerId(payee)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(Money.immutable(30L, CURRENCY))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("REFUND")
                .setBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("refund with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

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
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_PAY", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE");
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
     * 场景：直接充值把完整外部账户号伪装成外部账户引用 ID。
     * 输入：充值资金来源 FundsAccountId.id 为 12 位银行账户号。
     * 输出：请求被拒绝；用户账户、平台现金和预收款余额均不变化。
     * 预期：外部账户引用快照构造期阻断敏感原文，不生成资金交易事实和账务事实。
     * 红线：外部账户引用字段不得保存完整银行账户号、IBAN 或其他敏感原文。
     */
    @Test
    void testTopupWithRawExternalAccountIdShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("123456789012",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(10L, CURRENCY)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT")
                .setDescription("topup with raw external account id"), WindOperator.system()))
                .hasMessageContaining("externalAccountNo must be masked or token reference");

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
        assertNoFundsOrLedgerFactsForBusinessSn("DIRECT_TOPUP_RAW_EXTERNAL_ACCOUNT");
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
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_TOPUP_ONLY");

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_ONLY", firstRouteSnapshot);
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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_ONLY", firstRouteSnapshot);

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
     * 场景：直接充值使用相同业务流水重复通知，第二次请求只更换 traceId。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_TRACE`，首请求 traceId 为 TRACE-1，重试 traceId 为 TRACE-2。
     * 输出：重试返回同一资金交易流水；余额和账务事实保持第一次充值后的状态。
     * 预期：traceId 只用于审计追踪，不参与资金请求摘要的业务一致性判断。
     * 红线：易变审计字段变化不得误判为幂等冲突，也不得重复生成 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentTraceIdShouldReuseOriginalTransaction() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_trace_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_TRACE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("traceId", "TRACE-1")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE")
                .setDescription("idempotent topup with trace"), WindOperator.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_TOPUP_TRACE");

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_trace_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_TRACE_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("traceId", "TRACE-2")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE")
                .setDescription("idempotent topup with trace"), WindOperator.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_TRACE", firstRouteSnapshot);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_TRACE", 3, 4);
    }

    /**
     * 场景：直接充值使用相同业务流水重复通知，第二次请求更换非易变业务上下文字段。
     * 输入：充值 40 使用业务流水 `DIRECT_IDEMPOTENT_TOPUP_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：非易变 contextVariables 字段必须参与资金请求摘要，不能像 traceId 一样被过滤。
     * 红线：同业务流水不同业务上下文不得静默复用原交易，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectTopupSameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId account = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));

        String firstTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperator.system());
        BalanceSnapshot afterFirstTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFirstTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstTopupFacts = ledgerFactSnapshot();
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_TOPUP_CONTEXT");

        String retryTopupSn = directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperator.system());

        assertThat(retryTopupSn).isEqualTo(firstTopupSn);
        BalanceSnapshot afterRetryTopup = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstTopup, afterRetryTopup,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.topup(new FundsTransactionTopupRequest()
                .setAccountId(account)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_context_topup",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT_CHANNEL")
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("TOPUP")
                .setBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT")
                .setDescription("idempotent topup with business context"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(account, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryTopup, afterConflict,
                delta(account, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(account, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstTopupFacts);
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(account), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(account), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstTopupSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_TOPUP_CONTEXT", 3, 4);
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
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_PAY");

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY", firstRouteSnapshot);
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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY", firstRouteSnapshot);

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
     * 场景：直接付款使用相同业务流水重复提交，第二次请求更换非易变业务上下文字段。
     * 输入：充值 100，付款 40 使用业务流水 `DIRECT_IDEMPOTENT_PAY_CONTEXT`，首请求 context 为 RULE-A，重试同 context 后改为 RULE-B。
     * 输出：同 context 重试返回同一资金交易流水；业务上下文变化被摘要冲突拒绝。
     * 预期：付款幂等摘要必须覆盖非易变 contextVariables 字段，不能只覆盖金额和参与主体。
     * 红线：同业务流水不同业务上下文不得静默复用原交易，也不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testDirectPaySameBusinessSnWithDifferentBusinessContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("idem_pay_ctx_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "DIRECT_IDEMPOTENT_PAY_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstPaySn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperator.system());
        BalanceSnapshot afterFirstPay = snapshot(balances(payer, payee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstPayFacts = ledgerFactSnapshot();
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_PAY_CONTEXT");

        String retryPaySn = directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-A")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperator.system());

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_CONTEXT", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(40L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("businessContextVersion", "RULE-B")))
                .setBusinessScene("PAY")
                .setBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT")
                .setDescription("idempotent pay with business context"), WindOperator.system()))
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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_CONTEXT", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertThat(fundsTransactionDetails(firstPaySn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("DIRECT_IDEMPOTENT_PAY_CONTEXT", 2, 2);
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
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_PAY_PARTICIPANT");

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_PAY_PARTICIPANT", firstRouteSnapshot);

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
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_TRANSFER");

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER", firstRouteSnapshot);
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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_TRANSFER", firstRouteSnapshot);

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
        String firstRouteSnapshot = routeSnapshotJson("DIRECT_IDEMPOTENT_REFUND");

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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND", firstRouteSnapshot);
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
        assertDirectRouteSnapshotUnchanged("DIRECT_IDEMPOTENT_REFUND", firstRouteSnapshot);

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

    @Override
    protected void assertSingleFundsAndLedgerFactsForBusinessSn(String businessSn, int expectedDetails,
                                                                int expectedEntries) {
        super.assertSingleFundsAndLedgerFactsForBusinessSn(businessSn, expectedDetails, expectedEntries);
        assertLedgerFactsFollowRouteSnapshot(businessSn);
        assertDirectPostingPlansUseRouteSnapshotLegs(businessSn);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
        assertDirectTransactionKeepsContextMinimal(businessSn);
        assertDirectFactsShareBusinessScene(businessSn);
        assertDirectFactsShareTransactionIdentity(businessSn);
        assertDirectDetailsFollowRouteParticipants(businessSn);
        assertDirectDetailsKeepRequestFactsOutOfContext(businessSn);
        assertDirectEntriesFollowPostingPlans(businessSn);
        assertDirectLedgerTransactionKeepsContextMinimal(businessSn);
        assertDirectLedgerContextsKeepPostingEvidenceOnly(businessSn);
        assertDirectFactsCarryAuditTrail(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    @Override
    protected void assertFailedFundsTransactionWithoutLedgerFacts(String businessSn) {
        super.assertFailedFundsTransactionWithoutLedgerFacts(businessSn);
        assertFailedDirectFactsCarryIdentityAndAudit(businessSn);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
        assertDirectTransactionKeepsContextMinimal(businessSn);
        assertDirectDetailsFollowRouteParticipants(businessSn);
        assertDirectDetailsKeepRequestFactsOutOfContext(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    @Override
    protected void assertNoFundsOrLedgerFactsForBusinessSn(String businessSn) {
        super.assertNoFundsOrLedgerFactsForBusinessSn(businessSn);
        assertDirectBalancesMatchLedgerEntries();
    }

    private void assertDirectRouteSnapshotCarriesMetadata(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("direct route snapshot metadata for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(routeSnapshot.getSnapshotId()).isEqualTo(businessSn + "_ROUTE");
                    assertThat(routeSnapshot.getSnapshotSchemaVersion())
                            .isEqualTo(FundsRouteCodes.CURRENT_ROUTE_VERSION);
                    assertThat(routeSnapshot.getRouteCode()).isEqualTo(expectedDirectRouteCode(transaction));
                    assertThat(routeSnapshot.getRouteVersion()).isEqualTo(FundsRouteCodes.CURRENT_ROUTE_VERSION);
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(routeSnapshot.getResolvedAt()).isNotNull();
                });
    }

    private void assertDirectRouteSnapshotUnchanged(String businessSn, String expectedRouteSnapshot) {
        assertThat(routeSnapshotJson(businessSn))
                .as("direct route snapshot must not be rewritten for idempotent businessSn %s", businessSn)
                .isEqualTo(expectedRouteSnapshot);
        assertDirectRouteSnapshotCarriesMetadata(businessSn);
        assertDirectRouteSnapshotKeepsContextMinimal(businessSn);
    }

    private void assertDirectRouteSnapshotKeepsContextMinimal(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("direct route snapshot context for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getContextVariables())
                        .as("direct route snapshot must not carry request context variables for %s", businessSn)
                        .isEmpty());
    }

    private String routeSnapshotJson(String businessSn) {
        return fundsTransactionsByBusinessSn(businessSn).getFirst().getRouteSnapshot();
    }

    private void assertDirectTransactionKeepsContextMinimal(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        JSONObject transactionContext = contextVariablesOf(transaction.getContextVariables());

        assertThat(transactionContext.keySet())
                .as("direct transaction context must not carry request context for %s", businessSn)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private String expectedDirectRouteCode(FundsTransaction transaction) {
        return switch (transaction.getTransactionType()) {
            case TOPUP -> FundsRouteCodes.TOPUP_STANDARD;
            case TRANSFER -> FundsRouteCodes.INTERNAL_TRANSFER_STANDARD;
            case PAY -> FundsRouteCodes.DIRECT_PAY_STANDARD;
            case REFUND -> FundsRouteCodes.DIRECT_REFUND_STANDARD;
            default -> throw new AssertionError("unsupported direct transaction type: "
                    + transaction.getTransactionType());
        };
    }

    private void assertFailedDirectFactsCarryIdentityAndAudit(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);

        assertThat(transaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(transaction.getGmtCreate()).isNotNull();
        assertThat(transaction.getGmtModified()).isAfterOrEqualTo(transaction.getGmtCreate());
        assertThat(details)
                .as("failed direct details must carry identity and audit fields for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(detail.getTransactionSn()).isEqualTo(transaction.getSn());
                    assertThat(detail.getBusinessScene()).isEqualTo(transaction.getBusinessScene());
                    assertThat(detail.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(detail.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(detail.getAmount()).isEqualTo(transaction.getAmount());
                    assertThat(detail.getCurrency()).isEqualTo(transaction.getCurrency());
                    assertThat(detail.getGmtCreate()).isNotNull();
                    assertThat(detail.getGmtModified()).isAfterOrEqualTo(detail.getGmtCreate());
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("failed direct route snapshot identity must follow transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(routeSnapshot.getBusinessScene()).isEqualTo(transaction.getBusinessScene());
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(transaction.getBusinessSn());
                    assertThat(routeSnapshot.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(routeSnapshot.getResolvedAt()).isNotNull();
                    assertThat(details)
                            .as("failed direct details must share route event type for %s", businessSn)
                            .extracting(FundsTransactionDetail::getEventType)
                            .containsOnly(routeSnapshot.getEventType());
                });
    }

    private void assertDirectPostingPlansUseRouteSnapshotLegs(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<String> postingRouteLegIds = postingPlansOf(ledgerTransaction).stream()
                .map(LedgerPostingPlan::getRouteLegId)
                .toList();

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot for direct transaction %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(routeSnapshot.getLegs())
                            .as("route snapshot legs for direct transaction %s", businessSn)
                            .isNotEmpty();
                    assertThat(postingRouteLegIds)
                            .as("posting routeLegId must come from route snapshot for direct transaction %s",
                                    businessSn)
                            .containsExactlyInAnyOrderElementsOf(routeSnapshot.getLegs().stream()
                                    .map(RouteLegSpec::getLegId)
                                    .toList());
                    postingPlansOf(ledgerTransaction).forEach(plan -> {
                        RouteLegSpec routeLeg = directRouteLegById(routeSnapshot.getLegs(), plan.getRouteLegId());
                        assertThat(plan.getAmount())
                                .as("posting amount must follow route leg for direct transaction %s", businessSn)
                                .isEqualTo(routeLeg.getAmount().getAmount());
                        assertThat(plan.getCurrency())
                                .as("posting currency must follow route leg for direct transaction %s", businessSn)
                                .isEqualTo(routeLeg.getAmount().getCurrency());
                        assertThat(plan.getBalanceEffectType())
                                .as("posting balance effect must follow route leg for direct transaction %s",
                                        businessSn)
                                .isEqualTo(routeLeg.getBalanceEffectType().name());
                        assertThat(plan.getPhaseCode())
                                .as("posting phase must follow route leg for direct transaction %s", businessSn)
                                .isEqualTo(routeLeg.getPhaseCode().name());
                    });
                });
    }

    private RouteLegSpec directRouteLegById(List<RouteLegSpec> routeLegs, String routeLegId) {
        return routeLegs.stream()
                .filter(routeLeg -> routeLeg.getLegId().equals(routeLegId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("route leg not found: " + routeLegId));
    }

    private void assertDirectDetailsFollowRouteParticipants(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        List<FundsTransactionDetail> details = fundsTransactionDetailsByBusinessSn(businessSn);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot participants must explain funds transaction details for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(details.stream()
                        .map(DirectRouteParticipantKey::from)
                        .toList())
                        .containsExactlyInAnyOrderElementsOf(routeSnapshot.getParticipants().stream()
                                .map(DirectRouteParticipantKey::from)
                                .toList()));
    }

    private void assertDirectDetailsKeepRequestFactsOutOfContext(String businessSn) {
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("direct detail contexts for %s", businessSn)
                .isNotEmpty()
                .allSatisfy(detail -> assertThat(contextVariablesOf(detail.getContextVariables()).keySet())
                        .as("direct detail context must not carry request context variables for %s", businessSn)
                        .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS));
    }

    private void assertDirectEntriesFollowPostingPlans(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot for direct transaction %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> postingPlansOf(ledgerTransaction).forEach(plan -> {
                    RouteLegSpec routeLeg = directRouteLegById(routeSnapshot.getLegs(), plan.getRouteLegId());
                    List<LedgerEntry> planEntries = entries.stream()
                            .filter(entry -> plan.getSn().equals(entry.getPostingPlanSn()))
                            .toList();

                    assertThat(planEntries)
                            .as("ledger entries must follow posting plan for direct transaction %s", businessSn)
                            .hasSize(2);
                    assertThat(planEntries.stream()
                            .map(DirectRouteNodeKey::from)
                            .toList())
                            .as("ledger entries must follow route leg nodes and sides for direct transaction %s",
                                    businessSn)
                            .containsExactlyInAnyOrder(
                                    DirectRouteNodeKey.from(routeLeg.getSourceNode(), EntrySide.DEBIT),
                                    DirectRouteNodeKey.from(routeLeg.getTargetNode(), EntrySide.CREDIT));
                    assertThat(planEntries).allSatisfy(entry -> {
                        assertThat(entry.getIntent()).isEqualTo(plan.getIntent());
                        assertThat(entry.getPostingScope()).isEqualTo(plan.getPostingScope());
                        assertThat(entry.getBalanceEffectType()).isEqualTo(plan.getBalanceEffectType());
                        assertThat(entry.getPhaseCode()).isEqualTo(plan.getPhaseCode());
                        assertThat(entry.getAmount()).isEqualTo(plan.getAmount());
                        assertThat(entry.getCurrency()).isEqualTo(plan.getCurrency());
                        assertThat(entry.getOriginalAmount()).isEqualTo(routeLeg.getOriginalAmount().getAmount());
                        assertThat(entry.getOriginalCurrency()).isEqualTo(routeLeg.getOriginalAmount().getCurrency());
                        assertThat(entry.getExchangeRate()).isEqualByComparingTo(routeLeg.getExchangeRate());
                    });
                }));
    }

    private void assertDirectLedgerContextsKeepPostingEvidenceOnly(String businessSn) {
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        List<LedgerEntry> entries = entriesOf(ledgerTransaction);

        postingPlansOf(ledgerTransaction).forEach(plan -> {
            JSONObject planContext = contextVariablesOf(plan.getContextVariables());
            assertThat(planContext)
                    .as("posting plan context must retain route evidence for direct transaction %s", businessSn)
                    .containsEntry("routeLegId", plan.getRouteLegId())
                    .containsKey("replayPolicy");
            assertThat(planContext.keySet())
                    .as("posting plan context must not carry request context for direct transaction %s", businessSn)
                    .isSubsetOf(DIRECT_LEDGER_CONTEXT_KEYS)
                    .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);

            List<LedgerEntry> planEntries = entries.stream()
                    .filter(entry -> plan.getSn().equals(entry.getPostingPlanSn()))
                    .toList();
            assertThat(planEntries)
                    .as("posting entries must exist for direct transaction %s", businessSn)
                    .isNotEmpty()
                    .allSatisfy(entry -> assertLedgerEntryContextKeepsPostingEvidenceOnly(
                            businessSn, plan, entry));
        });
    }

    private void assertDirectLedgerTransactionKeepsContextMinimal(String businessSn) {
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);
        JSONObject transactionContext = contextVariablesOf(ledgerTransaction.getContextVariables());

        assertThat(transactionContext.keySet())
                .as("direct ledger transaction context must not carry request context for %s", businessSn)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private void assertLedgerEntryContextKeepsPostingEvidenceOnly(String businessSn,
                                                                  LedgerPostingPlan plan,
                                                                  LedgerEntry entry) {
        JSONObject entryContext = contextVariablesOf(entry.getContextVariables());
        assertThat(entryContext)
                .as("ledger entry context must retain route evidence for direct transaction %s", businessSn)
                .containsEntry("routeLegId", plan.getRouteLegId())
                .containsKey("replayPolicy");
        assertThat(entryContext.keySet())
                .as("ledger entry context must not carry request context for direct transaction %s", businessSn)
                .isSubsetOf(DIRECT_LEDGER_CONTEXT_KEYS)
                .doesNotContainAnyElementsOf(DIRECT_REQUEST_CONTEXT_KEYS);
    }

    private JSONObject contextVariablesOf(String contextVariables) {
        if (contextVariables == null || contextVariables.isBlank()) {
            return new JSONObject();
        }
        return JSON.parseObject(contextVariables);
    }

    private void assertDirectFactsCarryAuditTrail(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(transaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(transaction.getGmtCreate()).isNotNull();
        assertThat(transaction.getGmtModified()).isAfterOrEqualTo(transaction.getGmtCreate());
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction details must carry audit fields for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(detail.getGmtCreate()).isNotNull();
                    assertThat(detail.getGmtModified()).isAfterOrEqualTo(detail.getGmtCreate());
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        assertThat(ledgerTransaction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledgerTransaction.getGmtCreate()).isNotNull();
        assertThat(ledgerTransaction.getGmtModified()).isAfterOrEqualTo(ledgerTransaction.getGmtCreate());
        assertThat(ledgerTransaction.getTransactionTime()).isNotNull();
        assertThat(ledgerTransaction.getSha256()).isNotBlank();
        assertThat(postingPlansOf(ledgerTransaction))
                .as("posting plans must carry audit fields for direct transaction %s", businessSn)
                .allSatisfy(plan -> {
                    assertThat(plan.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(plan.getGmtCreate()).isNotNull();
                    assertThat(plan.getGmtModified()).isAfterOrEqualTo(plan.getGmtCreate());
                    assertThat(plan.getSha256()).isNotBlank();
                });
        assertThat(entriesOf(ledgerTransaction))
                .as("ledger entries must carry audit fields for direct transaction %s", businessSn)
                .allSatisfy(entry -> {
                    assertThat(entry.getTenantId()).isEqualTo(TENANT_ID);
                    assertThat(entry.getGmtCreate()).isNotNull();
                    assertThat(entry.getGmtModified()).isAfterOrEqualTo(entry.getGmtCreate());
                    assertThat(entry.getTransactionTime()).isEqualTo(ledgerTransaction.getTransactionTime());
                    assertThat(entry.getSha256()).isNotBlank();
                    assertThat(entry.getSettlementStatus()).isNotNull();
                    assertThat(entry.getReconcileStatus()).isNotNull();
                });
    }

    private void assertDirectBalancesMatchLedgerEntries() {
        Map<DirectBalanceKey, Long> deltas = new LinkedHashMap<>();
        entries().forEach(entry -> deltas.merge(DirectBalanceKey.from(entry), signedEntryAmount(entry), Long::sum));

        deltas.forEach((key, amountDelta) -> assertBucket(balance(key.accountId()), key.ledgerSubjectCode(),
                initialBalance(key) + amountDelta, key.currency()));
    }

    private long signedEntryAmount(LedgerEntry entry) {
        return entry.getEntrySide() == EntrySide.CREDIT ? entry.getAmount() : -entry.getAmount();
    }

    private long initialBalance(DirectBalanceKey key) {
        if (cashMappingAccount().id().equals(key.subjectId()) && key.ledgerSubjectCode() == LedgerSubjectCode.CASH) {
            return 10_000L;
        }
        return 0L;
    }

    private void assertDirectFactsShareBusinessScene(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(ledgerTransaction.getBusinessScene())
                .as("ledger transaction businessScene must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getBusinessScene());
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction detail businessScene must follow funds transaction for %s", businessSn)
                .extracting(FundsTransactionDetail::getBusinessScene)
                .containsOnly(transaction.getBusinessScene());
        assertThat(entriesOf(ledgerTransaction))
                .as("ledger entry businessScene must follow funds transaction for %s", businessSn)
                .extracting(LedgerEntry::getBusinessScene)
                .containsOnly(transaction.getBusinessScene());
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot businessScene must follow funds transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getBusinessScene())
                        .isEqualTo(transaction.getBusinessScene()));
    }

    private void assertDirectFactsShareTransactionIdentity(String businessSn) {
        FundsTransaction transaction = fundsTransactionsByBusinessSn(businessSn).getFirst();
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn(businessSn);

        assertThat(ledgerTransaction.getFundsTransactionSn())
                .as("ledger transaction must point to funds transaction for %s", businessSn)
                .isEqualTo(transaction.getSn());
        assertThat(ledgerTransaction.getTransactionType())
                .as("ledger transaction type must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getTransactionType().name());
        assertThat(ledgerTransaction.getAmount())
                .as("ledger transaction amount must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getAmount());
        assertThat(ledgerTransaction.getCurrency())
                .as("ledger transaction currency must follow funds transaction for %s", businessSn)
                .isEqualTo(transaction.getCurrency());
        assertThat(ledgerTransaction.getOriginalAmount())
                .as("direct ledger transaction original amount must equal amount for %s", businessSn)
                .isEqualTo(transaction.getAmount());
        assertThat(ledgerTransaction.getOriginalCurrency())
                .as("direct ledger transaction original currency must equal currency for %s", businessSn)
                .isEqualTo(transaction.getCurrency());
        assertThat(ledgerTransaction.getExchangeRate())
                .as("direct ledger transaction exchange rate must be one for %s", businessSn)
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(fundsTransactionDetailsByBusinessSn(businessSn))
                .as("funds transaction details must share transaction identity for %s", businessSn)
                .allSatisfy(detail -> {
                    assertThat(detail.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(detail.getEventType().name()).isEqualTo(ledgerTransaction.getEventType());
                    assertThat(detail.getAmount()).isEqualTo(transaction.getAmount());
                    assertThat(detail.getCurrency()).isEqualTo(transaction.getCurrency());
                });
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn()))
                .as("route snapshot identity must follow funds transaction for %s", businessSn)
                .hasValueSatisfying(routeSnapshot -> {
                    assertThat(routeSnapshot.getTransactionType()).isEqualTo(transaction.getTransactionType());
                    assertThat(routeSnapshot.getEventType().name()).isEqualTo(ledgerTransaction.getEventType());
                });
    }

    private record DirectRouteParticipantKey(String subjectId,
                                             String subjectType,
                                             RouteParticipantRole participantRole,
                                             Long amount,
                                             CurrencyIsoCode currency) {

        private static DirectRouteParticipantKey from(RouteParticipantSpec participant) {
            Money amount = participant.getAmount();
            return new DirectRouteParticipantKey(participant.getSubjectRef().getSubjectId(),
                    participant.getSubjectRef().getSubjectType().name(), participant.getParticipantRole(),
                    amount == null ? null : amount.getAmount(), amount == null ? null : amount.getCurrency());
        }

        private static DirectRouteParticipantKey from(FundsTransactionDetail detail) {
            return new DirectRouteParticipantKey(detail.getSubjectId(), detail.getSubjectType(),
                    detail.getParticipantRole(), detail.getAmount(), detail.getCurrency());
        }
    }

    private record DirectRouteNodeKey(String subjectId,
                                      String subjectType,
                                      LedgerSubjectCode ledgerSubjectCode,
                                      EntrySide entrySide) {

        private static DirectRouteNodeKey from(RouteNodeSpec node, EntrySide entrySide) {
            return new DirectRouteNodeKey(node.getSubjectRef().getSubjectId(),
                    node.getSubjectRef().getSubjectType().name(), node.getLedgerSubjectCode(), entrySide);
        }

        private static DirectRouteNodeKey from(LedgerEntry entry) {
            return new DirectRouteNodeKey(entry.getSubjectId(), entry.getSubjectType(),
                    entry.getLedgerSubjectCode(), entry.getEntrySide());
        }
    }

    private record DirectBalanceKey(String subjectId,
                                    String subjectType,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    CurrencyIsoCode currency) {

        private static DirectBalanceKey from(LedgerEntry entry) {
            return new DirectBalanceKey(entry.getSubjectId(), entry.getSubjectType(), entry.getLedgerSubjectCode(),
                    entry.getCurrency());
        }

        private FundsAccountId accountId() {
            return FundsAccountId.immutable(subjectId, subjectType);
        }
    }
}
