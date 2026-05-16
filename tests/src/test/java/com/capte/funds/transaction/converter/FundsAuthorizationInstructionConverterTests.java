package com.capte.funds.transaction.converter;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.MerchantInfoRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.enums.InternationalRegionCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsAuthorizationInstructionConverterTests {

    private FundsAuthorizationInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.authorizationInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testConvertToAuthorizeInstructionShouldCarryMerchantAndDeclineContext() {
        MerchantInfoRequest merchantInfo = new MerchantInfoRequest()
                .setMerchantId("merchant_001")
                .setMerchantName("merchant")
                .setMccCode("5812");
        FundsInstructionSpec instruction = converter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setTransactionAmount(FundsRouteTestSupport.transactionAmount(600L))
                        .setApproved(Boolean.FALSE)
                        .setDeclineReason("risk_denied")
                        .setTransactionCountry(InternationalRegionCode.US)
                        .setMerchantInfo(merchantInfo)
                        .setBusinessScene("CARD_AUTH")
                        .setBusinessSn("AUTH_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.APPROVED, Boolean.FALSE)
                .containsEntry(FundsInstructionContextKeys.DECLINE_REASON, "risk_denied")
                .containsEntry(FundsInstructionContextKeys.TRANSACTION_COUNTRY, InternationalRegionCode.US)
                .containsEntry(FundsInstructionContextKeys.MERCHANT_INFO, merchantInfo);
    }

    @Test
    void testConvertToSettleInstructionShouldCreateAuthorizationReference() {
        FundsInstructionSpec instruction = converter.convertToSettleInstruction(
                new FundsAuthorizationTransactionSettleRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setTransactionAmount(FundsRouteTestSupport.transactionAmount(500L))
                        .setBusinessScene("CARD_SETTLE")
                        .setBusinessSn("SETTLE_0001")
                        .setAuthorizationTransactionSn("AUTH_TX_0001"), WindOperator.system());

        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_0001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN, "AUTH_TX_0001");
    }

    @Test
    void testConvertToReversalInstructionShouldCreateAuthorizationReference() {
        FundsInstructionSpec instruction = converter.convertToReversalInstruction(
                new FundsAuthorizationTransactionReversalRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setAmount(FundsRouteTestSupport.amount(300L))
                        .setBusinessScene("CARD_REVERSAL")
                        .setBusinessSn("REVERSAL_0001")
                        .setAuthorizationTransactionSn("AUTH_TX_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_0001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN, "AUTH_TX_0001");
    }

    @Test
    void testConvertToSettleRefundInstructionShouldCreateAuthorizationReference() {
        FundsInstructionSpec instruction = converter.convertToSettleRefundInstruction(
                new FundsAuthorizationTransactionRefundRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setAmount(FundsRouteTestSupport.amount(200L))
                        .setBusinessScene("CARD_REFUND")
                        .setBusinessSn("AUTH_REFUND_0001")
                        .setAuthorizationTransactionSn("AUTH_TX_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_0001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN, "AUTH_TX_0001");
    }

    @Test
    void testConvertToChargebackInstructionShouldCreatePostSettlementDisputeReference() {
        FundsInstructionSpec instruction = converter.convertToChargebackInstruction(
                new FundsAuthorizationTransactionChargebackRequest()
                        .setAccountId(FundsRouteTestSupport.creditAccount("credit_001"))
                        .setAmount(FundsRouteTestSupport.amount(100L))
                        .setBusinessScene("CARD_POST_SETTLEMENT_DISPUTE")
                        .setBusinessSn("CHARGEBACK_0001")
                        .setAuthorizationTransactionSn("AUTH_TX_0001"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getBusinessScene()).isEqualTo("CARD_POST_SETTLEMENT_DISPUTE");
        assertThat(instruction.getBusinessSn()).isEqualTo("CHARGEBACK_0001");
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_0001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN, "AUTH_TX_0001");
    }
}
