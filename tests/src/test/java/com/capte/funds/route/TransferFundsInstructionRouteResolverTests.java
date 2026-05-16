package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransferFundsInstructionRouteResolverTests {

    private FundsDirectTransactionInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.transactionInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

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

    private static void assertLeg(RouteLegSpec leg,
                                  RouteLegType legType,
                                  LedgerSubjectCode sourceLedgerSubjectCode,
                                  LedgerSubjectCode targetLedgerSubjectCode,
                                  LedgerBalanceEffectType balanceEffectType,
                                  LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(sourceLedgerSubjectCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(targetLedgerSubjectCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(balanceEffectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    private static void assertMustNotBeNegative(RouteLegSpec leg,
                                                FundsAccountId accountId,
                                                LedgerSubjectCode ledgerSubjectCode) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }
}
