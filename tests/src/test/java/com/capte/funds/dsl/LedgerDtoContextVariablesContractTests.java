package com.capte.funds.dsl;

import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账务对外 DTO 上下文契约测试。
 */
class LedgerDtoContextVariablesContractTests {

    /**
     * 场景：服务层转换账本交易 DTO 后，调用方继续改写原始上下文。
     * 预期：DTO 持有构造时快照，不被外部可变 Map 污染。
     * 红线：对外账务事实不得因浅拷贝让 PAN、密钥或外部账户原文进入响应事实。
     */
    @Test
    void testLedgerTransactionDtoShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:ledger-transaction-dto-001");
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("processorPayload", processorPayload);

        LedgerTransactionDTO transaction = new LedgerTransactionDTO()
                .setContextVariables(contextVariables);

        contextVariables.put("pan", "PAN_AFTER_LEDGER_TRANSACTION_DTO_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_LEDGER_TRANSACTION_DTO_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(transaction.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = transaction.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:ledger-transaction-dto-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：服务层转换账本分录 DTO 后，调用方继续改写原始上下文。
     * 预期：DTO 持有构造时快照，不被外部可变 Map 污染。
     * 红线：对外分录事实不得因浅拷贝让 PAN、密钥或外部账户原文进入响应事实。
     */
    @Test
    void testLedgerEntryDtoShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:ledger-entry-dto-001");
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("processorPayload", processorPayload);

        LedgerEntryDTO entry = new LedgerEntryDTO()
                .setContextVariables(contextVariables);

        contextVariables.put("pan", "PAN_AFTER_LEDGER_ENTRY_DTO_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_LEDGER_ENTRY_DTO_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(entry.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = entry.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:ledger-entry-dto-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }
}
