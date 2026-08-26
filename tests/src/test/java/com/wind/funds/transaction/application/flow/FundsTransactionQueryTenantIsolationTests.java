package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金交易查询租户隔离测试。
 */
class FundsTransactionQueryTenantIsolationTests extends FundsTransactionFlowTestSupport {

    private static final Long FOREIGN_TENANT_ID = TENANT_ID + 1;

    /**
     * 场景：其他租户持有当前租户的资金交易流水号。
     * 预期：主交易、明细和路径快照均不可跨租户读取。
     */
    @Test
    void testTransactionFactsShouldNotCrossTenantBoundary() {
        String businessSn = "TENANT_SCOPE_TOPUP";
        topup(fundingAccount("funding_user"), 100L, businessSn);
        String transactionSn = fundsTransactionsByBusinessSn(businessSn).getFirst().getSn();

        assertThat(fundsTransactionQueryService.findFundsTransactionBySn(TENANT_ID, transactionSn)).isPresent();
        assertThat(fundsTransactionQueryService.queryFundsTransactionDetails(TENANT_ID, transactionSn)).isNotEmpty();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(TENANT_ID, transactionSn))
                .isPresent();

        assertThat(fundsTransactionQueryService.findFundsTransactionBySn(FOREIGN_TENANT_ID, transactionSn)).isEmpty();
        assertThat(fundsTransactionQueryService.queryFundsTransactionDetails(FOREIGN_TENANT_ID, transactionSn))
                .isEmpty();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByTransactionSn(
                FOREIGN_TENANT_ID, transactionSn)).isEmpty();
    }

    /**
     * 场景：其他租户持有当前租户的冻结单流水号。
     * 预期：冻结路径快照不可跨租户读取。
     */
    @Test
    void testFrozenOrderRouteSnapshotShouldNotCrossTenantBoundary() {
        topup(fundingAccount("funding_user"), 30L, "TENANT_SCOPE_FREEZE_TOPUP");
        String freezeOrderSn = freeze(fundingAccount("funding_user"), 30L, "TENANT_SCOPE_FREEZE");

        assertThat(fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(TENANT_ID, freezeOrderSn))
                .isPresent();
        assertThat(fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(
                FOREIGN_TENANT_ID, freezeOrderSn)).isEmpty();
    }

    /**
     * 场景：退款已消费原付款路径后，其他租户查询同一 replay leg。
     * 预期：其他租户既看不到消费标记，也不能累计消费金额。
     */
    @Test
    void testReplayConsumptionShouldNotCrossTenantBoundary() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("tenant_scope_payee");
        ensureFundingAccount(payee, LedgerProfileCode.FUNDING_MERCHANT);
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        topup(payer, 100L, "TENANT_SCOPE_REPLAY_TOPUP");
        String payTransactionSn = pay(
                payer, payee, LedgerSubjectCode.SETTLEMENT, 60L, "TENANT_SCOPE_REPLAY_PAY");
        RouteSnapshotSpec payRoute = fundsTransactionQueryService
                .findRouteSnapshotByTransactionSn(TENANT_ID, payTransactionSn)
                .orElseThrow();
        RouteLegSpec sourceLeg = payRoute.getLegs().stream()
                .filter(leg -> leg.getReplayPolicy() != RouteReplayPolicy.NON_REPLAYABLE)
                .findFirst()
                .orElseThrow();
        directTransactionService.refund(new FundsTransactionRefundRequest()
                .setReferenceTransactionSn(payTransactionSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(20L, CURRENCY)))
                .setBusinessScene("REFUND")
                .setBusinessSn("TENANT_SCOPE_REPLAY_REFUND"), WindOperatorFactory.system());

        assertThat(fundsTransactionQueryService.hasConsumedReplayLeg(
                TENANT_ID, payTransactionSn, FundsTransactionEventType.REFUND, sourceLeg.getLegId())).isTrue();
        assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(
                TENANT_ID, payTransactionSn, FundsTransactionEventType.REFUND,
                sourceLeg.getLegId(), CURRENCY).getAmount()).isEqualTo(20L);

        assertThat(fundsTransactionQueryService.hasConsumedReplayLeg(
                FOREIGN_TENANT_ID, payTransactionSn, FundsTransactionEventType.REFUND,
                sourceLeg.getLegId())).isFalse();
        assertThat(fundsTransactionQueryService.sumConsumedReplayLegAmount(
                FOREIGN_TENANT_ID, payTransactionSn, FundsTransactionEventType.REFUND,
                sourceLeg.getLegId(), CURRENCY).getAmount()).isZero();
    }
}
