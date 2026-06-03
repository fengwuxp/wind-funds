package com.wind.funds.dsl;

import com.wind.funds.ledger.request.UpdateLedgerTransactionRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 账务对外请求上下文契约测试。
 */
class LedgerRequestContextVariablesContractTests {

    /**
     * 场景：调用方构造账本交易更新请求后，继续改写原始上下文。
     * 预期：请求对象持有构造时快照，不被外部可变 Map 污染。
     * 红线：更新请求不得因浅拷贝让 PAN、密钥或外部账户原文进入账务事实。
     */
    @Test
    void testUpdateLedgerTransactionRequestShouldDefensivelyCopyNestedContextVariable() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:update-ledger-transaction-request-001");
        Map<String, Object> contextVariable = new HashMap<>();
        contextVariable.put("processorPayload", processorPayload);

        UpdateLedgerTransactionRequest request = new UpdateLedgerTransactionRequest()
                .setContextVariable(contextVariable);

        contextVariable.put("pan", "PAN_AFTER_LEDGER_UPDATE_REQUEST_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_LEDGER_UPDATE_REQUEST_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(request.getContextVariable()).doesNotContainKey("pan");
        Object payloadValue = request.getContextVariable().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:update-ledger-transaction-request-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：调用方不传账本交易上下文更新字段。
     * 预期：请求对象保留 null，以维持服务层“未更新上下文”的语义。
     */
    @Test
    void testUpdateLedgerTransactionRequestShouldKeepNullContextVariableAsNotUpdated() {
        UpdateLedgerTransactionRequest request = new UpdateLedgerTransactionRequest()
                .setContextVariable(null);

        assertThat(request.getContextVariable()).isNull();
    }

    /**
     * 场景：调用方构造账本交易更新请求时，上下文携带权益金额和资金责任。
     * 预期：请求构造被拒绝，避免更新入口成为权益核心事实的旁路承载。
     * 红线：账本交易更新请求上下文不得承载权益核心事实。
     */
    @Test
    void testUpdateLedgerTransactionRequestShouldRejectCoreBenefitContextVariable() {
        assertThatThrownBy(() -> new UpdateLedgerTransactionRequest()
                .setContextVariable(Map.of(
                        "amount", "100.00",
                        "fundingNature", "COUPON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "updateLedgerTransactionRequest.contextVariables must not contain core benefit field");
    }
}
