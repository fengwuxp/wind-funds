package com.capte.funds.dsl;

import com.wind.integration.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本余额变更事件契约测试。
 */
class LedgerBalanceChangedEventContractTests {

    /**
     * 场景：调用方直接通过 Builder 构造余额变更观察事件，随后继续改写原始上下文。
     * 预期：已构造事件保持发布时快照，不被后续追加字段污染。
     * 红线：余额观察事件不得因浅拷贝让 PAN、密钥或外部账户原文进入监听、日志或报表。
     */
    @Test
    void testBuilderShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:balance-event-builder-001");
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("processorPayload", processorPayload);
        LedgerBalanceChangedEvent event = LedgerBalanceChangedEvent.builder()
                .subjectId("FA-BALANCE-EVENT-001")
                .subjectType("FUNDING_ACCOUNT")
                .ledgerId(1L)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .currency(CurrencyIsoCode.USD)
                .beforeBalance(100L)
                .balance(125L)
                .balanceDelta(25L)
                .ledgerTransactionSn("LT-BALANCE-EVENT-001")
                .ledgerEntrySn("LE-BALANCE-EVENT-001")
                .ledgerEntryDigest("sha256-balance-event-001")
                .businessScene("BALANCE_EVENT_CONTRACT")
                .businessSn("BALANCE_EVENT_CONTRACT_001")
                .transactionTime(LocalDateTime.of(2026, 5, 27, 20, 20))
                .contextVariables(contextVariables)
                .build();

        contextVariables.put("pan", "PAN_AFTER_BALANCE_EVENT_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_BALANCE_EVENT_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(event.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = event.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:balance-event-builder-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }
}
