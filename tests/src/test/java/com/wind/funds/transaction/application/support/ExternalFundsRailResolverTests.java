package com.wind.funds.transaction.application.support;

import com.wind.funds.transaction.application.support.ExternalFundsRailResolver.ExternalFundsRailDecision;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 外部资金 rail 解析测试。
 */
class ExternalFundsRailResolverTests {

    /**
     * 场景：支付工具收款入口接收交易枚举名、业务 rail 或通道别名。
     * 输入：BANK_TRANSFER、wire-transfer、bank_rail 和 ach。
     * 输出：统一解析为当前交易层可承载的 BANK_TRANSFER。
     * 红线：业务 rail 不直接下沉给交易内核。
     */
    @Test
    void testRequireReceiveRailDecisionShouldPreserveRailAndNormalizeTransactionChannel() {
        ExternalFundsRailDecision ach =
                ExternalFundsRailResolver.requireReceiveRailDecision("VA", "ach");
        ExternalFundsRailDecision wire =
                ExternalFundsRailResolver.requireReceiveRailDecision("VIRTUAL_ACCOUNT", "wire-transfer");

        assertThat(ach.externalRailCode()).isEqualTo("ACH");
        assertThat(wire.externalRailCode()).isEqualTo("WIRE");
        assertThat(ach.transactionChannel()).isEqualTo(FundsTransactionChannel.BANK_TRANSFER);
        assertThat(wire.transactionChannel()).isEqualTo(FundsTransactionChannel.BANK_TRANSFER);
    }

    /**
     * 场景：支付工具收款入口收到未知 rail。
     * 输入：mystery_rail。
     * 输出：返回面向调用方的拒绝原因和支持列表。
     * 红线：不得暴露 Enum.valueOf 这类实现细节异常。
     */
    @Test
    void testRequireReceiveRailDecisionShouldRejectUnknownRailAlias() {
        assertThatThrownBy(() -> ExternalFundsRailResolver.requireReceiveRailDecision("VA", "mystery_rail"))
                .hasMessageContaining("收款外部 rail 编码不支持")
                .hasMessageContaining("mystery_rail")
                .hasMessageContaining("支持的外部 rail");
    }

    /**
     * 场景：支付工具类型与本次外部 rail 不一致。
     * 输入：VA 工具声明 DIGITAL_WALLET rail。
     * 输出：进入交易内核前拒绝。
     * 红线：调用方不得覆盖支付工具类别并伪造资金渠道。
     */
    @Test
    void testRequireReceiveRailDecisionShouldRejectInstrumentRailMismatch() {
        assertThatThrownBy(() -> ExternalFundsRailResolver.requireReceiveRailDecision("VA", "DIGITAL_WALLET"))
                .hasMessageContaining("支付工具类型与外部 rail 不匹配");
    }

    /**
     * 场景：外部钱包支付工具通过钱包 rail 收款。
     * 输入：EXTERNAL_WALLET 工具和 DIGITAL_WALLET rail。
     * 输出：交易层归一为 DIGITAL_WALLET，同时保留钱包 rail。
     */
    @Test
    void testRequireReceiveRailDecisionShouldSupportExternalWallet() {
        ExternalFundsRailDecision decision = ExternalFundsRailResolver.requireReceiveRailDecision(
                "EXTERNAL_WALLET", "DIGITAL_WALLET");

        assertThat(decision.externalRailCode()).isEqualTo("DIGITAL_WALLET");
        assertThat(decision.transactionChannel()).isEqualTo(FundsTransactionChannel.DIGITAL_WALLET);
    }

    /**
     * 场景：已确认外部入金事件进入资金域前先归一 rail 和交易渠道。
     * 输入：大小写不敏感的 ACH 和银行入金事件。
     * 输出：给出外部 rail 解释码，并映射到当前交易层的 BANK_TRANSFER 渠道。
     * 红线：confirmed credit 可入金不代表扣账、退票、NOC 或撤销可入金。
     */
    @Test
    void testRequireConfirmedCreditRailDecisionShouldExplainEventMapping() {
        ExternalFundsRailDecision ach =
                ExternalFundsRailResolver.requireConfirmedCreditRailDecision("ach_credit_confirmed");
        ExternalFundsRailDecision bank =
                ExternalFundsRailResolver.requireConfirmedCreditRailDecision("BANK_CREDIT_CONFIRMED");

        assertThat(ach.externalRailCode()).isEqualTo("ACH");
        assertThat(bank.externalRailCode()).isEqualTo("BANK");
        assertThat(ach.transactionChannel()).isEqualTo(FundsTransactionChannel.BANK_TRANSFER);
        assertThat(bank.transactionChannel()).isEqualTo(FundsTransactionChannel.BANK_TRANSFER);
    }

    /**
     * 场景：外部事件类型不是已确认入金。
     * 输入：ACH_RETURN_CONFIRMED。
     * 输出：拒绝真实消费。
     * 红线：return/NOC/reversal 不能复用 confirmed credit 入金通道。
     */
    @Test
    void testRequireConfirmedCreditRailDecisionShouldRejectUnsupportedEventType() {
        assertThatThrownBy(() -> ExternalFundsRailResolver.requireConfirmedCreditRailDecision(
                "ACH_RETURN_CONFIRMED"))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");
        assertThatThrownBy(() -> ExternalFundsRailResolver.requireConfirmedCreditRailDecision(
                "EXTERNAL_CREDIT_CONFIRMED"))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");
    }
}
