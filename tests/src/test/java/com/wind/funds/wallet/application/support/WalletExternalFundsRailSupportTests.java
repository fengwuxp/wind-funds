package com.wind.funds.wallet.application.support;

import com.wind.funds.wallet.application.support.WalletExternalFundsRailSupport.ExternalCreditRailDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 钱包外部资金 rail 解析测试。
 */
class WalletExternalFundsRailSupportTests {

    /**
     * 场景：支付工具收款入口接收交易枚举名、业务 rail 或通道别名。
     * 输入：WIRE_TRANSFER、wire-transfer、bank_rail 和 ach。
     * 输出：统一解析为当前交易层可承载的 WIRE_TRANSFER。
     * 红线：wallet application 不把业务 rail 直接下沉给交易内核。
     */
    @Test
    void testResolveReceiveChannelShouldNormalizeSupportedRailAliases() {
        assertThat(WalletExternalFundsRailSupport.resolveReceiveChannelCode("WIRE_TRANSFER"))
                .isEqualTo("WIRE_TRANSFER");
        assertThat(WalletExternalFundsRailSupport.resolveReceiveChannelCode("wire-transfer"))
                .isEqualTo("WIRE_TRANSFER");
        assertThat(WalletExternalFundsRailSupport.resolveReceiveChannelCode("bank_rail"))
                .isEqualTo("WIRE_TRANSFER");
        assertThat(WalletExternalFundsRailSupport.resolveReceiveChannelCode("ach"))
                .isEqualTo("WIRE_TRANSFER");
    }

    /**
     * 场景：支付工具收款入口收到未知 rail。
     * 输入：mystery_rail。
     * 输出：返回面向调用方的拒绝原因和支持列表。
     * 红线：不得暴露 Enum.valueOf 这类实现细节异常。
     */
    @Test
    void testResolveReceiveChannelShouldRejectUnknownRailAlias() {
        assertThatThrownBy(() -> WalletExternalFundsRailSupport.resolveReceiveChannelCode("mystery_rail"))
                .hasMessageContaining("收款渠道编码不支持")
                .hasMessageContaining("mystery_rail")
                .hasMessageContaining("支持的收款渠道");
    }

    /**
     * 场景：已确认外部入金事件进入资金域前先归一 rail 和交易渠道。
     * 输入：大小写不敏感的 ACH、银行和通用外部入金事件。
     * 输出：给出外部 rail 解释码，并映射到当前交易层的 WIRE_TRANSFER 渠道。
     * 红线：confirmed credit 可入金不代表扣账、退票、NOC 或撤销可入金。
     */
    @Test
    void testRequireConfirmedCreditRailDecisionShouldExplainEventMapping() {
        ExternalCreditRailDecision ach =
                WalletExternalFundsRailSupport.requireConfirmedCreditRailDecision("ach_credit_confirmed");
        ExternalCreditRailDecision bank =
                WalletExternalFundsRailSupport.requireConfirmedCreditRailDecision("BANK_CREDIT_CONFIRMED");
        ExternalCreditRailDecision external =
                WalletExternalFundsRailSupport.requireConfirmedCreditRailDecision("external-credit-confirmed");

        assertThat(ach.externalRailCode()).isEqualTo("ACH_RAIL");
        assertThat(bank.externalRailCode()).isEqualTo("BANK_RAIL");
        assertThat(external.externalRailCode()).isEqualTo("EXTERNAL_RAIL");
        assertThat(ach.transactionChannelCode()).isEqualTo("WIRE_TRANSFER");
        assertThat(bank.transactionChannelCode()).isEqualTo("WIRE_TRANSFER");
        assertThat(external.transactionChannelCode()).isEqualTo("WIRE_TRANSFER");
    }

    /**
     * 场景：外部事件类型不是已确认入金。
     * 输入：ACH_RETURN_CONFIRMED。
     * 输出：拒绝真实消费。
     * 红线：return/NOC/reversal 不能复用 confirmed credit 入金通道。
     */
    @Test
    void testRequireConfirmedCreditRailDecisionShouldRejectUnsupportedEventType() {
        assertThatThrownBy(() -> WalletExternalFundsRailSupport.requireConfirmedCreditRailDecision(
                "ACH_RETURN_CONFIRMED"))
                .hasMessageContaining("外部资金事件类型暂不支持真实消费");
    }
}
