package com.capte.funds.dsl;

import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金指令 DSL 契约测试。
 */
class FundsInstructionDslContractTests {

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    /**
     * 场景：业务侧把外部交易流水带入资金指令，进入路由和账本前形成稳定 DSL 事实。
     * 预期：资金指令保留交易语义、业务幂等号、操作者和可追溯引用。
     * 红线：资金指令不能丢失业务身份或引用定位能力。
     */
    @Test
    void testFundsInstructionShouldExposeStableDslFields() {
        FundsInstructionSpec instruction = validInstruction(externalTransactionReference());

        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.PAY);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
        assertThat(instruction.getAmount()).isEqualTo(Money.immutable(100L, CURRENCY));
        assertThat(instruction.getOriginalAmount()).isEqualTo(Money.immutable(100L, CURRENCY));
        assertThat(instruction.getExchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(instruction.getBusinessScene()).isEqualTo("FUNDS_INSTRUCTION_DSL");
        assertThat(instruction.getBusinessSn()).isEqualTo("BIZ-FI-001");
        assertThat(instruction.getOperator().getAppName()).isEqualTo("wind-funds-tests");
        assertThat(instruction.getReference()).isInstanceOfSatisfying(FundsInstructionReferenceSpec.class, reference -> {
            assertThat(reference.getReferenceType()).isEqualTo(FundsInstructionReferenceType.EXTERNAL_TRANSACTION);
            assertThat(reference.getExternalTransactionId()).isEqualTo("EXT-202605200001");
        });
    }

