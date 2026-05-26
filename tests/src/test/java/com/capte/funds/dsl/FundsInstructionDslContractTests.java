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

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference) {
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
                .contextVariables(Map.of("dslCase", "B1-02"))
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
