package com.capte.funds.dsl;

import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /**
     * 场景：服务层转换账本交易 DTO 时，交易上下文携带权益金额和资金责任。
     * 预期：DTO 构造被拒绝，避免账务响应事实承载权益核心事实。
     * 红线：账本交易 DTO 上下文不得成为权益核心事实的旁路承载。
     */
    @Test
    void testLedgerTransactionDtoShouldRejectCoreBenefitContextVariables() {
        assertThatThrownBy(() -> new LedgerTransactionDTO()
                .setContextVariables(Map.of(
                        "amount", "100.00",
                        "fundingNature", "COUPON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerTransactionDto.contextVariables must not contain core benefit field");
    }

    /**
     * 场景：服务层转换账本分录 DTO 时，分录上下文嵌套携带实时营销规则。
     * 预期：DTO 构造被拒绝，避免账务分录响应事实承载实时权益决策事实。
     * 红线：账本分录 DTO 上下文不得成为权益核心事实的旁路承载。
     */
    @Test
    void testLedgerEntryDtoShouldRejectNestedCoreBenefitContextVariables() {
        assertThatThrownBy(() -> new LedgerEntryDTO()
                .setContextVariables(Map.of(
                        "benefitDecisionTrace", Map.of("currentMarketingRule", "RULE-DTO-001"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerEntryDto.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");
    }
}
