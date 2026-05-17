package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionOrchestrationFlowTests extends FundsTransactionOrchestrationFlowTestSupport {
    /**
     * 场景：授权请求被拒绝，业务只需要记录生命周期结果。
     * 输入：`approved=false` 的授权指令。
     * 输出：交易流水号、解析路径、账本组装记录和生命周期成功回填值。
     * 预期：Route 可解析但 legs 为空，不进入账本组装与入账链路，成功结果不回填账本交易号。
     */
    @Test
    void testAuthorizeDeclinedShouldCompleteWithoutPostingLedger() {
        String transactionSn = service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.FALSE)
                .setDeclineReason("insufficient_funds")
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_DECLINED_0001")
                .setDescription("declined"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs()).isEmpty();
        assertThat(postingAssembler.route.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get()).isNull();
    }

    /**
     * 场景：原收款方把已入账资金退回给目标账户。
     * 输入：退款到账账户、原收款方账户、原收款方账本编码与退款金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `DIRECT_REFUND_STANDARD` 路由，包含单条 `REFUND` 恢复路径，并完成账本入账。
     */
    @Test
    void testRefundShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.refund(new FundsTransactionRefundRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayerId(FundsAccountId.immutable("merchant_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(amount(100L))
                .setBusinessScene("REFUND")
                .setBusinessSn("REFUND_0001")
                .setDescription("refund"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("DIRECT_REFUND_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("REFUND");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：两个内部资金账户之间发生普通转账。
     * 输入：付款账户、收款账户与转账金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `INTERNAL_TRANSFER_STANDARD` 路由，包含单条 `TRANSFER` 路径，并完成账本入账。
     */
    @Test
    void testTransferShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeAccountId(FundsAccountId.immutable("funding_002", FundsSubjectType.FUNDING_ACCOUNT))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_0001")
                .setDescription("transfer"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("INTERNAL_TRANSFER_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("TRANSFER");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：账户发生独立手续费扣收。
     * 输入：扣费账户、手续费类型与手续费金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `FEE_STANDARD` 路由，包含单条 `FEE` 路径，并完成账本入账。
     */
    @Test
    void testFeeShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.fee(new FundsTransactionFeeRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setAmount(amount(30L))
                .setFeeType(com.wind.integration.funds.transaction.enums.DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_0001")
                .setDescription("fee"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("FEE_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("FEE");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：付款账户把余额支付给收款账户的指定账本桶。
     * 输入：付款账户、收款账户、收款账本编码与支付金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `DIRECT_PAY_STANDARD` 路由，包含单条 `PAY` 路径，并完成账本入账。
     */
    @Test
    void testPayShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.pay(new FundsTransactionPayRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeId(FundsAccountId.immutable("merchant_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_0001")
                .setDescription("pay"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("DIRECT_PAY_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("PAY");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

}
