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
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
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
     * 场景：授权拒绝使用相同业务流水重复提交，第二次请求摘要一致时复用原拒绝交易，拒绝原因变化时拒绝。
     * 输入：账户充值 100 后，授权拒绝 60，拒绝原因为 RISK_DECLINED；随后同流水同原因重试，再改为 LIMIT_DECLINED。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持首次拒绝后的状态。
     * 预期：授权拒绝幂等必须保护拒绝金额、授权主体和拒绝原因。
     * 红线：同业务流水不同拒绝原因不得静默复用原交易，也不得生成 route、posting、ledger entry 或污染拒绝事实。
     */
    @Test
    void testAuthorizationDeclineSameBusinessSnWithDifferentReasonShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_DECLINE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();

        String declinedSn = declineAuthorization(user, 60L, "RISK_DECLINED", "AUTH_IDEMPOTENT_DECLINE");
        BalanceSnapshot afterFirstDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        String retryDeclinedSn = declineAuthorization(user, 60L, "RISK_DECLINED", "AUTH_IDEMPOTENT_DECLINE");
        BalanceSnapshot afterRetryDecline = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryDeclinedSn).isEqualTo(declinedSn);
        assertOnlyBalanceDeltas(afterFirstDecline, afterRetryDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertThatThrownBy(() -> declineAuthorization(user, 60L, "LIMIT_DECLINED", "AUTH_IDEMPOTENT_DECLINE"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryDecline, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(declinedSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isZero();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(declinedSn))
                .hasValueSatisfying(routeSnapshot -> assertThat(routeSnapshot.getLegs()).isEmpty());

        assertThat(fundsTransactionDetails(declinedSn))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.REJECTED);
                    assertThat(detail.getLedgerTransactionSn()).isNull();
                    assertThat(detail.getContextVariables())
                            .contains("RISK_DECLINED")
                            .doesNotContain("LIMIT_DECLINED");
                });
        assertPostedTransactions(1);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_DECLINE_TOPUP", 3, 4);
        assertThat(fundsTransactionsByBusinessSn("AUTH_IDEMPOTENT_DECLINE"))
                .singleElement()
                .satisfies(rejectedTransaction -> {
                    assertThat(rejectedTransaction.getSn()).isEqualTo(declinedSn);
                    assertThat(rejectedTransaction.getStatus()).isEqualTo(FundsTransactionStatus.REJECTED);
                    assertNoLedgerFactsForFundsTransaction(rejectedTransaction.getSn());
                });
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_DECLINE"))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getTransactionSn()).isEqualTo(declinedSn);
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

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REVERSAL_AUTHORIZE");
        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REVERSAL_CANCEL");
        assertThat(reversalTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REVERSAL.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REVERSAL_CANCEL", 0, 1, 1, 2);
    }

    /**
     * 场景：用户充值后授权批准，先完成部分金额，再由系统过期释放剩余授权。
     * 输入：充值 100、授权批准 80、完成 30、授权过期释放 50。
     * 输出：AVAILABLE/AUTHORIZATION/SETTLEMENT 余额变化、过期明细和原 route replay 账务事实。
     * 预期：过期只释放剩余授权占用，终态为 EXPIRED，事件语义为 EXPIRE。
     * 红线：授权过期不得复用 reversal 终态和事件，不得释放已完成金额。
     */
    @Test
    void testAuthorizationPartialSettleThenExpireShouldReleaseOnlyRemainingAuthorizationBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_PARTIAL_SETTLE_EXPIRE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_PARTIAL_SETTLE_EXPIRE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 30L, authorizationSn, "AUTH_PARTIAL_SETTLE_EXPIRE_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY));

        expireAuthorization(user, 50L, authorizationSn, "AUTH_PARTIAL_SETTLE_EXPIRE_EXPIRE");
        BalanceSnapshot afterExpire = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterExpire,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.EXPIRED);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getSettledAmount()).isEqualTo(30L);
        assertThat(transaction.getReversedAmount()).isEqualTo(50L);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.EXPIRE.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_SETTLE_EXPIRE_AUTHORIZE");
        LedgerTransaction expireTransaction = ledgerTransactionByBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_EXPIRE");
        assertThat(expireTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(expireTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_EXPIRE").stream()
                .map(FundsTransactionDetail::getEventType)
                .toList())
                .containsOnly(FundsTransactionEventType.EXPIRE);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_PARTIAL_SETTLE_EXPIRE_EXPIRE", 0, 1, 1, 2);
    }

    /**
     * 场景：用户授权批准后已完成部分金额，系统收到超过剩余授权金额的过期释放请求。
     * 输入：充值 100、授权批准 80、完成 30、尝试过期释放 80。
     * 输出：过期释放失败，不生成本次过期资金事实、账本交易和分录。
     * 预期：原授权交易仍保持 OPEN，已完成金额不被过期释放，余额不变。
     * 红线：授权过期只能释放剩余授权占用，不得释放已完成金额。
     */
    @Test
    void testAuthorizationExpireMoreThanRemainingShouldFailWithoutExpireFacts() {
        FundsAccountId user = fundingAccount("funding_user");

        topup(user, 100L, "AUTH_EXPIRE_EXCEED_TOPUP");
        String authorizationSn = authorize(user, 80L, true, "AUTH_EXPIRE_EXCEED_AUTHORIZE");
        settleAuthorization(user, 30L, authorizationSn, "AUTH_EXPIRE_EXCEED_CAPTURE");
        BalanceSnapshot beforeExpire = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThatThrownBy(() -> expireAuthorization(user, 80L, authorizationSn, "AUTH_EXPIRE_EXCEED_EXPIRE"))
                .hasMessageContaining("资金交易剩余授权可释放金额不足")
                .hasMessageContaining("remainingAmount = 50")
                .hasMessageContaining("amount = 80");

        BalanceSnapshot afterExpire = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeExpire, afterExpire,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getSettledAmount()).isEqualTo(30L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_EXPIRE_EXCEED_EXPIRE");
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

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_SETTLE_AUTHORIZE");
        LedgerTransaction reversalTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL");
        assertThat(reversalTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(reversalTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(reversalTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REVERSAL.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());

        LedgerTransaction settleTransaction = ledgerTransactionByBusinessSn(
                "AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE");
        assertThat(settleTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(entriesOf(settleTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AUTHORIZATION, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(settleTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_PARTIAL_REVERSAL_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
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
        assertThat(settleTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
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
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_SETTLE_CAPTURE", 0, 2, 1, 2);
    }

    /**
     * 场景：外部已经完成扣款但本系统没有内部授权事实，运营按授权后继能力发起强制完成。
     * 输入：充值 100、强制完成 60、策略上限 60、外部原始事实引用和凭证引用齐备。
     * 输出：用户 AVAILABLE 直接扣减，平台 SETTLEMENT 增加，AUTHORIZATION 不变，账务事实保留强制完成审计上下文。
     * 预期：强制完成不伪造 authorizationTransactionSn，也不消费 AUTHORIZATION 桶。
     * 红线：没有内部授权事实时，普通完成路径不得被复用成“查不到授权”的失败，也不得绕过策略、原因和外部证据。
     */
    @Test
    void testForceSettleWithoutAuthorizationShouldConsumeAvailableBalanceAndPreserveAuditContext() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_FORCE_SETTLE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String forceSettleSn = forceSettleAuthorization(user, 60L, "AUTH_FORCE_SETTLE_CAPTURE");

        BalanceSnapshot afterForceSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterForceSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(forceSettleSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isEqualTo(60L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isZero();

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.SETTLE.name());

        LedgerTransaction settleTransaction = ledgerTransactionByBusinessSn("AUTH_FORCE_SETTLE_CAPTURE");
        assertThat(settleTransaction.getReferenceLedgerTransactionSn()).isNull();
        List<LedgerPostingPlan> settlePostingPlans = postingPlansOf(settleTransaction);
        assertThat(entriesOf(settleTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertThat(settlePostingPlans)
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.getSn()).hasSizeLessThanOrEqualTo(64);
                    assertThat(plan.getRouteLegId()).isEqualTo("FORCE_SETTLEMENT_1");
                });
        assertThat(settlePostingPlans.stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnlyNulls();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnlyNulls();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FORCE_SETTLE_CAPTURE"))
                .allSatisfy(detail -> assertThat(detail.getContextVariables())
                        .contains("\"settleMode\":\"FORCE\"")
                        .contains("\"forceSettlePolicyCode\":\"B4_FORCE_SETTLE_OPS\"")
                        .contains("\"externalOriginalFactRef\":\"processor_settlement_202606020001\"")
                        .contains("\"forceSettleVoucherRef\":\"ops_voucher_202606020001\""));
        assertLedgerFactsFollowRouteSnapshot("AUTH_FORCE_SETTLE_CAPTURE");
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_CAPTURE", 1, 2, 1, 2);
    }

    /**
     * 场景：强制完成请求缺少策略编码或金额超过策略上限。
     * 输入：充值 100，分别提交缺策略、超上限的强制完成请求。
     * 输出：请求在交易事实创建前被拒绝，余额、账务事实和交易事实不变化。
     * 预期：强制完成必须显式携带策略、原因、外部原始事实、凭证和金额上限。
     * 红线：缺少授权事实的完成不得降级成普通完成，不得在参数非法时留下 FAILED 资金交易或半成功账务。
     */
    @Test
    void testForceSettleMissingPolicyOrExceedingLimitShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_FORCE_SETTLE_REJECT_TOPUP");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_MISSING_POLICY")
                .setForceSettlePolicyCode(null), WindOperator.system()))
                .hasMessageContaining("forceSettlePolicyCode");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_UNKNOWN_POLICY")
                .setForceSettlePolicyCode("UNKNOWN_FORCE_SETTLE_POLICY"), WindOperator.system()))
                .hasMessageContaining("forceSettlePolicyCode");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_EXCEED_LIMIT")
                .setForceSettleLimitAmount(50L), WindOperator.system()))
                .hasMessageContaining("forceSettleLimitAmount");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_LIMIT_MISMATCH")
                .setForceSettleLimitAmount(99L), WindOperator.system()))
                .hasMessageContaining("forceSettleLimitAmount");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_WITH_AUTH_SN")
                .setAuthorizationTransactionSn("FT_SHOULD_NOT_BE_ACCEPTED"), WindOperator.system()))
                .hasMessageContaining("authorizationTransactionSn");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_MISSING_REASON")
                .setForceSettleReason("   "), WindOperator.system()))
                .hasMessageContaining("forceSettleReason");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_MISSING_EXTERNAL_FACT")
                .setExternalOriginalFactRef("   "), WindOperator.system()))
                .hasMessageContaining("externalOriginalFactRef");
        assertThatThrownBy(() -> authorizationTransactionService.settle(forceSettleRequest(user, 60L,
                "AUTH_FORCE_SETTLE_MISSING_VOUCHER")
                .setForceSettleVoucherRef("   "), WindOperator.system()))
                .hasMessageContaining("forceSettleVoucherRef");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_REJECT_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_MISSING_POLICY");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_UNKNOWN_POLICY");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_EXCEED_LIMIT");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_LIMIT_MISMATCH");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_WITH_AUTH_SN");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_MISSING_EXTERNAL_FACT");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_FORCE_SETTLE_MISSING_VOUCHER");
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

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REFUND_AUTHORIZE");
        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("AUTH_FULL_REFUND_RETURN");
        assertThat(refundTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
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
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_FULL_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_FULL_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：外部引用可追溯，但本系统没有内部授权流水，运营发起无授权直接退款。
     * 输入：用户充值 100 后向平台结算户付款 70 形成可退结算余额；随后按 `NO_AUTH` 模式退款 40。
     * 输出：用户 AVAILABLE 恢复 40，平台 SETTLEMENT 扣减 40，退款资金事实保留外部引用和退款原因。
     * 预期：无授权退款不携带内部授权流水，不查询原授权账本交易，也不补造 AUTHORIZATION 占用。
     * 红线：无授权退款不得按普通授权链退款回放，不得把外部事实伪装成内部授权或原交易聚合。
     */
    @Test
    void testNoAuthRefundShouldCreateStandaloneRefundFactAndPreserveExternalOriginalFact() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_NO_AUTH_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L, "AUTH_NO_AUTH_REFUND_EXTERNAL_CAPTURE");
        BalanceSnapshot afterExternalCapture = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterExternalCapture,
                delta(user, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY));

        String noAuthRefundSn = refundWithoutAuthorization(user, 40L, "AUTH_NO_AUTH_REFUND_RETURN");
        BalanceSnapshot afterNoAuthRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterExternalCapture, afterNoAuthRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(noAuthRefundSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.CLOSED);
        assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(transaction.getAuthorizedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isZero();
        assertThat(transaction.getRefundedAmount()).isEqualTo(40L);
        assertThat(transaction.getReferenceTransactionSn()).isNull();

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN");
        assertThat(refundTransaction.getReferenceLedgerTransactionSn()).isNull();
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN"))
                .allSatisfy(detail -> {
                    assertThat(detail.getReferenceDetailSn()).isNull();
                    assertThat(detail.getReferenceLedgerTransactionSn()).isNull();
                    assertThat(detail.getContextVariables())
                            .contains("\"refundMode\":\"NO_AUTH\"")
                            .contains("\"externalReferenceSn\":\"processor_capture_202606030001\"")
                            .contains("\"refundReason\":\"external capture refunded without internal authorization\"")
                            .doesNotContain("externalOriginalFactRef")
                            .doesNotContain("externalOriginalFactType")
                            .doesNotContain("refundVoucherRef")
                            .doesNotContain("originalFactAmount")
                            .doesNotContain("originalFactCurrency")
                            .doesNotContain("authorizationTransactionSn");
                });
        assertThat(fundsTransactionsByBusinessSn("AUTH_NO_AUTH_REFUND_RETURN"))
                .singleElement()
                .satisfies(refund -> assertThat(refund.getRouteSnapshot())
                        .contains("AUTHORIZATION_NO_AUTH_REFUND_STANDARD")
                        .doesNotContain("AUTHORIZATION_REFUND_REPLAY"));
        assertLedgerFactsFollowRouteSnapshot("AUTH_NO_AUTH_REFUND_RETURN");
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_EXTERNAL_CAPTURE", 2, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_RETURN", 1, 2, 1, 2);
    }

    /**
     * 场景：调用方不再传入退款模式字段，且未携带内部原授权流水。
     * 输入：平台结算户已有外部原消费沉淀余额，提交无 authorizationTransactionSn、无退款模式入参的请求。
     * 输出：系统按无授权退款处理，仍补充 NO_AUTH 上下文标签并保留外部引用审计。
     * 预期：无授权退款判定以内部原授权流水是否为空为准，refundMode 只作为资金指令内部归类标签。
     * 红线：请求侧没有 refundMode 时不得回退成普通授权链退款，也不得查询内部原授权账本交易。
     */
    @Test
    void testNoAuthRefundShouldInferModeWhenAuthorizationTransactionSnIsBlank() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_INFER_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_INFER_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        String refundSn = authorizationTransactionService.settleRefund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_INFER_RETURN"), WindOperator.system());

        BalanceSnapshot afterRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeRefund, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        assertThat(fundsTransaction(refundSn).getReferenceTransactionSn()).isNull();
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN"))
                .allSatisfy(detail -> assertThat(detail.getContextVariables())
                        .contains("\"refundMode\":\"NO_AUTH\"")
                        .contains("\"externalReferenceSn\":\"processor_capture_202606030001\"")
                        .contains("\"refundReason\":\"external capture refunded without internal authorization\"")
                        .doesNotContain("externalOriginalFactRef")
                        .doesNotContain("externalOriginalFactType")
                        .doesNotContain("refundVoucherRef")
                        .doesNotContain("originalFactAmount")
                        .doesNotContain("originalFactCurrency")
                        .doesNotContain("authorizationTransactionSn"));
        assertThat(fundsTransactionsByBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN"))
                .singleElement()
                .satisfies(refund -> assertThat(refund.getRouteSnapshot())
                        .contains("AUTHORIZATION_NO_AUTH_REFUND_STANDARD")
                        .doesNotContain("AUTHORIZATION_REFUND_REPLAY"));
        assertFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_INFER_RETURN", 1, 2, 1, 2);
    }

    /**
     * 场景：无授权退款缺少外部引用、原因或错误携带内部授权流水。
     * 输入：平台结算户已有可退余额，分别提交非法 no-auth refund 请求。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：无内部授权流水的退款必须携带最小外部引用和原因，且不得携带 `authorizationTransactionSn`。
     * 红线：无授权退款不得回退成普通授权链退款，不得查询原授权账本交易或留下半成功事实。
     */
    @Test
    void testNoAuthRefundMissingRequiredAuditFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_REJECT_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_REJECT_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_MISSING_EXTERNAL_REFERENCE")
                .setExternalReferenceSn("   "), WindOperator.system()))
                .hasMessageContaining("externalReferenceSn");

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_MISSING_REASON").setRefundReason("   "), WindOperator.system()))
                .hasMessageContaining("refundReason");

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_WITH_AUTH_SN")
                .setAuthorizationTransactionSn("FT202606030000000001"), WindOperator.system()))
                .hasMessageContaining("authorizationTransactionSn");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_REJECT_EXTERNAL_CAPTURE", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_MISSING_EXTERNAL_REFERENCE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_WITH_AUTH_SN");
    }

    /**
     * 场景：无授权退款请求错误携带争议/拒付字段。
     * 输入：无内部原授权流水的退款请求，同时携带 disputeMode、disputeReason、disputeVoucherRef 和 externalDisputeRef。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：NO_AUTH 退款与已完成授权后的争议退款互斥，不能静默丢弃争议审计字段。
     * 红线：无授权退款不得被带争议字段的请求伪装成争议退款，也不得在忽略争议字段后成功入账。
     */
    @Test
    void testNoAuthRefundWithDisputeFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_TOPUP");
        pay(user, settlementAccount(), LedgerSubjectCode.SETTLEMENT, 70L,
                "AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_EXTERNAL_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(noAuthRefundRequest(user, 40L,
                "AUTH_NO_AUTH_REFUND_WITH_DISPUTE")
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_NO_AUTH")
                .setExternalDisputeRef("DISPUTE_CASE_NO_AUTH"), WindOperator.system()))
                .hasMessageContaining("dispute");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_DISPUTE_REJECT_EXTERNAL_CAPTURE", 2, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_NO_AUTH_REFUND_WITH_DISPUTE");
    }

    /**
     * 场景：已完成授权发生外部争议，业务侧通过授权链退款承接争议退回。
     * 输入：充值 100、授权 60、完成 60、争议类退款 40，并携带争议原因和凭证引用。
     * 输出：用户 AVAILABLE 恢复 40，平台 SETTLEMENT 释放 40，资金明细和账本交易保留争议审计上下文。
     * 预期：争议类退款仍走 `settleRefund` 的 AUTH_REFUND 资金事实，可与普通退款通过业务场景和上下文区分。
     * 红线：争议类退款不得被压缩成授权拒绝，不得误写 CHARGEBACK 事件或 declinedAmount。
     */
    @Test
    void testAuthorizationDisputeRefundShouldUseSettleRefundAndPreserveAuditContext() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_DISPUTE_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_DISPUTE_REFUND_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 60L, authorizationSn, "AUTH_DISPUTE_REFUND_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        authorizationTransactionService.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(40L, CURRENCY))
                .setAuthorizationTransactionSn(authorizationSn)
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                .setExternalDisputeRef("DISPUTE_CASE_202605290001")
                .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                .setBusinessSn("AUTH_DISPUTE_REFUND_RETURN")
                .setDescription("authorization dispute refund")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "caseOwner", "ops-team-a"))), WindOperator.system());
        BalanceSnapshot afterDisputeRefund = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterDisputeRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getSettledAmount()).isEqualTo(60L);
        assertThat(transaction.getRefundedAmount()).isEqualTo(40L);
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

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_DISPUTE_REFUND_AUTHORIZE");
        LedgerTransaction disputeRefundTransaction = ledgerTransactionByBusinessSn("AUTH_DISPUTE_REFUND_RETURN");
        assertThat(disputeRefundTransaction.getBusinessScene()).isEqualTo("AUTHORIZATION_DISPUTE_REFUND");
        assertThat(disputeRefundTransaction.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND.name());
        assertThat(disputeRefundTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(disputeRefundTransaction.getContextVariables())
                .contains("\"refundMode\":\"DISPUTE\"")
                .contains("\"disputeMode\":\"CHARGEBACK\"")
                .contains("\"disputeReason\":\"CARDHOLDER_DISPUTE\"")
                .contains("\"disputeVoucherRef\":\"DISPUTE_EVIDENCE_202605290001\"")
                .contains("\"externalDisputeRef\":\"DISPUTE_CASE_202605290001\"")
                .contains("\"caseOwner\":\"ops-team-a\"")
                .doesNotContain("declineReason");
        assertThat(entriesOf(disputeRefundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(disputeRefundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());
        assertThat(postingPlansOf(disputeRefundTransaction))
                .allSatisfy(plan -> assertThat(plan.getContextVariables())
                        .contains("\"refundMode\":\"DISPUTE\"")
                        .contains("\"disputeMode\":\"CHARGEBACK\"")
                        .contains("\"disputeReason\":\"CARDHOLDER_DISPUTE\"")
                        .contains("\"disputeVoucherRef\":\"DISPUTE_EVIDENCE_202605290001\"")
                        .contains("\"externalDisputeRef\":\"DISPUTE_CASE_202605290001\"")
                        .contains("\"caseOwner\":\"ops-team-a\""));
        assertThat(entriesOf(disputeRefundTransaction))
                .allSatisfy(entry -> assertThat(entry.getContextVariables())
                        .contains("\"refundMode\":\"DISPUTE\"")
                        .contains("\"disputeMode\":\"CHARGEBACK\"")
                        .contains("\"disputeReason\":\"CARDHOLDER_DISPUTE\"")
                        .contains("\"disputeVoucherRef\":\"DISPUTE_EVIDENCE_202605290001\"")
                        .contains("\"externalDisputeRef\":\"DISPUTE_CASE_202605290001\"")
                        .contains("\"caseOwner\":\"ops-team-a\""));

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_DISPUTE_REFUND_RETURN"))
                .allSatisfy(detail -> {
                    assertThat(detail.getBusinessScene()).isEqualTo("AUTHORIZATION_DISPUTE_REFUND");
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
                    assertThat(detail.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.RETURN);
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.SUCCEEDED);
                    assertThat(detail.getReferenceDetailSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
                    assertThat(detail.getContextVariables())
                            .contains("\"refundMode\":\"DISPUTE\"")
                            .contains("\"disputeMode\":\"CHARGEBACK\"")
                            .contains("\"disputeReason\":\"CARDHOLDER_DISPUTE\"")
                            .contains("\"disputeVoucherRef\":\"DISPUTE_EVIDENCE_202605290001\"")
                            .contains("\"externalDisputeRef\":\"DISPUTE_CASE_202605290001\"")
                            .contains("\"caseOwner\":\"ops-team-a\"")
                            .doesNotContain("declineReason");
                    assertThat(detail.getRequestHash()).isNotBlank();
                });
        BalanceSnapshot beforeIdempotencyConflict = snapshot(balances(user, cashMappingAccount(),
                settlementAccount()));
        LedgerFactSnapshot beforeIdempotencyConflictFacts = ledgerFactSnapshot();
        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(
                new FundsAuthorizationTransactionRefundRequest()
                        .setAccountId(user)
                        .setAmount(Money.immutable(40L, CURRENCY))
                        .setAuthorizationTransactionSn(authorizationSn)
                        .setDisputeMode("CHARGEBACK")
                        .setDisputeReason("CARDHOLDER_DISPUTE")
                        .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                        .setExternalDisputeRef("DISPUTE_CASE_CHANGED")
                        .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                        .setBusinessSn("AUTH_DISPUTE_REFUND_RETURN")
                        .setDescription("authorization dispute refund")
                        .setContextVariables(WritableContextVariables.of(Map.of(
                                "caseOwner", "ops-team-a"))), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");
        BalanceSnapshot afterIdempotencyConflict = snapshot(balances(user, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(beforeIdempotencyConflict, afterIdempotencyConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeIdempotencyConflictFacts);

        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：争议类授权退款缺少模式、原因、凭证或外部争议引用。
     * 输入：已完成授权后分别提交 dispute 审计字段半填或空白的退款请求。
     * 输出：请求在交易事实创建前失败，余额、账务事实和资金事实均不变化。
     * 预期：争议类退款必须完整携带模式、原因、凭证和外部争议引用。
     * 红线：争议类退款不得半填审计字段后退化成普通退款。
     */
    @Test
    void testAuthorizationDisputeRefundMissingRequiredAuditFieldsShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        topup(user, 100L, "AUTH_DISPUTE_REFUND_REJECT_TOPUP");
        String authorizationSn = authorize(user, 60L, true, "AUTH_DISPUTE_REFUND_REJECT_AUTHORIZE");
        settleAuthorization(user, 60L, authorizationSn, "AUTH_DISPUTE_REFUND_REJECT_CAPTURE");
        BalanceSnapshot beforeFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_MODE")
                .setDisputeMode("   "), WindOperator.system()))
                .hasMessageContaining("disputeMode");

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_REASON")
                .setDisputeReason("   "), WindOperator.system()))
                .hasMessageContaining("disputeReason");

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_VOUCHER")
                .setDisputeVoucherRef("   "), WindOperator.system()))
                .hasMessageContaining("disputeVoucherRef");

        assertThatThrownBy(() -> authorizationTransactionService.settleRefund(disputeRefundRequest(user, 40L,
                authorizationSn, "AUTH_DISPUTE_REFUND_MISSING_EXTERNAL_REF")
                .setExternalDisputeRef(null), WindOperator.system()))
                .hasMessageContaining("externalDisputeRef");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_REJECT_CAPTURE", 0, 2, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_MODE");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_VOUCHER");
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_DISPUTE_REFUND_MISSING_EXTERNAL_REF");
    }

    /**
     * 场景：已完成授权发生拒付，业务侧使用现有 chargeback 入口承接争议扣回。
     * 输入：充值 100、授权 60、完成 60、拒付 40，并携带拒付原因和凭证引用。
     * 输出：用户 AVAILABLE 恢复 40，平台 SETTLEMENT 释放 40，拒付原因和凭证进入资金明细和账本交易。
     * 预期：拒付按原授权 route snapshot 回放为 CHARGEBACK 事实，可与授权拒绝、普通退款区分。
     * 红线：拒付不得被压缩成授权拒绝，不得丢失原授权账本引用、原因、凭证和外部争议引用。
     */
    @Test
    void testAuthorizationChargebackShouldReplaySettlePathAndPreserveAuditContext() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        topup(user, 100L, "AUTH_CHARGEBACK_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 60L, true, "AUTH_CHARGEBACK_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 60L, authorizationSn, "AUTH_CHARGEBACK_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));

        authorizationTransactionService.chargeback(new FundsAuthorizationTransactionChargebackRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(40L, CURRENCY))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene("AUTHORIZATION_CHARGEBACK")
                .setBusinessSn("AUTH_CHARGEBACK_RETURN")
                .setDescription("authorization chargeback")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "chargebackReason", "CARDHOLDER_DISPUTE",
                        "evidenceRef", "CHARGEBACK_EVIDENCE_202605290001",
                        "externalDisputeRef", "CHARGEBACK_CASE_202605290001"))), WindOperator.system());
        BalanceSnapshot afterChargeback = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterChargeback,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -40L, CURRENCY));

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(60L);
        assertThat(transaction.getSettledAmount()).isEqualTo(60L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isEqualTo(40L);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.CHARGEBACK.name());

        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn("AUTH_CHARGEBACK_AUTHORIZE");
        LedgerTransaction chargebackTransaction = ledgerTransactionByBusinessSn("AUTH_CHARGEBACK_RETURN");
        assertThat(chargebackTransaction.getBusinessScene()).isEqualTo("AUTHORIZATION_CHARGEBACK");
        assertThat(chargebackTransaction.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK.name());
        assertThat(chargebackTransaction.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
        assertThat(chargebackTransaction.getContextVariables())
                .contains("CARDHOLDER_DISPUTE", "CHARGEBACK_EVIDENCE_202605290001",
                        "CHARGEBACK_CASE_202605290001");
        assertThat(entriesOf(chargebackTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(chargebackTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.CHARGEBACK.name());

        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_CHARGEBACK_RETURN"))
                .allSatisfy(detail -> {
                    assertThat(detail.getBusinessScene()).isEqualTo("AUTHORIZATION_CHARGEBACK");
                    assertThat(detail.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
                    assertThat(detail.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(detail.getFundsEffectType()).isEqualTo(FundsEffectType.RETURN);
                    assertThat(detail.getStatus()).isEqualTo(FundsTransactionDetailStatus.SUCCEEDED);
                    assertThat(detail.getReferenceDetailSn()).isEqualTo(authorizationSn);
                    assertThat(detail.getReferenceLedgerTransactionSn()).isEqualTo(authorizationTransaction.getSn());
                    assertThat(detail.getContextVariables())
                            .contains("CARDHOLDER_DISPUTE", "CHARGEBACK_EVIDENCE_202605290001",
                                    "CHARGEBACK_CASE_202605290001")
                            .doesNotContain("declineReason");
                });
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：用户授权 80 后只完成 50，平台结算户另有充足余额时尝试拒付 60。
     * 输入：A 充值并授权 80、完成 50；B 另完成 100 使平台 SETTLEMENT 余额充足；A 拒付 60。
     * 输出：A 拒付请求失败，A/B/平台余额、交易累计和账务事实保持失败前状态。
     * 预期：拒付以本交易已完成可回退金额为上限，不以授权金额或平台总余额为上限。
     * 红线：失败拒付不得借用其他交易沉淀在 SETTLEMENT 的余额，不得写入 CHARGEBACK 账务事实。
     */
    @Test
    void testAuthorizationChargebackExceedingSettledAmountShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId reserveUser = fundingAccount("settlement_reserve_user");
        ensureLedger(reserveUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(reserveUser, LedgerSubjectCode.AUTHORIZATION);

        BalanceSnapshot beforeTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_CHARGEBACK_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_CHARGEBACK_EXCEED_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 50L, authorizationSn, "AUTH_CHARGEBACK_EXCEED_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, reserveUser, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        topup(reserveUser, 100L, "AUTH_CHARGEBACK_EXCEED_RESERVE_TOPUP");
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
                "AUTH_CHARGEBACK_EXCEED_RESERVE_AUTHORIZE");
        BalanceSnapshot afterReserveAuthorize = snapshot(balances(user, reserveUser, cashMappingAccount(),
                settlementAccount()));
        assertOnlyBalanceDeltas(afterReserveTopup, afterReserveAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(reserveUser, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(reserveUser, 100L, reserveAuthorizationSn,
                "AUTH_CHARGEBACK_EXCEED_RESERVE_CAPTURE");

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

        assertThatThrownBy(() -> authorizationTransactionService.chargeback(
                new FundsAuthorizationTransactionChargebackRequest()
                        .setAccountId(user)
                        .setAmount(Money.immutable(60L, CURRENCY))
                        .setAuthorizationTransactionSn(authorizationSn)
                        .setBusinessScene("AUTHORIZATION_CHARGEBACK")
                        .setBusinessSn("AUTH_CHARGEBACK_EXCEED_RETURN")
                        .setDescription("authorization chargeback exceed")
                        .setContextVariables(WritableContextVariables.of(Map.of(
                                "chargebackReason", "CARDHOLDER_DISPUTE",
                                "evidenceRef", "CHARGEBACK_EVIDENCE_EXCEED_202605290001"))),
                WindOperator.system()))
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
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_CAPTURE", 0, 2, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_RESERVE_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_RESERVE_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_RESERVE_CAPTURE", 0, 2, 1, 2);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("AUTH_CHARGEBACK_EXCEED_RETURN");
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
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_REVERSAL_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REVERSAL_CANCEL").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
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
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_SETTLE_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_SETTLE_CAPTURE")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_SETTLE_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
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
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_REFUND_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_REFUND_RETURN", 0, 2, 1, 2);
    }

    /**
     * 场景：授权完成后拒付使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、授权批准 80、完成 50、拒付 30，随后同流水同金额重试，再同流水改金额为 31。
     * 输出：同摘要重试返回同一授权交易流水；摘要冲突抛错；余额和账务事实保持第一次拒付后的状态。
     * 预期：授权拒付幂等必须保护原授权引用、拒付金额、拒付原因和原完成路径回放摘要。
     * 红线：同业务流水不同拒付请求不得重复回补 AVAILABLE、不得重复扣减 SETTLEMENT 或污染 declinedAmount。
     */
    @Test
    void testAuthorizationChargebackSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        topup(user, 100L, "AUTH_IDEMPOTENT_CHARGEBACK_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        String authorizationSn = authorize(user, 80L, true, "AUTH_IDEMPOTENT_CHARGEBACK_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterAuthorize,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

        settleAuthorization(user, 50L, authorizationSn, "AUTH_IDEMPOTENT_CHARGEBACK_CAPTURE");
        BalanceSnapshot afterSettle = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterAuthorize, afterSettle,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY));

        String firstChargebackSn = chargebackAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_CHARGEBACK_RETURN");
        BalanceSnapshot afterFirstChargeback = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterSettle, afterFirstChargeback,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY));
        LedgerFactSnapshot afterFirstChargebackFacts = ledgerFactSnapshot();

        String retryChargebackSn = chargebackAuthorization(user, 30L, authorizationSn,
                "AUTH_IDEMPOTENT_CHARGEBACK_RETURN");
        BalanceSnapshot afterRetryChargeback = snapshot(balances(user, cashMappingAccount(), settlementAccount()));

        assertThat(retryChargebackSn).isEqualTo(firstChargebackSn);
        assertOnlyBalanceDeltas(afterFirstChargeback, afterRetryChargeback,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstChargebackFacts);
        assertThatThrownBy(() -> chargebackAuthorization(user, 31L, authorizationSn,
                "AUTH_IDEMPOTENT_CHARGEBACK_RETURN"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), settlementAccount()));
        assertOnlyBalanceDeltas(afterRetryChargeback, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstChargebackFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.AUTHORIZATION, 30L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(settlementAccount()), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);

        FundsTransactionDTO transaction = fundsTransaction(authorizationSn);
        assertThat(transaction.getStatus()).isEqualTo(FundsTransactionStatus.OPEN);
        assertThat(transaction.getAuthorizedAmount()).isEqualTo(80L);
        assertThat(transaction.getReversedAmount()).isZero();
        assertThat(transaction.getSettledAmount()).isEqualTo(50L);
        assertThat(transaction.getRefundedAmount()).isZero();
        assertThat(transaction.getDeclinedAmount()).isEqualTo(30L);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.AUTHORIZE.name(),
                        FundsTransactionEventType.SETTLE.name(),
                        FundsTransactionEventType.CHARGEBACK.name());
        assertThat(fundsTransactionDetails(authorizationSn)).hasSize(5);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceDetailSn)
                .toList())
                .containsOnly(authorizationSn);
        LedgerTransaction authorizationTransaction = ledgerTransactionByBusinessSn(
                "AUTH_IDEMPOTENT_CHARGEBACK_AUTHORIZE");
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_CAPTURE")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(ledgerTransactionByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_RETURN")
                .getReferenceLedgerTransactionSn())
                .isEqualTo(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_CAPTURE").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertThat(fundsTransactionDetailsByBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_RETURN").stream()
                .map(FundsTransactionDetail::getReferenceLedgerTransactionSn)
                .toList())
                .containsOnly(authorizationTransaction.getSn());
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_TOPUP", 3, 4);
        assertSingleFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_AUTHORIZE", 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_CAPTURE", 0, 2, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("AUTH_IDEMPOTENT_CHARGEBACK_RETURN", 0, 2, 1, 2);
    }

    private String chargebackAuthorization(FundsAccountId accountId,
                                           long amount,
                                           String authorizationTransactionSn,
                                           String businessSn) {
        return authorizationTransactionService.chargeback(new FundsAuthorizationTransactionChargebackRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setBusinessScene("AUTHORIZATION_CHARGEBACK")
                .setBusinessSn(businessSn)
                .setDescription("authorization chargeback")
                .setContextVariables(WritableContextVariables.of(Map.of(
                        "chargebackReason", "CARDHOLDER_DISPUTE",
                        "evidenceRef", "CHARGEBACK_EVIDENCE_IDEMPOTENT_202605290001"))),
                WindOperator.system());
    }

    private String forceSettleAuthorization(FundsAccountId accountId, long amount, String businessSn) {
        return authorizationTransactionService.settle(forceSettleRequest(accountId, amount, businessSn),
                WindOperator.system());
    }

    private FundsAuthorizationTransactionSettleRequest forceSettleRequest(FundsAccountId accountId,
                                                                          long amount,
                                                                          String businessSn) {
        return new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setSettleMode("FORCE")
                .setForceSettlePolicyCode("B4_FORCE_SETTLE_OPS")
                .setForceSettleLimitAmount(amount)
                .setForceSettleReason("external settlement accepted without internal authorization")
                .setExternalOriginalFactRef("processor_settlement_202606020001")
                .setForceSettleVoucherRef("ops_voucher_202606020001")
                .setBusinessScene("AUTHORIZATION_FORCE_SETTLE")
                .setBusinessSn(businessSn)
                .setDescription("authorization force settle");
    }

    private FundsAuthorizationTransactionRefundRequest disputeRefundRequest(FundsAccountId accountId,
                                                                            long amount,
                                                                            String authorizationTransactionSn,
                                                                            String businessSn) {
        return new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setAuthorizationTransactionSn(authorizationTransactionSn)
                .setDisputeMode("CHARGEBACK")
                .setDisputeReason("CARDHOLDER_DISPUTE")
                .setDisputeVoucherRef("DISPUTE_EVIDENCE_202605290001")
                .setExternalDisputeRef("DISPUTE_CASE_202605290001")
                .setBusinessScene("AUTHORIZATION_DISPUTE_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("authorization dispute refund");
    }
}
