package com.capte.funds.transaction.converter;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsDirectTransactionFeeInstructionConverterTests {

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
    void testConvertToFeeInstructionShouldPopulateFeeContext() {
        FundsInstructionSpec instruction = converter.convertToFeeInstruction(new FundsTransactionFeeRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setAmount(FundsRouteTestSupport.amount(30L))
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_0001")
                .setDescription("fee"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FEE_CHARGE);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.FEE);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.FEE_TYPE, DefaultFeeType.FEE.getCode());
    }

    @Test
    void testConvertToFeeRefundInstructionShouldReferenceOriginalFeeTransaction() {
        FundsInstructionSpec instruction = converter.convertToFeeRefundInstruction(
                new FundsTransactionFeeRefundRequest()
                        .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                        .setAmount(FundsRouteTestSupport.amount(30L))
                        .setFeeSourceTransactionSn("FEE_0001")
                        .setBusinessScene("FEE_REFUND")
                        .setBusinessSn("FEE_REFUND_0001")
                        .setDescription("fee refund"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FEE);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FEE_0001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"));
    }
}
