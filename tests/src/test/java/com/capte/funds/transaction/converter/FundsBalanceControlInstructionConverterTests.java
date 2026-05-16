package com.capte.funds.transaction.converter;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsBalanceControlInstructionConverterTests {

    private FundsBalanceControlInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.balanceControlInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testConvertToFreezeInstructionShouldUseBalanceControlInstruction() {
        FundsInstructionSpec instruction = converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(FundsRouteTestSupport.amount(400L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_0001")
                .setDescription("freeze"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FREEZE);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"));
        assertThat(instruction.getReference()).isNull();
    }

    @Test
    void testConvertToFreezeInstructionShouldRejectWrongCurrencyWithoutFx() {
        assertThatThrownBy(() -> converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(Money.immutable(400L, CurrencyIsoCode.EUR))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_WRONG_CURRENCY"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("amount currency must equal account currency");
    }

    @Test
    void testConvertToAdjustInstructionShouldUseIncreaseEventAndAccountContext() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                .setAmount(FundsRouteTestSupport.amount(5_000L))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("LIMIT")
                .setBusinessSn("LIMIT_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.LIMIT_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID, FundsRouteTestSupport.creditAccount("credit_001"))
                .containsEntry(FundsInstructionContextKeys.INCREASE, Boolean.TRUE);
    }

    @Test
    void testConvertToFundingAdjustInstructionShouldUseBalanceAdjustEvent() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(new FundsBalanceAdjustRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(FundsRouteTestSupport.amount(300L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("BALANCE")
                .setBusinessSn("BALANCE_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.ADJUSTMENT);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.BALANCE_ADJUST);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.INCREASE, Boolean.FALSE);
    }

    @Test
    void testConvertToUnfreezeInstructionShouldCarryFreezeReference() {
        FundsInstructionSpec instruction = converter.convertToUnfreezeInstruction(new FundsBalanceUnfreezeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setAmount(FundsRouteTestSupport.amount(100L))
                .setReferenceFreezeSn("FREEZE_0001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_0001"), WindOperator.system());

        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, "FREEZE_0001");
    }
}
