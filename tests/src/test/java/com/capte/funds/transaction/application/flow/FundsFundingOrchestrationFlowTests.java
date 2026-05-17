package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsFundingOrchestrationFlowTests extends FundsTransactionOrchestrationFlowTestSupport {

    /**
     * 场景：钱包充值从外部账户入金到账户余额。
     * 输入：外部银行账户来源、充值目标账户、金额与渠道流水。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `TOPUP_STANDARD` 路由，包含 `FUND_IN -> SETTLEMENT` 两段路径，并完成账本入账。
     */
    @Test
    void testTopupShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.topup(new FundsTransactionTopupRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        com.wind.integration.funds.wallet.enums.DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("BANK_TXN_0001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_0001")
                .setDescription("topup"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("TOPUP_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("FUND_IN", "TOPUP_SETTLEMENT");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：钱包提现从冻结余额出金到外部账户。
     * 输入：提现账户、外部收款账户、冻结引用号与提现金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `WITHDRAW_STANDARD` 路由，包含 `WITHDRAW_SETTLEMENT -> FUND_OUT` 两段路径，并完成账本入账。
     */
    @Test
    void testWithdrawShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        com.wind.integration.funds.wallet.enums.DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_0001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_0001")
                .setDescription("withdraw"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("WITHDRAW_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("WITHDRAW_SETTLEMENT", "FUND_OUT");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }
}
