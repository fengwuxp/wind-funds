package com.capte.funds.dsl;

import com.wind.funds.spec.ledger.SettlementPolicySpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SettlementPolicy DSL 契约测试。
 */
class SettlementPolicySpecTests {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 20, 22, 30);

    /**
     * 场景：清结算规则解析产品 DSL 中声明的标准结算策略。
     * 预期：策略表达稳定解析为类型、窗口和必要参数。
     * 红线：目标 DSL 样例不能因为只支持内部表达式而被拒绝。
     */
    @Test
    void testSettlementPolicyShouldParseDocumentedDslSamples() {
        SettlementPolicySpec realtime = SettlementPolicySpec.parse("RT");
        SettlementPolicySpec delay = SettlementPolicySpec.parse("T+1");
        SettlementPolicySpec dailyAt = SettlementPolicySpec.parse("D@23:00");
        SettlementPolicySpec weekly = SettlementPolicySpec.parse("W@MON");
        SettlementPolicySpec monthly = SettlementPolicySpec.parse("M@15");
        SettlementPolicySpec quarterly = SettlementPolicySpec.parse("Q@03-31");
        SettlementPolicySpec yearly = SettlementPolicySpec.parse("Y@01-01");
        SettlementPolicySpec customRange = SettlementPolicySpec.parse("C@contract-2026-05-16_2026-06-15");

        assertThat(realtime.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.REALTIME);
        assertThat(delay.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.DELAY_DAYS);
        assertThat(delay.getInterval()).isEqualTo(1);
        assertThat(dailyAt.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.DAILY_AT);
        assertThat(dailyAt.nextSettlementTime(BASE_TIME)).isEqualTo(LocalDateTime.of(2026, 5, 20, 23, 0));
        assertThat(weekly.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.WEEKLY);
        assertThat(weekly.getParam()).isEqualTo(1);
        assertThat(monthly.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.MONTHLY);
        assertThat(monthly.getParam()).isEqualTo(15);
        assertThat(quarterly.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.QUARTERLY);
        assertThat(quarterly.nextSettlementTime(BASE_TIME)).isEqualTo(LocalDateTime.of(2026, 6, 30, 0, 0));
        assertThat(yearly.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.YEARLY);
        assertThat(yearly.getParam()).isEqualTo(1);
        assertThat(yearly.getEndParam()).isEqualTo(1);
        assertThat(customRange.getSettlementMode()).isEqualTo(SettlementPolicySpec.SettlementMode.CUSTOM_RANGE);
        assertThat(customRange.getRangeId()).isEqualTo("contract-2026-05-16_2026-06-15");
    }

    /**
     * 场景：结算策略表达式非法或无法识别。
     * 预期：解析必须显式失败。
     * 红线：空表达式、未知策略或非法时间不能静默降级成实时结算。
     */
    @Test
    void testSettlementPolicyShouldRejectInvalidExpressions() {
        List.of("", " ", "UNKNOWN", "T+X", "D@25:00", "C@").forEach(expression ->
                assertThatThrownBy(() -> SettlementPolicySpec.parse(expression))
                        .as(expression)
                        .isInstanceOf(IllegalArgumentException.class));
    }

    /**
     * 场景：自定义 range 策略只负责表达外部账期引用。
     * 预期：解析能保留 rangeId，但不能在缺少外部账期日历时自行计算候选结算时间。
     * 红线：自定义 range 不能被伪装成实时结算或固定日期账期。
     */
    @Test
    void testCustomRangePolicyShouldRequireExternalRangeResolution() {
        SettlementPolicySpec customRange = SettlementPolicySpec.parse("C@contract-2026-05-16_2026-06-15");

        assertThatThrownBy(() -> customRange.nextSettlementTime(BASE_TIME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external range");
    }
}
