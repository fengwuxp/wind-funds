package com.wind.funds.dsl;

import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.support.WindOperatorTestFixture;
import com.wind.integration.operator.WindOperator;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金指令 DSL 契约测试。
 */
class FundsInstructionDslContractTests {

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    /**
     * 场景：余额冻结和解冻需要与余额调整形成两个稳定动作族。
     * 预期：交易类型公开 BALANCE_CONTROL，供冻结和解冻指令归类。
     * 红线：冻结、解冻不能继续伪装成人工或系统调账。
     */
    @Test
    void testTransactionTypeShouldExposeBalanceControlActionFamily() {
        DefaultFundsTransactionType transactionType = DefaultFundsTransactionType.valueOf("BALANCE_CONTROL");

        assertThat(transactionType.getDesc()).isEqualTo("余额控制");
    }

    /**
     * 场景：调用方尝试从粗粒度交易类型推导精确生命周期事件。
     * 预期：旧方法仅作为待移除的兼容桥保留，精确事件仍由 eventType 显式表达。
     * 红线：不得破坏已发布 Core API，也不得把兼容字符串当成唯一事件事实。
     */
    @Test
    void testLegacyEventTypeProjectionShouldRemainDeprecatedDuringMigration() throws NoSuchMethodException {
        Deprecated annotation = DefaultFundsTransactionType.class.getDeclaredMethod("asEventType")
                .getAnnotation(Deprecated.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.forRemoval()).isTrue();
        assertThat(DefaultFundsTransactionType.BALANCE_CONTROL.asEventType()).isEqualTo("balance.control");
    }

    /**
     * 场景：调用方组合了直接交易处理模型、支付事件和退款动作族。
     * 预期：资金指令构造阶段立即拒绝不合法三元组。
     * 红线：非法组合不能进入 route、账本或生命周期持久化阶段。
     */
    @Test
    void testFundsInstructionShouldRejectIllegalSemanticCombination() {
        assertThatThrownBy(() -> instruction(FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.PAY,
                DefaultFundsTransactionType.REFUND))
                .hasMessageContaining("instructionType/eventType/transactionType combination is invalid");
    }

