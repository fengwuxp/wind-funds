package com.capte.funds.dsl;

import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.model.FundsTransactionParticipant;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金交易参与方契约测试。
 */
class FundsTransactionParticipantContractTests {

    /**
     * 场景：生命周期保存前构造交易参与方，随后调用方继续改写原始上下文。
     * 预期：交易参与方持有的是构造时快照，不被外部可变 Map 污染。
     * 红线：参与方上下文不得因浅拷贝让 PAN、密钥或外部账户原文进入交易事实。
     */
    @Test
    void testParticipantShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:participant-context-001");
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("processorPayload", processorPayload);

        FundsTransactionParticipant participant = new FundsTransactionParticipant()
                .setSubjectId("FA-PARTICIPANT-001")
                .setSubjectType("FUNDING_ACCOUNT")
                .setParticipantRole(RouteParticipantRole.PAYER)
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setFundsEffectType(FundsEffectType.DIRECT)
                .setContextVariables(contextVariables);

        contextVariables.put("pan", "PAN_AFTER_PARTICIPANT_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_PARTICIPANT_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(participant.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = participant.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:participant-context-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：交易参与方上下文直接携带支付工具密钥或外部账户字段。
     * 预期：参与方建模入口立即拒绝。
     * 红线：交易参与方上下文会进入资金交易事实，不得成为 PAN、密钥或外部账户原文旁路。
     */
    @Test
    void testParticipantShouldRejectSensitiveContextVariables() {
        assertThatThrownBy(() -> participantWithContext(Map.of("processorPayload",
                Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("fundsTransactionParticipant.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> participantWithContext(Map.of("externalAccount",
                Map.of("bankAccountNo", "123456789012"))))
                .hasMessageContaining("fundsTransactionParticipant.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：交易参与方上下文携带权益金额、资金责任或当前营销规则。
     * 预期：参与方建模入口立即拒绝。
     * 红线：参与方上下文只能承载非关键扩展信息，不能替代权益快照或资金责任一等字段。
     */
    @Test
    void testParticipantShouldRejectCoreBenefitContextVariables() {
        assertThatThrownBy(() -> participantWithContext(Map.of("benefitPayload",
                Map.of(
                        "amount", Money.immutable(100L, CurrencyIsoCode.USD),
                        "fundingNature", "PLATFORM_BORNE"))))
                .hasMessageContaining(
                        "fundsTransactionParticipant.contextVariables must not contain core benefit field");

        assertThatThrownBy(() -> participantWithContext(Map.of("benefitTrace",
                Map.of("currentMarketingRule", "latest-rule"))))
                .hasMessageContaining(
                        "fundsTransactionParticipant.contextVariables must not contain core benefit field: "
                                + "currentMarketingRule");
    }

    private FundsTransactionParticipant participantWithContext(Map<String, Object> contextVariables) {
        return new FundsTransactionParticipant()
                .setSubjectId("FA-PARTICIPANT-001")
                .setSubjectType("FUNDING_ACCOUNT")
                .setParticipantRole(RouteParticipantRole.PAYER)
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setFundsEffectType(FundsEffectType.DIRECT)
                .setContextVariables(contextVariables);
    }
}
