package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionOrchestrationReplayFlowTests extends FundsTransactionOrchestrationFlowTestSupport {
    /**
     * 场景：已授权金额在后续被撤销释放。
     * 输入：带原授权交易号的 reversal 请求，且能查询到原授权快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原授权快照生成
     * `AUTHORIZATION_REVERSAL_REPLAY` 路径并完成入账。
     */
    @Test
    void testReversalShouldReplayOriginalAuthorizationPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());

        String transactionSn = service.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(creditAccount("credit_001"))
                .setAmount(amount(100L))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_REVERSAL")
                .setBusinessSn("REVERSAL_0001")
                .setDescription("reversal"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("AUTHORIZATION_REVERSAL_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.REVERSAL);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：已授权金额在后续进入结算扣款。
     * 输入：带原授权交易号的 settle 请求，且能查询到原授权快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原授权快照生成
     * `AUTHORIZATION_SETTLE_REPLAY` 路径并完成入账。
     */
    @Test
    void testSettleShouldReplayOriginalAuthorizationPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());

        String transactionSn = service.settle(new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_SETTLE")
                .setBusinessSn("SETTLE_0001")
                .setDescription("settle"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.SETTLEMENT);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：已结算授权交易发生拒付/争议。
     * 输入：带原授权交易号的 chargeback 请求，且能查询到原结算快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原快照生成
     * `CHARGEBACK_REPLAY` 路径并完成入账。
     */
    @Test
    void testChargebackShouldReplayOriginalSettlementPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalSettlementSnapshot());

        String transactionSn = service.chargeback(new FundsAuthorizationTransactionChargebackRequest()
                .setAccountId(creditAccount("credit_001"))
                .setAmount(amount(100L))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_POST_SETTLEMENT_DISPUTE")
                .setBusinessSn("CHARGEBACK_0001")
                .setDescription("chargeback"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.CHARGEBACK);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：解冻请求带冻结单引用。
     * 输入：`referenceType=FREEZE_ORDER` 的 unfreeze 请求。
     * 输出：回放路径编码、账本阶段、原 leg 引用和成功回填的账本交易号。
     * 预期：编排器通过冻结单号定位原冻结路径快照，生成 `BALANCE_UNFREEZE_REPLAY` 路径并完成入账。
     */
    @Test
    void testUnfreezeWithFreezeOrderReferenceShouldReplayOriginalFreezePath() {
        transactionQueryService.freezeOrderSnapshots.put("FREEZE_ORDER_0001", originalFreezeSnapshot());

        String transactionSn = service.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setAmount(amount(100L))
                .setReferenceFreezeSn("FREEZE_ORDER_0001")
                .setBusinessScene("RISK_UNFREEZE")
                .setBusinessSn("UNFREEZE_0001")
                .setDescription("unfreeze"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("BALANCE_UNFREEZE_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
    }

}
