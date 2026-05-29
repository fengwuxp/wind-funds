package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 授权交易业务流测试。
 */
class FundsAuthorizationTransactionFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：风控或额度判断拒绝授权。
     * 输入：账户余额 100，授权请求 60，授权结果 approved=false。
     * 输出：记录授权拒绝交易事实和拒绝明细，余额不变，无账务路径。
     * 预期：拒绝不是授权创建，不生成 route leg、posting plan 或 LedgerEntry。
     * 红线：授权拒绝不得被当作完成后拒付，也不得写入 chargeback/declined 累计金额。
     */
    @Test
    void testAuthorizationDeclinedShouldRecordRejectedFactWithoutLedgerPosting() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_DECLINE_TOPUP");
        BalanceSnapshot beforeDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeDeclineFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(beforeTopup, beforeDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, false, "AUTH_DECLINE_AUTHORIZE");

        BalanceSnapshot afterDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeDecline, afterDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeDeclineFacts);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(authorizationSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getLegs()).isEmpty());

        assertThat(fundsTransactionDetails(authorizationSn))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(detail.getContextVariables()).contains("\"approved\":false");
                });

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertNoLedgerFactsForFundsTransaction(authorizationSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DECLINE_TOPUP", 3, 4);
        assertThat(fundsTransactionsByBusinessSn("AUTH_DECLINE_AUTHORIZE"))
                .as("rejected funds transactions for businessSn AUTH_DECLINE_AUTHORIZE")
                .singleElement()
                .satisfies(rejectedTransaction -> {
                    assertThat(rejectedTransaction.getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
                    assertThat(rejectedTransaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
                    assertThat(rejectedTransaction.getAuthorizedAmount()).isZero();
                    assertThat(rejectedTransaction.getReversedAmount()).isZero();
                    assertThat(rejectedTransaction.getSettledAmount()).isZero();
                    assertThat(rejectedTransaction.getRefundedAmount()).isZero();
                    assertThat(rejectedTransaction.getDeclinedAmount()).isZero();
                    assertNoLedgerFactsForFundsTransaction(rejectedTransaction.getSn());
                });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_DECLINE_AUTHORIZE"))
                .as("rejected funds transaction details for businessSn AUTH_DECLINE_AUTHORIZE")
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.HOLD);
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                });
    }

    /**
     * 场景：授权请求把卡组织 CVV 原文字段放入扩展上下文。
     * 输入：账户充值 100 后，授权请求 contextVariables 含嵌套 cardSecurityCode 字段。
     * 输出：授权请求被拒绝，AVAILABLE/AUTHORIZATION 和平台账户余额保持充值后状态。
     * 预期：请求扩展上下文不得进入资金交易事实、route snapshot 或账务事实。
     * 红线：完整卡号、CVV、密钥和 token secret 不得通过普通授权上下文落库。
     */
    @Test
    void testAuthorizeWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("cardSecurityCode", "123"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_AUTHORIZE")
                        .setDescription("authorization with sensitive context"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("networkReference", "4242424242424242"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_PAN_VALUE")
                        .setDescription("authorization with sensitive PAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> authorizationTransactionService.authorize(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(user)
                        .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                        .setApproved(true)
                        .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                                Map.of("networkReference", "GB82WEST12345698765432"))))
                        .setBusinessScene("AUTHORIZATION")
                        .setBusinessSn("AUTH_SENSITIVE_CONTEXT_IBAN_VALUE")
                        .setDescription("authorization with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterRejectedAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_AUTHORIZE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_PAN_VALUE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：用户充值后发起资金账户授权，授权批准后全额撤销。
     * 输入：充值 100、授权批准 60、全额撤销 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION 余额快照和账务事实。
     * 预期：授权批准只占用可用余额，撤销只释放授权占用。
     * 红线：授权撤销不得进入 SETTLEMENT，不得表达消费、扣划或完成后退款。
     */
    @Test
    void testFundingAuthorizationApproveThenFullReversalShouldReleaseAuthorizationBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_REVERSAL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_REVERSAL_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REVERSAL_CANCEL");
        BalanceSnapshot afterReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isEqualTo(60L);
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());

        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REVERSAL_CANCEL");
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REVERSAL.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_CANCEL", 0, 1, 1, 2);
    }

    /**
     * 场景：用户充值后授权批准，先部分撤销，再完成剩余授权金额。
     * 输入：充值 100、授权批准 80、部分撤销 30、剩余完成 50。
     * 输出：每一步 AVAILABLE/AUTHORIZATION/SETTLEMENT 余额变化和账务事实。
     * 预期：部分撤销释放授权占用，剩余完成只消费剩余授权占用。
     * 红线：完成剩余授权不得重新从 AVAILABLE 扣款，部分撤销后的累计处理金额不得超过原授权。
     */
    @Test
    void testFundingAuthorizationPartialReversalThenSettleRemainingShouldCloseAuthorization() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_PARTIAL_REVERSAL_SETTLE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_PARTIAL_REVERSAL_SETTLE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 30L, authorizationSn, "AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL");
        BalanceSnapshot afterReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 50L, authorizationSn, "AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterReversal, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getSettledAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name(),
                        FundsTransactionEventType.SETTLE.name());

        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL");
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REVERSAL.name());

        LedgerTransaction settleTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE");
        assertThat(entriesOf(settleTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(settleTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL", 0, 1, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：用户充值后授权批准，部分撤销后再次发起超过剩余授权的撤销。
     * 输入：充值 100、授权批准 80、先撤销 30、再撤销 60。
     * 输出：第二次撤销失败，余额、交易累计和账务事实保持第一次撤销后的状态。
     * 预期：剩余授权只有 50 时，不允许再释放 60。
     * 红线：超额撤销不得透支 AUTHORIZATION，不得把失败请求记录成成功账务事实。
     */
    @Test
    void testAuthorizationReversalExceedingRemainingShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_REVERSAL_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_REVERSAL_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        reverseAuthorization(user, 30L, authorizationSn, "AUTH_REVERSAL_EXCEED_FIRST_CANCEL");
        BalanceSnapshot afterFirstReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstReversalFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> reverseAuthorization(user, 60L, authorizationSn,
                "AUTH_REVERSAL_EXCEED_SECOND_CANCEL"))
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterFirstReversal, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_FIRST_CANCEL", 0, 1, 1, 2);
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REVERSAL_EXCEED_SECOND_CANCEL");
    }

    /**
     * 场景：用户充值后发起资金账户授权，授权批准后全额完成。
     * 输入：充值 100、授权批准 60、全额完成 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION、平台 CASH/SETTLEMENT 余额快照和账务事实。
     * 预期：授权批准只占用可用余额，完成只消费授权占用并进入平台结算桶。
     * 红线：授权批准不是消费；普通完成不得触碰 LIMIT，也不得重新从 AVAILABLE 扣款。
     */
    @Test
    void testFundingAuthorizationApproveThenFullSettleShouldConsumeAuthorizationBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_SETTLE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_SETTLE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 60L, authorizationSn, "AUTH_FULL_SETTLE_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_SETTLE_AUTHORIZE");
        assertThat(entriesOf(authorizationTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.AUTHORIZATION);
        assertThat(postingPlansOf(authorizationTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.AUTHORIZATION.name());

        LedgerTransaction settleTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_SETTLE_CAPTURE");
        List<LedgerPostingPlan> settlePostingPlans = postingPlansOf(settleTransaction);
        assertThat(entriesOf(settleTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.SETTLEMENT);
        assertThat(settlePostingPlans)
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getSn()).hasSizeLessThanOrEqualTo(64);
                    assertThat(plan.getRouteLegId()).isEqualTo("CONSUME_AUTHORIZATION_1");
                });
        assertThat(settlePostingPlans.stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：用户充值后发起授权，授权全额完成后再全额退款。
     * 输入：充值 100、授权批准 60、全额完成 60、全额退款 60。
     * 输出：用户 AVAILABLE/AUTHORIZATION、平台 SETTLEMENT 余额逐步变化和账务事实。
     * 预期：完成后退款沿原完成路径回退，回补用户 AVAILABLE 并扣减平台 SETTLEMENT。
     * 红线：完成后退款不得重新释放 AUTHORIZATION，不得按当前绑定重新选路。
     */
    @Test
    void testFundingAuthorizationFullSettleThenFullRefundShouldRestoreAvailableBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FULL_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_FULL_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REFUND_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        refundSettledAuthorization(user, 60L, authorizationSn, "AUTH_FULL_REFUND_RETURN");
        BalanceSnapshot afterRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getSettledAmount()).isEqualTo(60L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.AUTH_REFUND.name());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REFUND_RETURN");
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：用户授权 80 后只完成 50，平台结算户另有充足余额时尝试退款 60。
     * 输入：A 充值并授权 80、完成 50；B 另完成 100 使平台 SETTLEMENT 余额充足；A 退款 60。
     * 输出：A 退款请求失败，A/B/平台余额、交易累计和账务事实保持失败前状态。
     * 预期：授权完成后退款以本交易已完成可回退金额为上限，不以授权金额或平台总余额为上限。
     * 红线：失败退款不得借用其他交易沉淀在 SETTLEMENT 的余额，不得写入 AUTH_REFUND 账务事实。
     */
    @Test
    void testAuthorizationRefundExceedingSettledAmountShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId reserveUser = fundingAccount("settlement_reserve_user");
        ensureLedger(reserveUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(reserveUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_REFUND_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_REFUND_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 50L, authorizationSn, "AUTH_REFUND_EXCEED_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        topup(reserveUser, 100L, "AUTH_REFUND_EXCEED_RESERVE_TOPUP");
        BalanceSnapshot afterReserveTopup = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterReserveTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String reserveAuthorizationSn = authorize(reserveUser, 100L, true,
                "AUTH_REFUND_EXCEED_RESERVE_AUTHORIZE");
        BalanceSnapshot afterReserveAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveTopup, afterReserveAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(reserveUser, 100L, reserveAuthorizationSn, "AUTH_REFUND_EXCEED_RESERVE_CAPTURE");

        BalanceSnapshot beforeFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveAuthorize, beforeFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, -100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 100L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(reserveUser), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 150L, CURRENCY);

        assertThatThrownBy(() -> refundSettledAuthorization(user, 60L, authorizationSn,
                "AUTH_REFUND_EXCEED_RETURN"))
                .hasMessageContaining("资金交易已结算可回退金额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getSettledAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();

        assertPostedTransactions(6);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_CAPTURE", 0, 2, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RESERVE_CAPTURE", 0, 2, 1, 2);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_REFUND_EXCEED_RETURN");
    }

    /**
     * 场景：授权批准使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 60，随后同流水同金额重试，再同流水改金额为 61。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次授权后的状态。
     * 预期：授权批准幂等必须由业务键和请求摘要共同保护。
     * 红线：同业务流水不同授权批准请求不得重复冻结、不得新增 route、posting、ledger entry 或污染余额。
     */
    @Test
    void testAuthorizeSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_AUTHORIZE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_AUTHORIZE");
        BalanceSnapshot afterFirstAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstAuthorizeFacts = ledgerFactSnapshot();

        String retryAuthorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_AUTHORIZE");
        BalanceSnapshot afterRetryAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryAuthorizationSn).isEqualTo(authorizationSn);
        assertOnlyBalanceDeltas(afterFirstAuthorize, afterRetryAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);
        assertThatThrownBy(() -> authorize(user, 61L, true, "AUTH_IDEMPOTENT_AUTHORIZE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryAuthorize, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_AUTHORIZE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_AUTHORIZE", 1, 2);
    }

    /**
     * 场景：授权批准使用相同业务流水重复提交，但第二次把授权账户换成新的主体。
     * 输入：两个账户各充值 100，第一次账户 A 授权 60，随后同业务流水改为账户 B 授权 60。
     * 输出：第二次请求被幂等摘要拒绝；账户 B 不冻结，授权聚合和账务事实保持第一次授权后的状态。
     * 预期：授权批准幂等必须覆盖授权主体，不只覆盖金额。
     * 红线：同业务流水不同授权主体不得新增 detail、route、posting、ledger entry 或污染余额。
     */
    @Test
    void testAuthorizeSameBusinessSnWithDifferentAccountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId anotherUser = fundingAccount("auth_user2");
        ensureLedger(anotherUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, anotherUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_ACCOUNT_TOPUP");
        BalanceSnapshot afterUserTopup = snapshot(balances(user, anotherUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterUserTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        topup(anotherUser, 100L, "AUTH_IDEMPOTENT_ACCOUNT_ANOTHER_TOPUP");
        BalanceSnapshot afterAnotherTopup = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterUserTopup, afterAnotherTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_IDEMPOTENT_ACCOUNT");
        BalanceSnapshot afterFirstAuthorize = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterAnotherTopup, afterFirstAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstAuthorizeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorize(anotherUser, 60L, true, "AUTH_IDEMPOTENT_ACCOUNT"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, anotherUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterFirstAuthorize, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstAuthorizeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT_ANOTHER_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_ACCOUNT", 1, 2);
    }

    /**
     * 场景：授权撤销使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、撤销 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次撤销后的状态。
     * 预期：授权撤销幂等必须保护原授权引用、撤销金额和原 route replay 摘要。
     * 红线：同业务流水不同撤销请求不得重复释放授权占用或污染授权累计金额。
     */
    @Test
    void testAuthorizationReversalSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_REVERSAL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String firstReversalSn = reverseAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL");
        BalanceSnapshot afterFirstReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstReversalFacts = ledgerFactSnapshot();

        String retryReversalSn = reverseAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL");
        BalanceSnapshot afterRetryReversal = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryReversalSn).isEqualTo(firstReversalSn);
        assertOnlyBalanceDeltas(afterFirstReversal, afterRetryReversal,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);
        assertThatThrownBy(() -> reverseAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_REVERSAL_CANCEL"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryReversal, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstReversalFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isEqualTo(30L);
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.REVERSAL.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL", 0, 1, 1, 2);
    }

    /**
     * 场景：授权完成使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、完成 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次完成后的状态。
     * 预期：授权完成幂等必须保护原授权引用、完成金额和原 route replay 摘要。
     * 红线：同业务流水不同完成请求不得重复扣减 AUTHORIZATION、不得重复增加 SETTLEMENT。
     */
    @Test
    void testAuthorizationSettleSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_SETTLE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_SETTLE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String firstSettleSn = settleAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_SETTLE_CAPTURE");
        BalanceSnapshot afterFirstSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterFirstSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));
        LedgerFactSnapshot afterFirstSettleFacts = ledgerFactSnapshot();

        String retrySettleSn = settleAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_SETTLE_CAPTURE");
        BalanceSnapshot afterRetrySettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retrySettleSn).isEqualTo(firstSettleSn);
        assertOnlyBalanceDeltas(afterFirstSettle, afterRetrySettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstSettleFacts);
        assertThatThrownBy(() -> settleAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_SETTLE_CAPTURE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetrySettle, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstSettleFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isEqualTo(30L);
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_SETTLE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_SETTLE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_SETTLE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：授权完成后退款使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、完成 50、退款 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次退款后的状态。
     * 预期：授权退款幂等必须保护原授权引用、退款金额和原完成路径回放摘要。
     * 红线：同业务流水不同退款请求不得重复回补 AVAILABLE、不得重复扣减 SETTLEMENT。
     */
    @Test
    void testAuthorizationRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 50L, authorizationSn, "AUTH_IDEMPOTENT_REFUND_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        String firstRefundSn = refundSettledAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN");
        BalanceSnapshot afterFirstRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterFirstRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY));
        LedgerFactSnapshot afterFirstRefundFacts = ledgerFactSnapshot();

        String retryRefundSn = refundSettledAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN");
        BalanceSnapshot afterRetryRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryRefundSn).isEqualTo(firstRefundSn);
        assertOnlyBalanceDeltas(afterFirstRefund, afterRetryRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);
        assertThatThrownBy(() -> refundSettledAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_REFUND_RETURN"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryRefund, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstRefundFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(30L);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.AUTH_REFUND.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(5);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN", 0, 2, 1, 2);
    }
}
