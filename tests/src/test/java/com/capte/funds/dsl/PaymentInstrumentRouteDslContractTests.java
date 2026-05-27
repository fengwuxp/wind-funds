package com.capte.funds.dsl;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentInstrument Route DSL 契约测试。
 */
class PaymentInstrumentRouteDslContractTests {

    /**
     * 场景：支付工具付款已完成路由解析。
     * 预期：工具和外部账户只能作为快照引用，route leg 的账务节点只能是内部可记账主体。
     * 红线：支付工具 ID、外部账户或通道 token 不得被写成 LedgerEntry 主体。
     */
    @Test
    void testPaymentInstrumentAndExternalAccountShouldNotBecomeLedgerRouteNodes() {
        SubjectRef payer = fundingAccount("FA-PAYER-001");
        SubjectRef payee = fundingAccount("FA-PAYEE-001");
        RouteLegSpec leg = routeLeg(payer, payee);
        PaymentInstrumentRefSpec instrumentRef = paymentInstrumentRef("PI-001", "**** 4242");
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-001", "acct_****_0422");

        assertThat(leg.getSourceNode().getNodeType()).isEqualTo(RouteNodeType.SUBJECT);
        assertThat(leg.getTargetNode().getNodeType()).isEqualTo(RouteNodeType.SUBJECT);
        assertThat(leg.getSourceNode().getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(leg.getTargetNode().getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(instrumentRef.getInstrumentId()).isEqualTo("PI-001");
        assertThat(externalAccountRef.getExternalAccountId()).isEqualTo("EA-001");
    }

    /**
     * 场景：支付工具、绑定关系和资金来源共同决定 route。
     * 预期：RoutingDecision 必须保留命中规则、资金来源、优先级和选择原因。
     * 红线：缺资金来源或选择原因的 route snapshot 不能解释后续回放和审计。
     */
    @Test
    void testRoutingDecisionShouldRecordFundingAllocationPriorityAndReason() {
        FundingAllocationDecisionSpec allocation = fundingAllocation("ALLOC-001",
                fundingAccount("FA-PAYER-001"),
                LedgerSubjectCode.AVAILABLE,
                10,
                "DEFAULT_PAYMENT_INSTRUMENT");

        RoutingDecisionSpec decision = ImmutableRoutingDecisionSpec.builder()
                .policyCode("PAYMENT_INSTRUMENT_ROUTE")
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY", "UNIQUE_FUNDING_SOURCE"))
                .selectedProcessor("CARD_PROCESSOR")
                .fundingAllocations(List.of(allocation))
                .decisionReason("ACTIVE_CARD_WITH_DEFAULT_FUNDING_ACCOUNT")
                .contextVariables(Map.of("bindingVersion", 3))
                .build();

        assertThat(decision.getPolicyCode()).isEqualTo("PAYMENT_INSTRUMENT_ROUTE");
        assertThat(decision.getMatchedRules()).containsExactly("INSTRUMENT_ACTIVE", "DIRECTION_PAY", "UNIQUE_FUNDING_SOURCE");
        assertThat(decision.getDecisionReason()).isEqualTo("ACTIVE_CARD_WITH_DEFAULT_FUNDING_ACCOUNT");
        assertThat(decision.getFundingAllocations()).singleElement().satisfies(item -> {
            assertThat(item.getSubjectRef().getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
            assertThat(item.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(item.getPriority()).isEqualTo(10);
            assertThat(item.getReason()).isEqualTo("DEFAULT_PAYMENT_INSTRUMENT");
        });
    }

    /**
     * 场景：授权组合场景使用资金账户、共享卡 + 资金账户、共享卡 + 预算组 + 资金账户三种模型。
     * 预期：RoutingDecision 能分别表达真实资金来源、工具快照和预算额度控制维度。
     * 红线：共享卡不得替代真实资金账户；预算组不得成为唯一真实资金来源。
     */
    @Test
    void testRoutingDecisionShouldCoverRequiredFundingSourceModels() {
        RoutingDecisionSpec fundingAccountOnly = routingDecision("FUNDING_ACCOUNT_ONLY",
                List.of(fundingAllocation("ALLOC-FA",
                        fundingAccount("FA-AUTH-001"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")));
        PaymentInstrumentRefSpec sharedCard = paymentInstrumentRef("PI-SHARED-001",
                "**** 1888",
                Map.of("bindingRole", "SHARED_CARD", "bindingVersion", 5));
        RoutingDecisionSpec sharedCardFundingAccount = routingDecision("SHARED_CARD_FUNDING_ACCOUNT",
                List.of(fundingAllocation("ALLOC-SHARED-FA",
                        fundingAccount("FA-AUTH-002"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "SHARED_CARD_REAL_FUNDING_ACCOUNT")));
        RoutingDecisionSpec sharedCardBudgetGroupFundingAccount = routingDecision("SHARED_CARD_BUDGET_GROUP_FUNDING_ACCOUNT",
                List.of(fundingAllocation("ALLOC-BUDGET",
                                budgetGroup("BG-AUTH-001"),
                                LedgerSubjectCode.AVAILABLE,
                                10,
                                "BUDGET_CONTROL"),
                        fundingAllocation("ALLOC-BUDGET-FA",
                                fundingAccount("FA-AUTH-003"),
                                LedgerSubjectCode.AVAILABLE,
                                20,
                                "REAL_FUNDING_ACCOUNT")));

        assertThat(fundingAccountOnly.getFundingAllocations())
                .extracting(item -> item.getSubjectRef().getSubjectType())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(sharedCard.getBindingSnapshot()).containsEntry("bindingRole", "SHARED_CARD");
        assertThat(sharedCardFundingAccount.getFundingAllocations())
                .extracting(item -> item.getSubjectRef().getSubjectType())
                .containsExactly(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(sharedCardBudgetGroupFundingAccount.getFundingAllocations())
                .extracting(item -> item.getSubjectRef().getSubjectType())
                .containsExactly(FundsSubjectType.BUDGET_GROUP, FundsSubjectType.FUNDING_ACCOUNT);
    }

    /**
     * 场景：支付工具命中多条资金来源规则但没有确定资金来源。
     * 预期：RoutingDecision 构造期拒绝缺失资金来源。
     * 红线：缺资金来源仍生成 route 会让后续回放和审计无法解释。
     */
    @Test
    void testRoutingDecisionShouldRejectMissingFundingAllocation() {
        assertThatThrownBy(() -> routingDecision("MISSING_FUNDING_SOURCE", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fundingAllocations must not be empty");
    }

    /**
     * 场景：支付工具路由决策上下文被调用方塞入通道密钥或支付工具原文。
     * 预期：RoutingDecision 构造期立即拒绝。
     * 红线：路由决策会进入 route snapshot、归档重放和审计链路，不能保存 PAN、CVV、密钥或外部账户原文。
     */
    @Test
    void testRoutingDecisionContextVariablesShouldRejectSensitiveValues() {
        assertThatThrownBy(() -> routingDecision("SENSITIVE_ROUTING_CONTEXT",
                List.of(fundingAllocation("ALLOC-SENSITIVE-CONTEXT",
                        fundingAccount("FA-SENSITIVE-CONTEXT"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")),
                Map.of("processorPayload", Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("routingDecision.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方在 RoutingDecision 构造后继续改写原始嵌套上下文。
     * 预期：已构造的路由决策保持稳定，不被追加的支付工具原文污染。
     * 红线：路由决策不能因浅拷贝绕过敏感字段校验并污染后续 route snapshot。
     */
    @Test
    void testRoutingDecisionShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:route-decision-001");
        RoutingDecisionSpec decision = routingDecision("IMMUTABLE_ROUTING_CONTEXT",
                List.of(fundingAllocation("ALLOC-IMMUTABLE-CONTEXT",
                        fundingAccount("FA-IMMUTABLE-CONTEXT"),
                        LedgerSubjectCode.AVAILABLE,
                        10,
                        "REAL_FUNDING_ACCOUNT")),
                Map.of("processorPayload", processorPayload));

        processorPayload.put("pan", "4242424242424242");

        Object payloadValue = decision.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:route-decision-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：支付工具资金来源存在多个候选，但优先级缺失或冲突。
     * 预期：FundingAllocation 必须有确定优先级，RoutingDecision 不允许重复优先级。
     * 红线：多来源命中不得随机选路。
     */
    @Test
    void testRoutingDecisionShouldRejectMissingOrDuplicateFundingPriority() {
        assertThatThrownBy(() -> fundingAllocation("ALLOC-NO-PRIORITY",
                fundingAccount("FA-NO-PRIORITY"),
                LedgerSubjectCode.AVAILABLE,
                null,
                "REAL_FUNDING_ACCOUNT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation priority is required");

        assertThatThrownBy(() -> routingDecision("DUPLICATE_PRIORITY",
                List.of(fundingAllocation("ALLOC-FA-01",
                                fundingAccount("FA-DUP-001"),
                                LedgerSubjectCode.AVAILABLE,
                                10,
                                "REAL_FUNDING_ACCOUNT"),
                        fundingAllocation("ALLOC-FA-02",
                                budgetGroup("BG-DUP-001"),
                                LedgerSubjectCode.AVAILABLE,
                                10,
                                "BUDGET_CONTROL"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation priority must be unique");
    }

    /**
     * 场景：资金来源决策只有主体和金额，没有选择原因。
     * 预期：FundingAllocation 构造期拒绝缺失原因。
     * 红线：缺少选择原因的资金来源不能支撑争议、对账和回放解释。
     */
    @Test
    void testFundingAllocationShouldRejectMissingReason() {
        assertThatThrownBy(() -> fundingAllocation("ALLOC-NO-REASON",
                fundingAccount("FA-NO-REASON"),
                LedgerSubjectCode.AVAILABLE,
                10,
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("funding allocation reason is required");
    }

    /**
     * 场景：支付工具换绑后发起退款、撤销、退费或拒付回放。
     * 预期：原 route leg 明确声明按原路径回放，不能按当前绑定重新选路。
     * 红线：工具换绑、默认资金来源变化后，逆向资金路径不得漂移。
     */
    @Test
    void testPaymentInstrumentReplayShouldDeclareOriginalRouteReplayPolicy() {
        RouteLegSpec originalLeg = routeLeg(fundingAccount("FA-PAYER-001"), fundingAccount("FA-PAYEE-001"));
        RouteLegSpec replayLeg = ImmutableRouteLegSpec.builder()
                .legId("REFUND-PAY")
                .sequence(1)
                .legType(RouteLegType.RESTORE)
                .sourceNode(originalLeg.getTargetNode())
                .targetNode(originalLeg.getSourceNode())
                .amount(originalLeg.getAmount())
                .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .replayRefLegId(originalLeg.getLegId())
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();

        assertThat(replayLeg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.FULL_ONLY);
        assertThat(replayLeg.getReplayRefLegId()).isEqualTo("PAY");
        assertThat(replayLeg.getSourceNode()).isSameAs(originalLeg.getTargetNode());
        assertThat(replayLeg.getTargetNode()).isSameAs(originalLeg.getSourceNode());
    }

    /**
     * 场景：业务侧错误地把完整卡号放入支付工具快照。
     * 预期：构造期拒绝明显敏感原文。
     * 红线：完整 PAN、CVV、密钥、token secret 不得进入普通快照、日志、导出、报表或测试数据。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectRawPanLikeNumber() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-RAW", "4242424242424242"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instrumentNo must be masked or token reference");
    }

    /**
     * 场景：业务侧错误地把 CVV 或 token secret 放入绑定快照。
     * 预期：构造期拒绝敏感字段名。
     * 红线：绑定快照会进入 route snapshot、日志、导出和报表，不能承载支付工具敏感原文。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectSensitiveBindingSnapshotFields() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CVV", "**** 4242", Map.of("cvv", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CARD-NO",
                "**** 4242",
                Map.of("cardNumber", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-TOKEN-SECRET",
                "tok_card_001",
                Map.of("token_secret", "secret-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-SECRET",
                "tok_card_002",
                Map.<String, Object>of("processorPayload", Map.of("secretKey", "secret-value"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-PAN",
                "tok_card_003",
                Map.<String, Object>of("processorPayload", Map.of("pan", "4242424242424242"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-NESTED-PAN-VALUE",
                "tok_card_004",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "4242424242424242"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
    }

    /**
     * 场景：通道回传字段名可能携带大小写、空格、短横线或下划线。
     * 预期：字段名归一化后仍按敏感字段阻断。
     * 红线：不能因字段命名风格差异让完整卡号、token secret 或密钥进入绑定快照。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldRejectNormalizedSensitiveBindingSnapshotFields() {
        assertThatThrownBy(() -> paymentInstrumentRef("PI-CARD-NO-STYLE",
                "**** 4242",
                Map.of("card-no", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-PRIMARY-ACCOUNT-STYLE",
                "**** 4242",
                Map.of("PRIMARY ACCOUNT NUMBER", "4242424242424242")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
        assertThatThrownBy(() -> paymentInstrumentRef("PI-TOKEN-SECRET-STYLE",
                "tok_card_006",
                Map.of("Token Secret", "secret-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindingSnapshot must not contain sensitive payment instrument fields");
    }

    /**
     * 场景：业务侧错误地把外部银行账户原文放入 route external account 快照。
     * 预期：构造期拒绝外部账户原始账号和上下文敏感字段。
     * 红线：外部账户、VA、卡或通道 token 的敏感原文不得进入普通快照、日志、导出或报表。
     */
    @Test
    void testExternalAccountSnapshotShouldRejectSensitiveAccountValues() {
        assertThatThrownBy(() -> externalAccountRef("EA-RAW", "1234567890123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalAccountNo must be masked or token reference");
        assertThatThrownBy(() -> externalAccountRef("EA-RAW-IBAN", "GB82WEST12345698765432"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalAccountNo must be masked or token reference");
        assertThatThrownBy(() -> externalAccountRef("EA-NESTED-RAW",
                "token:external-account-001",
                Map.<String, Object>of("processorPayload", Map.of("accountNumber", "1234567890123456"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
        assertThatThrownBy(() -> externalAccountRef("EA-NESTED-IBAN-VALUE",
                "token:external-account-002",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：通道回传外部账户字段名时使用大小写、空格或短横线变体。
     * 预期：字段名归一化后仍按敏感外部账户字段阻断。
     * 红线：外部账户号和 routing number 不能因字段命名风格差异进入 route 快照。
     */
    @Test
    void testExternalAccountSnapshotShouldRejectNormalizedSensitiveContextFields() {
        assertThatThrownBy(() -> externalAccountRef("EA-ACCOUNT-NUMBER-STYLE",
                "token:external-account-006",
                Map.of("Account Number", "token:external-account-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
        assertThatThrownBy(() -> externalAccountRef("EA-ROUTING-NUMBER-STYLE",
                "token:external-account-007",
                Map.of("routing-number", "token:external-routing-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：内部资金交易号以两个字母加数字开头，形态上接近 IBAN 前缀。
     * 预期：上下文允许保存内部交易号引用。
     * 红线：敏感值识别不能误杀内部资金交易号、账本交易号或幂等号，导致授权后续链路不可执行。
     */
    @Test
    void testExternalAccountContextShouldAllowInternalFundsTransactionSnLikeIbanPrefix() {
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-INTERNAL-REF",
                "token:external-account-003",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "FT2026052714000062")));

        assertThat(externalAccountRef.getContextVariables())
                .containsEntry("processorPayload", Map.of("networkReference", "FT2026052714000062"));
    }

    /**
     * 场景：解冻和提现链路把内部冻结单号放入 referenceFreezeSn，冻结单号可能形似有效 IBAN。
     * 预期：内部冻结单引用允许进入上下文，但相同值放在普通通道字段中仍按敏感 IBAN 阻断。
     * 红线：敏感值治理不能误杀内部资金生命周期引用，也不能放开普通字段中的真实 IBAN。
     */
    @Test
    void testExternalAccountContextShouldAllowInternalFreezeSnOnlyForReferenceField() {
        String freezeSn = "FO2026052716000030";
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-INTERNAL-FREEZE-REF",
                "token:external-account-004",
                Map.<String, Object>of("referenceFreezeSn", freezeSn));

        assertThat(externalAccountRef.getContextVariables()).containsEntry("referenceFreezeSn", freezeSn);
        assertThatThrownBy(() -> externalAccountRef("EA-INTERNAL-FREEZE-RAW",
                "token:external-account-005",
                Map.<String, Object>of("processorPayload", Map.of("networkReference", "GB82WEST12345698765432"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables must not contain sensitive external account fields");
    }

    /**
     * 场景：调用方在支付工具快照构造后继续改写原始嵌套绑定上下文。
     * 预期：已构造的支付工具快照保持稳定，不被追加的卡敏感字段污染。
     * 红线：bindingSnapshot 会进入 route snapshot、日志和报表，不能因浅拷贝绕过构造期敏感字段校验。
     */
    @Test
    void testPaymentInstrumentSnapshotShouldDefensivelyCopyNestedBindingSnapshot() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "tok_card_005");
        PaymentInstrumentRefSpec instrumentRef = paymentInstrumentRef("PI-IMMUTABLE",
                "tok_card_005",
                Map.of("processorPayload", processorPayload));

        processorPayload.put("pan", "4242424242424242");

        Object payloadValue = instrumentRef.getBindingSnapshot().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("tok_card_005");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    /**
     * 场景：调用方在外部账户快照构造后继续改写原始嵌套上下文。
     * 预期：已构造的外部账户快照保持稳定，不被追加的账户原文污染。
     * 红线：外部账户快照不能因浅拷贝让银行账号原文进入普通 route 上下文。
     */
    @Test
    void testExternalAccountSnapshotShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("networkReference", "token:external-account-004");
        ExternalAccountRefSpec externalAccountRef = externalAccountRef("EA-IMMUTABLE",
                "token:external-account-004",
                Map.of("processorPayload", processorPayload));

        processorPayload.put("accountNumber", "1234567890123456");

        Object payloadValue = externalAccountRef.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:external-account-004");
        assertThat(payload.containsKey("accountNumber")).isFalse();
    }

    private RouteLegSpec routeLeg(SubjectRef payer, SubjectRef payee) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode(payer, RouteNodeRole.SOURCE))
                .targetNode(routeNode(payee, RouteNodeRole.TARGET))
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private RoutingDecisionSpec routingDecision(String policyCode,
                                                List<FundingAllocationDecisionSpec> fundingAllocations) {
        return routingDecision(policyCode, fundingAllocations, Map.of("accountModel", policyCode));
    }

    private RoutingDecisionSpec routingDecision(String policyCode,
                                                List<FundingAllocationDecisionSpec> fundingAllocations,
                                                Map<String, Object> contextVariables) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(policyCode)
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY", policyCode))
                .selectedProcessor("CARD_PROCESSOR")
                .fundingAllocations(fundingAllocations)
                .decisionReason(policyCode + "_DECISION")
                .contextVariables(contextVariables)
                .build();
    }

    private FundingAllocationDecisionSpec fundingAllocation(String allocationId,
                                                            SubjectRef subjectRef,
                                                            LedgerSubjectCode ledgerSubjectCode,
                                                            Integer priority,
                                                            String reason) {
        return ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId(allocationId)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .priority(priority)
                .reason(reason)
                .build();
    }

    private RouteNodeSpec routeNode(SubjectRef subjectRef, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef fundingAccount(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private SubjectRef budgetGroup(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.BUDGET_GROUP);
    }

    private SubjectRef subjectRef(String subjectId, FundsSubjectType subjectType) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(CurrencyIsoCode.USD.name())
                .ledgerProfileCode("DEFAULT")
                .build();
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId, String instrumentNo) {
        return paymentInstrumentRef(instrumentId,
                instrumentNo,
                Map.of("bindingId", "BINDING-001", "bindingVersion", 3));
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId,
                                                          String instrumentNo,
                                                          Map<String, Object> bindingSnapshot) {
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(1L)
                .instrumentId(instrumentId)
                .instrumentType("CARD")
                .instrumentNo(instrumentNo)
                .ownerId("USER-001")
                .ownerType("USER")
                .currency(CurrencyIsoCode.USD.name())
                .status("ACTIVE")
                .bindingSnapshot(bindingSnapshot)
                .build();
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId, String externalAccountNo) {
        return externalAccountRef(externalAccountId,
                externalAccountNo,
                Map.of("externalAccountVersion", 2));
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId,
                                                      String externalAccountNo,
                                                      Map<String, Object> contextVariables) {
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId)
                .externalAccountType("BANK_ACCOUNT")
                .externalAccountNo(externalAccountNo)
                .providerCode("BANK")
                .channelCode("ACH")
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(contextVariables)
                .build();
    }
}
