package com.capte.funds.transaction.application.flow;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

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

        topup(user, 100L, "AUTH_DECLINE_TOPUP");
        BalanceSnapshot beforeDecline = snapshot(balances(user, settlementAccount()));

        String authorizationSn = authorize(user, 60L, false, "AUTH_DECLINE_AUTHORIZE");

        BalanceSnapshot afterDecline = snapshot(balances(user, settlementAccount()));
        assertOnlyBalanceDeltas(beforeDecline, afterDecline,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(settlementAccount(), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));

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
    }
}
