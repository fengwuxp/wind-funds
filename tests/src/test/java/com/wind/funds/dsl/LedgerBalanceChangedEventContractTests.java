package com.wind.funds.dsl;

import com.wind.funds.ledger.LedgerBalanceChangedEvent;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        LedgerBalanceChangedEvent event = balanceChangedEvent(contextVariables);

        contextVariables.put("pan", "PAN_AFTER_BALANCE_EVENT_SHOULD_NOT_LEAK");
        processorPayload.put("pan", "PAN_AFTER_BALANCE_EVENT_PAYLOAD_SHOULD_NOT_LEAK");

        assertThat(event.getContextVariables()).doesNotContainKey("pan");
        Object payloadValue = event.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:balance-event-builder-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：调用方把权益金额、资金责任或实时营销规则藏入余额变更事件上下文。
     * 预期：事件构造阶段显式失败，但仍允许只放权益快照引用和稳定摘要。
     * 红线：余额观察事件只能携带账务事实派生信号，不能成为权益核心事实的旁路承载。
     */
    @Test
    void testBuilderShouldRejectCoreBenefitFactsButAllowSummaryRefs() {
        assertThatThrownBy(() -> balanceChangedEvent(Map.of("benefitPayload", Map.of(
                "amount", Money.immutable(2000L, CurrencyIsoCode.USD),
                "fundingNature", "PLATFORM_BORNE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerBalanceChangedEvent.contextVariables must not contain core benefit field");

        assertThatThrownBy(() -> balanceChangedEvent(Map.of("benefitDecisionTrace",
                new Object[] {Map.of("currentMarketingRule", "latest-rule")})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerBalanceChangedEvent.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        LedgerBalanceChangedEvent event = balanceChangedEvent(Map.of(
                "benefitSnapshotId", "BS-BALANCE-EVENT-SUMMARY-001",
                "stableDigest", "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "benefitGroupSn", "BG-BALANCE-EVENT-SUMMARY-001",
                "componentSn", "COMP-BALANCE-EVENT-SUMMARY-001",
                "ruleVersion", "rule-v1",
                "refundDecisionId", "refund-decision-001",
                "externalDecisionId", "pricing-decision-001"));

        assertThat(event.getContextVariables())
                .containsEntry("benefitSnapshotId", "BS-BALANCE-EVENT-SUMMARY-001")
                .containsEntry("stableDigest",
                        "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .containsEntry("benefitGroupSn", "BG-BALANCE-EVENT-SUMMARY-001")
                .containsEntry("componentSn", "COMP-BALANCE-EVENT-SUMMARY-001")
                .containsEntry("ruleVersion", "rule-v1")
                .containsEntry("refundDecisionId", "refund-decision-001")
                .containsEntry("externalDecisionId", "pricing-decision-001");
    }

    private LedgerBalanceChangedEvent balanceChangedEvent(Map<String, Object> contextVariables) {
        return LedgerBalanceChangedEvent.builder()
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
    }
}
