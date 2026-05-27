package com.capte.funds.dsl;

import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感扩展上下文校验器契约测试。
 */
class SensitiveContextVariablesValidatorTests {

    /**
     * 场景：调用方提交畸形 JSON，字段名中仍包含 token secret 或外部账户原文字段。
     * 预期：校验器按原始片段兜底识别敏感字段。
     * 红线：JSON 解析失败不能成为敏感上下文绕过普通快照、日志或报表链路的入口。
     */
    @Test
    void testMalformedContextVariablesShouldStillRejectSensitiveFieldNames() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"secretKey\":\"secret-value\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"externalAccount\":{\"bankAccountNo\":\"123456789012\""))
                .isTrue();
    }

    /**
     * 场景：调用方提交畸形 JSON，字段名看似普通但值中包含完整 PAN 或有效 IBAN。
     * 预期：校验器按原始片段兜底识别敏感值。
     * 红线：敏感值识别不能只依赖可正常反序列化的对象树。
     */
    @Test
    void testMalformedContextVariablesShouldStillRejectSensitiveValues() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"networkReference\":\"4242424242424242\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"networkReference\":\"GB82WEST12345698765432\""))
                .isTrue();
    }

    /**
     * 场景：调用方把数组值放入 Map 型 contextVariables。
     * 预期：校验器递归进入数组元素，识别隐藏在普通字段下的 PAN 或 IBAN。
     * 红线：DSL Map 上下文不能因为 Java 数组不属于 Iterable 而绕过敏感值治理。
     */
    @Test
    void testMapContextVariablesShouldRejectSensitiveArrayValues() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(
                java.util.Map.of("processorPayload", new String[] {"token-ref", "4242424242424242"})))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("processorPayload", new String[] {"token-ref", "GB82WEST12345698765432"})))
                .isTrue();
    }

    /**
     * 场景：调用方提交畸形 JSON，但内容不包含敏感字段名或敏感值。
     * 预期：校验器不把所有解析失败都归类为敏感。
     * 红线：敏感值治理要精准阻断旁路存储，不能无差别拒绝无敏感信息的扩展文本。
     */
    @Test
    void testMalformedContextVariablesWithoutSensitiveFragmentsShouldRemainAllowed() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"networkReference\":\"FT2026052714000062\""))
                .isFalse();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"networkReference\":\"FT2026052714000062\""))
                .isFalse();
    }
}