    /**
     * 场景：逆向、授权、冻结或外部交易指令传入引用对象，但未声明引用类型。
     * 预期：DSL 入口立即拒绝。
     * 红线：不可把无类型引用推迟到 route、账本或归档重放阶段再猜测语义。
     */
    @Test
    void testInstructionReferenceShouldRequireReferenceType() {
        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .externalTransactionId("EXT-202605200002")
                .build())
                .hasMessageContaining("fundsInstruction.referenceType must not be null");
    }

    /**
     * 场景：引用对象有类型，但原交易、业务单、账本交易、外部交易和授权码均为空。
     * 预期：DSL 入口立即拒绝。
     * 红线：资金引用必须可追溯，不能只靠 referenceType 形成不可定位的关联事实。
     */
    @Test
    void testInstructionReferenceShouldRequireTraceableIdentifier() {
        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn(" ")
                .referenceBusinessSn(" ")
                .referenceLedgerTransactionSn(" ")
                .externalTransactionId(" ")
                .authCode(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.reference identifier is required");
    }

    /**
     * 场景：外部支付通道只返回外部交易流水，平台交易流水尚未生成或不可作为关联键。
     * 预期：引用对象允许 externalTransactionId 单独承担追溯入口。
     * 红线：不能为了强制 referenceSn 破坏外部交易接入场景。
     */
    @Test
    void testInstructionReferenceShouldAcceptExternalTransactionIdentifierOnly() {
        FundsInstructionReferenceSpec reference = externalTransactionReference();

        assertThat(reference.getReferenceSn()).isNull();
        assertThat(reference.getExternalTransactionId()).isEqualTo("EXT-202605200001");
        assertThat(reference.getContextVariables()).containsEntry("channel", "test-channel");
    }

    /**
     * 场景：外部交易引用上下文被调用方塞入通道密钥或外部账户原文。
     * 预期：资金指令引用 DSL 构造期立即拒绝。
     * 红线：资金指令引用会进入交易生命周期、回放和审计链路，不能保存完整卡号、密钥或银行账户原文。
     */
    @Test
    void testInstructionReferenceShouldRejectSensitiveContextVariables() {
        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-SENSITIVE-001")
                .contextVariables(Map.of("processorPayload", Map.of("secretKey", "secret-value")))
                .build())
                .hasMessageContaining("fundsInstruction.reference.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-SENSITIVE-002")
                .contextVariables(Map.of("externalAccount", Map.of("iban", "GB82WEST12345698765432")))
                .build())
                .hasMessageContaining("fundsInstruction.reference.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方把权益金额、资金责任或当前营销规则藏入资金指令引用上下文。
     * 预期：资金指令引用构造阶段显式失败，但仍允许只放权益快照引用和稳定摘要。
     * 红线：资金指令引用 contextVariables 不能绕过资金指令顶层守门承载权益核心事实。
     */
    @Test
    void testInstructionReferenceContextShouldRejectCoreBenefitFactsButAllowSummaryRefs() {
        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-BENEFIT-REF-001")
                .contextVariables(Map.of("benefitPayload", Map.of(
                        "amount", Money.immutable(2000L, CURRENCY),
                        "fundingNature", "PLATFORM_BORNE")))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.reference.contextVariables must not contain core benefit field");

        assertThatThrownBy(() -> ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-BENEFIT-REF-002")
                .contextVariables(Map.of("benefitDecisionTrace",
                        new Object[] {Map.of("currentMarketingRule", "latest-rule")}))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.reference.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-BENEFIT-REF-003")
                .contextVariables(Map.of(
                        "benefitSnapshotId", "BS-REFERENCE-SUMMARY-001",
                        "stableDigest", "sha256:fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
                        "benefitGroupSn", "BG-REFERENCE-SUMMARY-001",
                        "componentSn", "COMP-REFERENCE-SUMMARY-001",
                        "ruleVersion", "rule-v1",
                        "refundDecisionId", "refund-decision-001",
                        "externalDecisionId", "pricing-decision-001"))
                .build();

        assertThat(reference.getContextVariables())
                .containsEntry("benefitSnapshotId", "BS-REFERENCE-SUMMARY-001")
                .containsEntry("stableDigest",
                        "sha256:fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
                .containsEntry("benefitGroupSn", "BG-REFERENCE-SUMMARY-001")
                .containsEntry("componentSn", "COMP-REFERENCE-SUMMARY-001")
                .containsEntry("ruleVersion", "rule-v1")
                .containsEntry("refundDecisionId", "refund-decision-001")
                .containsEntry("externalDecisionId", "pricing-decision-001");
    }

    /**
     * 场景：调用方把操作者扩展上下文当作旁路，塞入通道密钥或外部账户原文。
     * 预期：资金 DSL 操作者快照构造期立即拒绝。
     * 红线：操作者上下文会随指令进入审计链路，不能保存完整卡号、CVV、密钥或银行账户原文。
     */
    @Test
    void testOperationActorShouldRejectSensitiveContextVariables() {
        assertThatThrownBy(() -> ImmutableFundsOperationActorSpec.builder()
                .operatorId(1L)
                .operatorType("SYSTEM")
                .operatorName("Codex")
                .appName("wind-funds-tests")
                .contextVariables(Map.of("processorPayload", Map.of("secretKey", "secret-value")))
                .build())
                .hasMessageContaining("fundsOperationActor.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> ImmutableFundsOperationActorSpec.builder()
                .operatorId(1L)
                .operatorType("SYSTEM")
                .operatorName("Codex")
                .appName("wind-funds-tests")
                .contextVariables(Map.of("externalAccount", Map.of("bankAccountNo", "123456789012")))
                .build())
                .hasMessageContaining("fundsOperationActor.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方绕过交易转换器，直接在资金指令扩展上下文塞入通道密钥或外部账户原文。
     * 预期：资金 DSL 指令构造期立即拒绝。
     * 红线：资金指令上下文会进入交易事实链路，不能依赖上游适配器单点拦截敏感值。
     */
    @Test
    void testFundsInstructionShouldRejectSensitiveContextVariables() {
        assertThatThrownBy(() -> ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(100L, CURRENCY))
                .originalAmount(Money.immutable(100L, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .reference(externalTransactionReference())
                .businessScene("FUNDS_INSTRUCTION_DSL")
                .businessSn("BIZ-FI-SENSITIVE-001")
                .eventTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .operatorName("Codex")
                        .appName("wind-funds-tests")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of("processorPayload", Map.of("cardSecurityCode", "123")))
                .build())
                .hasMessageContaining("fundsInstruction.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(100L, CURRENCY))
                .originalAmount(Money.immutable(100L, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .reference(externalTransactionReference())
                .businessScene("FUNDS_INSTRUCTION_DSL")
                .businessSn("BIZ-FI-SENSITIVE-002")
                .eventTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .operatorName("Codex")
                        .appName("wind-funds-tests")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432")))
                .build())
                .hasMessageContaining("fundsInstruction.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方把权益核心金额、资金责任或当前营销规则藏入资金指令顶层上下文。
     * 预期：资金指令构造阶段显式失败，但仍允许过渡期只放权益快照引用和稳定摘要。
     * 红线：资金指令 contextVariables 不能替代权益快照、route snapshot 或交易事实快照。
     */
    @Test
    void testFundsInstructionContextShouldRejectCoreBenefitFactsButAllowSummaryRefs() {
        assertThatThrownBy(() -> validInstruction(externalTransactionReference(),
                Map.of("benefitPayload", Map.of(
                        "amount", Money.immutable(2000L, CURRENCY),
                        "fundingNature", "MERCHANT_BORNE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.contextVariables must not contain core benefit field");

        assertThatThrownBy(() -> validInstruction(externalTransactionReference(),
                Map.of("benefitDecisionTrace",
                        new Object[] {Map.of("currentMarketingRule", "latest-rule")})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        FundsInstructionSpec instruction = validInstruction(externalTransactionReference(), Map.of(
                "benefitSnapshotId", "BS-CONTEXT-SUMMARY-001",
                "stableDigest", "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "ruleVersion", "rule-v1",
                "refundDecisionId", "refund-decision-001",
                "externalDecisionId", "pricing-decision-001"));

        assertThat(instruction.getContextVariables())
                .containsEntry("benefitSnapshotId", "BS-CONTEXT-SUMMARY-001")
                .containsEntry("stableDigest",
                        "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .containsEntry("ruleVersion", "rule-v1")
                .containsEntry("refundDecisionId", "refund-decision-001")
                .containsEntry("externalDecisionId", "pricing-decision-001");
    }

    /**
     * 场景：调用方在构造资金指令后继续改写原始嵌套上下文。
     * 预期：已构造的资金指令上下文保持稳定，不被追加的敏感字段污染。
     * 红线：资金指令是交易事实入口，不能因浅拷贝让后续可变对象绕过构造期敏感字段校验。
     */
    @Test
    void testFundsInstructionShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "FT2026052714000062");
        FundsInstructionSpec instruction = validInstruction(externalTransactionReference(),
                Map.of("processorPayload", processorPayload));

        processorPayload.put("secretKey", "secret-value");

        Object payloadValue = instruction.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("FT2026052714000062");
        assertThat(payload.containsKey("secretKey")).isFalse();
    }

    /**
     * 场景：调用方在构造资金指令引用后继续改写原始嵌套上下文。
     * 预期：已构造的引用上下文保持稳定，不被追加的敏感字段污染。
     * 红线：资金追溯引用不能因浅拷贝让后续可变对象绕过构造期敏感字段校验。
     */
    @Test
    void testInstructionReferenceShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> channelPayload = new HashMap<>();
        channelPayload.put("channelTraceId", "CH-TRACE-202605270001");
        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-202605270001")
                .contextVariables(Map.of("channelPayload", channelPayload))
                .build();

        channelPayload.put("bankAccountNo", "123456789012");

        Object payloadValue = reference.getContextVariables().get("channelPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("channelTraceId")).isEqualTo("CH-TRACE-202605270001");
        assertThat(payload.containsKey("bankAccountNo")).isFalse();
    }

    /**
     * 场景：调用方在构造资金操作人快照后继续改写原始嵌套上下文。
     * 预期：已构造的操作者上下文保持稳定，不被追加的敏感字段污染。
     * 红线：审计操作者快照不能因浅拷贝让后续可变对象绕过构造期敏感字段校验。
     */
    @Test
    void testOperationActorShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("requestId", "REQ-202605270001");
        ImmutableFundsOperationActorSpec actor = ImmutableFundsOperationActorSpec.builder()
                .operatorId(1L)
                .operatorType("SYSTEM")
                .operatorName("Codex")
                .appName("wind-funds-tests")
                .contextVariables(Map.of("auditPayload", auditPayload))
                .build();

        auditPayload.put("secretKey", "secret-value");

        Object payloadValue = actor.getContextVariables().get("auditPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("requestId")).isEqualTo("REQ-202605270001");
        assertThat(payload.containsKey("secretKey")).isFalse();
    }

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference) {
        return validInstruction(reference, Map.of("dslCase", "B1-02"));
    }

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference,
                                                  Map<String, Object> contextVariables) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(100L, CURRENCY))
                .originalAmount(Money.immutable(100L, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .reference(reference)
                .businessScene("FUNDS_INSTRUCTION_DSL")
                .businessSn("BIZ-FI-001")
                .eventTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .operatorName("Codex")
                        .appName("wind-funds-tests")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(contextVariables)
                .build();
    }

    private FundsInstructionReferenceSpec externalTransactionReference() {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-202605200001")
                .contextVariables(Map.of("channel", "test-channel"))
                .build();
    }
}
