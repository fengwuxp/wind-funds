package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsTransactionCommandValidationTests extends FundsTransactionCommandServiceImplTestSupport {

    @Test
    void testTopupShouldRejectNonExternalSourceBeforeOrchestrator() {
        FundsAccountId target = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.topup(new FundsTransactionTopupRequest()
                .setAccountId(target)
                .setFundsSourceAccountId(fundingAccount("funding_002"))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("bank_txn_001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(1_000L)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_INVALID_SOURCE")
                .setDescription("topup"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("top-up funds source must external account");
        assertThat(instruction()).isNull();
    }

    @Test
    void testWithdrawShouldRejectNonExternalPayeeBeforeOrchestrator() {
        FundsAccountId payer = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(payer)
                .setPayeeId(fundingAccount("funding_002"))
                .setReferenceFreezeSn("FREEZE_00000001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(800L)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_INVALID_PAYEE")
                .setDescription("withdraw"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("withdraw payee must external account");
        assertThat(instruction()).isNull();
    }

    @Test
    void testTransferShouldRejectSamePayerAndPayeeBeforeOrchestrator() {
        FundsAccountId account = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(account)
                .setPayeeAccountId(account)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(500L)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_SAME_ACCOUNT")
                .setDescription("transfer"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能一致");
        assertThat(instruction()).isNull();
    }
}
