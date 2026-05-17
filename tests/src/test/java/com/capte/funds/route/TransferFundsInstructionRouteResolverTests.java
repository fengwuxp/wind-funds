package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferFundsInstructionRouteResolverTests extends TransferFundsInstructionRouteResolverTestSupport {

    @Test
    void testResolveTopupShouldBuildExternalInAndInternalTransferLegs() {
        FundsAccountId fundingAccountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToTopupInstruction(new FundsTransactionTopupRequest()
                .setAccountId(fundingAccountId)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("bank_txn_001")
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(1_000L))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("TOPUP_STANDARD");
        assertThat(route.getLegs()).hasSize(2);
        assertLeg(route.getLegs().get(0), RouteLegType.EXTERNAL_IN, LedgerSubjectCode.CASH,
                LedgerSubjectCode.PREPAYMENT, LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.FUND_IN);
        assertLeg(route.getLegs().get(1), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.PREPAYMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getLegs().get(1).getTargetNode().getSubjectRef().getSubjectId())
                .isEqualTo(fundingAccountId.id());
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getCashFundingAccount()).isNotNull();
        assertThat(route.getPlatformAccounts().getPrepaymentFundingAccount()).isNotNull();
        assertThat(route.getExternalAccountRef()).isNotNull();
        assertThat(route.getExternalAccountRef().getExternalAccountId()).isEqualTo("external_bank_001");
        assertThat(route.getExternalAccountRef().getContextVariables())
                .containsEntry("externalTransactionId", "bank_txn_001");
    }

    @Test
    void testResolveWithdrawShouldBuildConsumeAndExternalOutLegs() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToWithdrawInstruction(new FundsTransactionWithdrawRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_0001")
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(800L))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("WITHDRAW_STANDARD");
        assertThat(route.getLegs()).hasSize(2);
        assertLeg(route.getLegs().get(0), RouteLegType.CONSUME, LedgerSubjectCode.FROZEN,
                LedgerSubjectCode.PREPAYMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertLeg(route.getLegs().get(1), RouteLegType.EXTERNAL_OUT, LedgerSubjectCode.PREPAYMENT,
                LedgerSubjectCode.CASH, LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.FUND_OUT);
        assertMustNotBeNegative(route.getLegs().get(0), accountId, LedgerSubjectCode.FROZEN);
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getCashFundingAccount()).isNotNull();
        assertThat(route.getPlatformAccounts().getPrepaymentFundingAccount()).isNotNull();
        assertThat(route.getExternalAccountRef()).isNotNull();
        assertThat(route.getExternalAccountRef().getExternalAccountId()).isEqualTo("external_bank_001");
    }

    /**
     * 场景：付款方直接支付给收款方结算账目。
     * 输入：付款方资金账户发起 PAY，收款方指定 SETTLEMENT 账目。
     * 输出：付款方 AVAILABLE 消费到收款方 SETTLEMENT。
     * 预期：付款方 source AVAILABLE 携带 MUST_NOT_BE_NEGATIVE 约束。
     * 红线：负 AVAILABLE 不得被当作可继续消费余额。
     */
    @Test
    void testResolvePayShouldUseProvidedPayeeLedgerSubjectCode() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToPayInstruction(new FundsTransactionPayRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsRouteTestSupport.fundingAccount("merchant_001"))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(500L))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("DIRECT_PAY_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.INTERNAL_TRANSFER);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.SETTLEMENT);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.SETTLEMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    @Test
    void testResolveTransferShouldBuildSingleInternalTransferLeg() {
        FundsAccountId payerAccountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToTransferInstruction(new FundsTransactionTransferRequest()
                .setPayerAccountId(payerAccountId)
                .setPayeeAccountId(FundsRouteTestSupport.fundingAccount("funding_002"))
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(700L))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("INTERNAL_TRANSFER_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                    LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.TRANSFER);
            assertMustNotBeNegative(leg, payerAccountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：提现 route 需要平台 CASH_MAPPING 和 PREPAYMENT 账户，但配置缺失。
     * 输入：已转换的提现指令，route resolver 查询平台 PREPAYMENT 时失败。
     * 输出：route 解析失败。
     * 预期：错误向上抛出，不生成缺平台账户的 route snapshot。
     * 红线：平台账户角色不得被 route 层隐式创建、跳过或用角色字面量代替真实资金账户。
     */
    @Test
    void testResolveWithdrawShouldRejectMissingPlatformPrepaymentAccount() {
        FundsInstructionSpec instruction = converter.convertToWithdrawInstruction(new FundsTransactionWithdrawRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_0002")
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(800L))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_0002"), WindOperator.system());
        TransferFundsInstructionRouteResolver resolver = transferRouteResolverWithout(
                PlatformFundingAccountRole.PREPAYMENT);

        assertThatThrownBy(() -> resolver.resolve(instruction))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing platform funding account: PREPAYMENT");
    }

    private static TransferFundsInstructionRouteResolver transferRouteResolverWithout(
            PlatformFundingAccountRole missingRole) {
        return new TransferFundsInstructionRouteResolver(new RouteParticipantFactory(), new RouteSubjectSupport(),
                new PlatformAccountRouteSupport(FundsRouteTestSupport.platformFundingAccountServiceWithout(
                        Set.of(missingRole))));
    }
}