    /**
     * 场景：公共资金能力按当前已实现的转换器和路由构造全部合法指令三元组。
     * 预期：19 组 canonical 组合均可进入后续路由解析。
     * 红线：跨主体调账和 PAYOUT_RETURNED 尚未闭合，不能混入当前白名单。
     */
    @Test
    void testFundsInstructionShouldAcceptCanonicalSemanticCombinations() {
        DefaultFundsTransactionType balanceControl = DefaultFundsTransactionType.valueOf("BALANCE_CONTROL");
        List<InstructionCombination> combinations = List.of(
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.TOPUP,
                        DefaultFundsTransactionType.TOPUP),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.TRANSFER,
                        DefaultFundsTransactionType.TRANSFER),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAY,
                        DefaultFundsTransactionType.PAY),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.REFUND,
                        DefaultFundsTransactionType.REFUND),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.WITHDRAW,
                        DefaultFundsTransactionType.WITHDRAW),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.FEE_CHARGE,
                        DefaultFundsTransactionType.FEE),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.FEE_REFUND,
                        DefaultFundsTransactionType.REFUND),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.CLEARING_CONFIRM,
                        DefaultFundsTransactionType.CLEARING),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.SETTLEMENT_LOCK,
                        DefaultFundsTransactionType.SETTLEMENT),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAYOUT_SUCCEEDED,
                        DefaultFundsTransactionType.PAYOUT),
                combination(FundsInstructionType.DIRECT_TRANSACTION, FundsTransactionEventType.PAYOUT_FAILED,
                        DefaultFundsTransactionType.PAYOUT),
                combination(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.AUTHORIZE,
                        DefaultFundsTransactionType.PAY),
                combination(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.REVERSAL,
                        DefaultFundsTransactionType.PAY),
                combination(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.COMPLETE,
                        DefaultFundsTransactionType.PAY),
                combination(FundsInstructionType.AUTHORIZATION_TRANSACTION, FundsTransactionEventType.AUTH_REFUND,
                        DefaultFundsTransactionType.REFUND),
                combination(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.FREEZE, balanceControl),
                combination(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.UNFREEZE, balanceControl),
                combination(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.BALANCE_ADJUST,
                        DefaultFundsTransactionType.ADJUSTMENT),
                combination(FundsInstructionType.BALANCE_CONTROL, FundsTransactionEventType.LIMIT_ADJUST,
                        DefaultFundsTransactionType.ADJUSTMENT));

        assertThat(combinations)
                .allSatisfy(combination -> assertThat(instruction(combination.instructionType(),
                        combination.eventType(), combination.transactionType())).isNotNull());
    }

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
        assertThat(instruction.getReference()).isInstanceOfSatisfying(FundsInstructionReferenceSpec.class, reference -> {
            assertThat(reference.getReferenceType()).isEqualTo(FundsInstructionReferenceType.EXTERNAL_TRANSACTION);
            assertThat(reference.getExternalTransactionId()).isEqualTo("EXT-202605200001");
        });
    }

    /**
     * 场景：应用服务把统一的 WindOperator 传入资金指令。
     * 预期：资金指令直接持有当前运行时操作者，不创建平行快照类型。
     * 红线：资金 Core 不得复制或降级操作者语义。
     */
    @Test
    void testFundsInstructionShouldReuseWindOperator() {
        WindOperator operator = WindOperatorTestFixture.system();

        FundsInstructionSpec instruction = validInstruction(externalTransactionReference(), Map.of(), operator);

        assertThat(instruction.getOperator()).isSameAs(operator);
    }

    /**
     * 场景：资金指令携带会影响 route、posting 或账本周期的账户类输入。
     * 预期：账户、对手方、账目和账本周期必须是 typed DSL 字段，不能藏在 contextVariables。
     * 红线：contextVariables 只能做证据和解释补充，不能成为第二套资金指令模型。
     */
    @Test
    void testFundsInstructionShouldExposeRouteInputsAsTypedFields() {
        FundsAccountId accountId = FundsAccountId.immutable("ACC-001", "FUNDING_ACCOUNT");
        FundsAccountId payerAccountId = FundsAccountId.immutable("PAYER-001", "FUNDING_ACCOUNT");
        FundsAccountId payeeAccountId = FundsAccountId.immutable("PAYEE-001", "FUNDING_ACCOUNT");
        FundsAccountId payerId = FundsAccountId.immutable("PAYER-LEDGER-001", "FUNDING_ACCOUNT");
        FundsAccountId payeeId = FundsAccountId.immutable("PAYEE-LEDGER-001", "FUNDING_ACCOUNT");
        FundsAccountId linkedFundingAccountId = FundsAccountId.immutable("PARENT-001", "FUNDING_ACCOUNT");

        FundsInstructionSpec instruction = ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTHORIZE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(Money.immutable(100L, CURRENCY))
                .originalAmount(Money.immutable(100L, CURRENCY))
                .exchangeRate(BigDecimal.ONE)
                .accountId(accountId)
                .payerAccountId(payerAccountId)
                .payeeAccountId(payeeAccountId)
                .payerId(payerId)
                .payeeId(payeeId)
                .payerLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .payeeLedgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                .linkedFundingAccountId(linkedFundingAccountId)
                .ledgerPeriodType(AccountBalancePeriodType.MONTHLY)
                .ledgerPeriodId("2026-06")
                .reference(externalTransactionReference())
                .businessScene("FUNDS_INSTRUCTION_DSL")
                .businessSn("BIZ-FI-ROUTE-INPUT-001")
                .eventTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of("evidenceRef", "EV-001"))
                .build();

        assertThat(instruction.getAccountId()).isEqualTo(accountId);
        assertThat(instruction.getPayerAccountId()).isEqualTo(payerAccountId);
        assertThat(instruction.getPayeeAccountId()).isEqualTo(payeeAccountId);
        assertThat(instruction.getPayerId()).isEqualTo(payerId);
        assertThat(instruction.getPayeeId()).isEqualTo(payeeId);
        assertThat(instruction.getPayerLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        assertThat(instruction.getPayeeLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.SETTLEMENT);
        assertThat(instruction.getLinkedFundingAccountId()).isEqualTo(linkedFundingAccountId);
        assertThat(instruction.getLedgerPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(instruction.getLedgerPeriodId()).isEqualTo("2026-06");
        assertThat(instruction.getContextVariables()).containsOnly(Map.entry("evidenceRef", "EV-001"));
    }

    /**
     * 场景：调用方把影响 route 或账本周期的关键字段塞入扩展上下文。
     * 预期：资金指令构造阶段显式失败。
     * 红线：不能用 contextVariables 绕过 typed DSL 字段和资金事实审计。
     */
    @Test
    void testFundsInstructionContextShouldRejectRouteInputFields() {
        assertThatThrownBy(() -> validInstruction(externalTransactionReference(),
                Map.of("accountId", FundsAccountId.immutable("ACC-CTX-001", "FUNDING_ACCOUNT"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.contextVariables must not contain core instruction field: "
                        + "accountId");

        assertThatThrownBy(() -> validInstruction(externalTransactionReference(),
                Map.of("ledgerPeriodType", AccountBalancePeriodType.MONTHLY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundsInstruction.contextVariables must not contain core instruction field: "
                        + "ledgerPeriodType");
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
                        "ruleVersion", "rule-v1",
                        "refundDecisionId", "refund-decision-001",
                        "externalDecisionId", "pricing-decision-001"))
                .build();

        assertThat(reference.getContextVariables())
                .containsEntry("benefitSnapshotId", "BS-REFERENCE-SUMMARY-001")
                .containsEntry("stableDigest",
                        "sha256:fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210")
                .containsEntry("ruleVersion", "rule-v1")
                .containsEntry("refundDecisionId", "refund-decision-001")
                .containsEntry("externalDecisionId", "pricing-decision-001");
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
                .operator(WindOperatorTestFixture.system())
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
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432")))
                .build())
                .hasMessageContaining("fundsInstruction.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：授权撤销、结算、退款或拒付指令在上下文中携带原授权交易号。
     * 预期：内部授权交易号即使形似有效 IBAN，也不能被误判为外部账户原文。
     * 红线：敏感值校验不得阻断资金交易内部引用链路。
     */
    @Test
    void testFundsInstructionShouldAllowInternalAuthorizationTransactionReference() {
        FundsInstructionSpec instruction = validInstruction(externalTransactionReference(),
                Map.of("authorizationTransactionSn", "FT2026052812000061"));

        assertThat(instruction.getContextVariables())
                .containsEntry("authorizationTransactionSn", "FT2026052812000061");
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

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference) {
        return validInstruction(reference, Map.of("dslCase", "B1-02"));
    }

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference,
                                                  Map<String, Object> contextVariables) {
        return validInstruction(reference, contextVariables, WindOperatorTestFixture.system());
    }

    private FundsInstructionSpec validInstruction(FundsInstructionReferenceSpec reference,
                                                   Map<String, Object> contextVariables,
                                                   WindOperator operator) {
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
                .operator(operator)
                .contextVariables(contextVariables)
                .build();
    }

    private FundsInstructionSpec instruction(FundsInstructionType instructionType,
                                             FundsTransactionEventType eventType,
                                             DefaultFundsTransactionType transactionType) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(transactionType)
                .amount(Money.immutable(100L, CURRENCY))
                .businessScene("FUNDS_INSTRUCTION_SEMANTIC_CONTRACT")
                .businessSn("BIZ-FI-SEMANTIC-001")
                .eventTime(LocalDateTime.of(2026, 7, 30, 16, 0))
                .operator(WindOperatorTestFixture.system())
                .contextVariables(Map.of())
                .build();
    }

    private InstructionCombination combination(FundsInstructionType instructionType,
                                               FundsTransactionEventType eventType,
                                               DefaultFundsTransactionType transactionType) {
        return new InstructionCombination(instructionType, eventType, transactionType);
    }

    private record InstructionCombination(FundsInstructionType instructionType,
                                          FundsTransactionEventType eventType,
                                          DefaultFundsTransactionType transactionType) {
    }

    private FundsInstructionReferenceSpec externalTransactionReference() {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId("EXT-202605200001")
                .contextVariables(Map.of("channel", "test-channel"))
                .build();
    }
}
