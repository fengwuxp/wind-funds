package com.capte.funds.transaction.converter;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
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

class FundsDirectTransactionInstructionConverterTests {

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
    void testConvertToTopupInstructionShouldPopulateExternalReferenceAndContext() {
        FundsInstructionSpec instruction = converter.convertToTopupInstruction(new FundsTransactionTopupRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("bank_txn_001")
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(1_000L))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_0001")
                .setDescription("topup"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.TOPUP);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TOPUP);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID, FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.CHANNEL_CODE, FundsTransactionChannel.WIRE_TRANSFER.name());
        assertThat(instruction.getExternalAccountRef()).isNotNull();
        assertThat(instruction.getExternalAccountRef().getExternalAccountId()).isEqualTo("external_bank_001");
        assertThat(instruction.getExternalAccountRef().getChannelCode())
                .isEqualTo(FundsTransactionChannel.WIRE_TRANSFER.name());
        assertThat(instruction.getExternalAccountRef().getContextVariables())
                .containsEntry("externalTransactionId", "bank_txn_001");
    }

    @Test
    void testConvertToTransferInstructionShouldPopulatePayerAndPayeeContext() {
        FundsInstructionSpec instruction = converter.convertToTransferInstruction(new FundsTransactionTransferRequest()
                .setPayerAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeAccountId(FundsRouteTestSupport.fundingAccount("funding_002"))
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(500L))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_0001")
                .setDescription("transfer"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.TRANSFER);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TRANSFER);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.PAYER_ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.PAYEE_ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_002"));
    }

    @Test
    void testConvertToPayInstructionShouldPopulatePayeeContext() {
        FundsInstructionSpec instruction = converter.convertToPayInstruction(new FundsTransactionPayRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeId(FundsRouteTestSupport.fundingAccount("merchant_001"))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(700L))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_0001")
                .setDescription("pay"), WindOperator.system());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.PAY);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.ACCOUNT_ID,
                        FundsRouteTestSupport.fundingAccount("funding_001"))
                .containsEntry(FundsInstructionContextKeys.PAYEE_ID,
                        FundsRouteTestSupport.fundingAccount("merchant_001"))
                .containsEntry(FundsInstructionContextKeys.PAYEE_LEDGER_SUBJECT_CODE, LedgerSubjectCode.SETTLEMENT);
    }

    @Test
    void testConvertToPayInstructionShouldRejectWrongCurrencyWithoutFxDecision() {
        assertThatThrownBy(() -> converter.convertToPayInstruction(new FundsTransactionPayRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeId(FundsRouteTestSupport.fundingAccount("merchant_001"))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(700L, CurrencyIsoCode.EUR)))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_WRONG_CURRENCY"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");
    }

    @Test
    void testConvertToWithdrawInstructionShouldPopulateFreezeReference() {
        FundsInstructionSpec instruction = converter.convertToWithdrawInstruction(new FundsTransactionWithdrawRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_0001")
                .setTransactionAmount(FundsRouteTestSupport.transactionAmount(800L))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_0001")
                .setDescription("withdraw"), WindOperator.system());

        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FREEZE_0001");
        assertThat(instruction.getExternalAccountRef()).isNotNull();
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, "FREEZE_0001");
    }

    @Test
    void testConvertToRefundInstructionShouldPopulateExternalTransactionReference() {
        FundsInstructionSpec instruction = converter.convertToRefundInstruction(new FundsTransactionRefundRequest()
                .setAccountId(FundsRouteTestSupport.fundingAccount("funding_002"))
                .setPayerId(FundsRouteTestSupport.fundingAccount("merchant_001"))
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("refund_channel_001")
                .setAmount(FundsRouteTestSupport.amount(300L))
                .setBusinessScene("REFUND")
                .setBusinessSn("REFUND_0001"), WindOperator.system());

        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getReference()).isNotNull();
        assertThat(instruction.getReference().getReferenceType())
                .isEqualTo(FundsInstructionReferenceType.EXTERNAL_TRANSACTION);
        assertThat(instruction.getReference().getExternalTransactionId()).isEqualTo("refund_channel_001");
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.PAYER_LEDGER_SUBJECT_CODE, LedgerSubjectCode.SETTLEMENT);
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
}
