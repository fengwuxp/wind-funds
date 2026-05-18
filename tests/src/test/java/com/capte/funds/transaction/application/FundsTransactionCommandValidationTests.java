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

    /**
     * 场景：充值请求错误地把内部资金账户作为资金来源。
     * 输入：目标 FundingAccount、来源 FundingAccount 和充值金额 1000。
     * 输出：命令服务在进入编排器前抛出校验异常。
     * 预期：充值资金来源必须是外部账户，校验失败时不生成资金指令。
     */
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

    /**
     * 场景：提现请求错误地把内部资金账户作为出款收款方。
     * 输入：付款 FundingAccount、内部 FundingAccount 收款方、冻结单引用和提现金额 800。
     * 输出：命令服务在进入编排器前抛出校验异常。
     * 预期：提现收款方必须是外部账户，校验失败时不生成资金指令。
     */
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

    /**
     * 场景：转账请求付款方和收款方是同一资金账户。
     * 输入：同一个 FundingAccount 同时作为 payer 和 payee。
     * 输出：命令服务在进入编排器前抛出校验异常。
     * 预期：同主体自转账被拒绝，不生成资金指令。
     */
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
