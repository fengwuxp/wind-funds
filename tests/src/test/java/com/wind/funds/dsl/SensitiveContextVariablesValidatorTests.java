package com.wind.funds.dsl;

import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
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
     * 场景：调用方提交畸形 JSON，敏感字段名使用大小写、空格或短横线变体。
     * 预期：校验器兜底扫描原始字段名时仍执行统一归一化。
     * 红线：敏感字段治理不能只在可正常解析的对象树中成立。
     */
    @Test
    void testMalformedContextVariablesShouldRejectNormalizedSensitiveFieldNameVariants() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"Token Secret\":\"token-ref\""))
                .isTrue();
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"card-no\":\"token-ref\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"routing-number\":\"bank-ref\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{\"processorPayload\":{\"Account Number\":\"bank-ref\""))
                .isTrue();
    }

    /**
     * 场景：调用方提交畸形 JSON，敏感字段名未按 JSON 规范加双引号。
     * 预期：校验器兜底扫描类 JSON 字段名时仍识别敏感字段。
     * 红线：坏 JSON 不能成为 token secret、卡号字段或外部账户字段旁路存储入口。
     */
    @Test
    void testMalformedContextVariablesShouldRejectUnquotedSensitiveFieldNames() {
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{processorPayload:{secretKey:\"secret-value\""))
                .isTrue();
        assertThat(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(
                "{processorPayload:{card-no:\"token-ref\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{externalAccount:{bankAccountNo:\"123456789012\""))
                .isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(
                "{processorPayload:{routing-number:\"bank-ref\""))
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
     * 场景：内部交易引用偶然满足 IBAN 校验规则。
     * 预期：仅内部引用字段允许对应稳定引用，普通字段下仍按敏感值拒绝。
     * 红线：内部引用白名单不得放宽其他扩展字段的外部账号检测。
     */
    @Test
    void testInternalReferenceSnShouldNotBeMisclassifiedAsExternalAccountNumber() {
        String sourceSn = "FT2000000000000027";

        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("sourceSn", sourceSn))).isFalse();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("referenceFreezeSn", sourceSn))).isFalse();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("networkReference", sourceSn))).isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("sourceSn", "GB82WEST12345698765432"))).isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("authorizationTransactionSn", "GB82WEST12345698765432"))).isTrue();
        assertThat(ExternalAccountSensitiveValueValidator.containsSensitiveContextField(
                java.util.Map.of("referenceFreezeSn", "GB82WEST12345698765432"))).isTrue();
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
