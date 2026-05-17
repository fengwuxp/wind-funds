package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransferFundsInstructionFeeRouteResolverTests extends TransferFundsInstructionRouteResolverTestSupport {

    @Test
    void testResolveTransferShouldAppendFeeLegWhenFeeSpecProvided() {
        FundsInstructionSpec instruction = converter.convertToTransferInstruction(new FundsTransactionTransferRequest()
                .setPayerAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeAccountId(FundsRouteTestSupport.fundingAccount("funding_002"))
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(700L))
                .setFeeSpec(FundsRouteTestSupport.fixedFeeSpec(30L))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("INTERNAL_TRANSFER_STANDARD");
        assertThat(route.getLegs()).hasSize(2);
        assertThat(route.getLegs().get(1).getLegId()).isEqualTo("FEE");
        assertThat(route.getLegs().get(1).getLegType()).isEqualTo(RouteLegType.INTERNAL_TRANSFER);
        assertThat(route.getLegs().get(1).getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.CONSUME);
        assertThat(route.getLegs().get(1).getPhaseCode()).isEqualTo(LedgerPhaseCode.FEE);
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getFeeFundingAccount()).isNotNull();
        assertThat(route.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TRANSFER);
    }

    /**
     * 场景：共享额度/信用账户付款时由交易层显式传入手续费。
     * 输入：CREDIT_ACCOUNT 直接支付给商户 SETTLEMENT，同时携带固定手续费 FeeSpec。
     * 输出：主交易 leg 加一条从信用账户 AVAILABLE 到平台 FEE 的手续费 leg。
     * 预期：手续费不依赖账户级 FeeProvider，且信用账户同样生成手续费路径。
     * 红线：信用账户交易不得漏收小额交易费、授权拒付费、跨境交易费等业务层已决策费用。
     */
    @Test
    void testResolvePayShouldAppendFeeLegForCreditAccountWhenFeeSpecProvided() {
        FundsAccountId accountId = FundsRouteTestSupport.creditAccount("credit_001");
        FundsInstructionSpec instruction = converter.convertToPayInstruction(new FundsTransactionPayRequest()
                .setAccountId(accountId)
                .setPayeeId(FundsRouteTestSupport.fundingAccount("merchant_001"))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(500L))
                .setFeeSpec(FundsRouteTestSupport.fixedFeeSpec(25L))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("DIRECT_PAY_STANDARD");
        assertThat(route.getLegs()).hasSize(2);
        assertLeg(route.getLegs().get(0), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getLegs().get(1)).satisfies(leg -> {
            assertThat(leg.getLegId()).isEqualTo("FEE");
            assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                    LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
            assertThat(leg.getAmount()).isEqualTo(FundsRouteTestSupport.amount(25L));
            assertThat(leg.getSourceNode().getSubjectRef().getSubjectId()).isEqualTo(accountId.id());
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getFeeFundingAccount()).isNotNull();
    }

    @Test
    void testResolveFeeShouldBuildIndependentInternalTransferLeg() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToFeeInstruction(new FundsTransactionFeeRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(30L))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("FEE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                    LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getFeeFundingAccount()).isNotNull();
    }
}
