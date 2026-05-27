package com.capte.funds.dsl;

import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.model.FundsTransactionParticipant;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
