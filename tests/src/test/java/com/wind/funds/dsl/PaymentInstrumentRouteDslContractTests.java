package com.wind.funds.dsl;

import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.model.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.route.model.ImmutableExternalAccountRefSpec;
import com.wind.funds.route.model.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableRoutingDecisionSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
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
     * 场景：支付工具和业务规则共同决定路由。
     * 预期：RoutingDecision 保留策略、命中规则、处理方和选择原因。
     * 红线：RoutingDecision 只解释选路，不重复表达 RouteLeg 中已经确定的资金主体和金额。
     */
    @Test
    void testRoutingDecisionShouldRecordRouteSelectionFacts() {
        RoutingDecisionSpec decision = ImmutableRoutingDecisionSpec.builder()
                .policyCode("PAYMENT_INSTRUMENT_ROUTE")
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY"))
                .selectedProcessor("CARD_PROCESSOR")
                .decisionReason("ACTIVE_CARD_ROUTE")
                .contextVariables(Map.of("bindingVersion", 3))
                .build();

        assertThat(decision.getPolicyCode()).isEqualTo("PAYMENT_INSTRUMENT_ROUTE");
        assertThat(decision.getMatchedRules()).containsExactly("INSTRUMENT_ACTIVE", "DIRECTION_PAY");
        assertThat(decision.getSelectedProcessor()).isEqualTo("CARD_PROCESSOR");
        assertThat(decision.getDecisionReason()).isEqualTo("ACTIVE_CARD_ROUTE");
    }

    /**
     * 场景：VCC 共享卡路由到信用子账户，子账户当时存在直接父资金账户。
     * 预期：支付工具保持引用身份，层级关系固化在信用账户 participant。
     * 红线：账户层级不得创建父账户 RouteLeg，也不得把支付工具当作账本主体。
     */
    @Test
    void testVccSharedCardShouldKeepHierarchyOnCreditAccountParticipant() {
        PaymentInstrumentRefSpec sharedCard = paymentInstrumentRef("PI-VCC-SHARED-001",
                "**** 1888",
                Map.of("bindingRole", "VCC_SHARED_CARD", "bindingVersion", 7));
        SubjectRef cardCreditAccount = creditAccount("CA-VCC-CARD-001");
        SubjectRef parentAccount = fundingAccount("FA-VCC-PARENT-001");
        AccountHierarchySnapshotSpec hierarchySnapshot = ImmutableAccountHierarchySnapshotSpec.builder()
                .relationSn("AHR-VCC-SHARED-001")
                .parentAccountRef(parentAccount)
                .build();
        RouteParticipantSpec participant = ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(cardCreditAccount)
                .ledgerProfileCode("CREDIT_BASIC")
                .currency(CurrencyIsoCode.USD)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .accountHierarchySnapshot(hierarchySnapshot)
                .contextVariables(Map.of())
                .build();

        assertThat(sharedCard.getBindingSnapshot()).containsEntry("bindingRole", "VCC_SHARED_CARD");
        assertThat(participant.getSubjectRef()).isSameAs(cardCreditAccount);
        assertThat(participant.getAccountHierarchySnapshot().getRelationSn())
                .isEqualTo("AHR-VCC-SHARED-001");
        assertThat(participant.getAccountHierarchySnapshot().getParentAccountRef()).isSameAs(parentAccount);
    }

    /**
     * 场景：支付工具路由决策上下文被调用方塞入通道密钥或支付工具原文。
     * 预期：RoutingDecision 构造期立即拒绝。
     * 红线：路由决策会进入 route snapshot、归档重放和审计链路，不能保存 PAN、CVV、密钥或外部账户原文。
     */
    @Test
    void testRoutingDecisionContextVariablesShouldRejectSensitiveValues() {
        assertThatThrownBy(() -> routingDecision("SENSITIVE_ROUTING_CONTEXT",
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
                Map.of("processorPayload", processorPayload));

        processorPayload.put("pan", "4242424242424242");

        Object payloadValue = decision.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("networkReference")).isEqualTo("token:route-decision-001");
        assertThat(payload.containsKey("pan")).isFalse();
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
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .replayRefLegId(originalLeg.getLegId())
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
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .contextVariables(Map.of())
                .build();
    }

    private RoutingDecisionSpec routingDecision(String policyCode,
                                                Map<String, Object> contextVariables) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode(policyCode)
                .matchedRules(List.of("INSTRUMENT_ACTIVE", "DIRECTION_PAY", policyCode))
                .selectedProcessor("CARD_PROCESSOR")
                .decisionReason(policyCode + "_DECISION")
                .contextVariables(contextVariables)
                .build();
    }

    private RouteNodeSpec routeNode(SubjectRef subjectRef, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef fundingAccount(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private SubjectRef creditAccount(String subjectId) {
        return subjectRef(subjectId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private SubjectRef subjectRef(String subjectId, FundsSubjectType subjectType) {
        return accountSubject(subjectId, subjectType, 1L, CurrencyIsoCode.USD);
    }

    private SubjectRef accountSubject(String subjectId,
                                      FundsSubjectType subjectType,
                                      Long tenantId,
                                      CurrencyIsoCode currency) {
        return ImmutableSubjectRef.builder()
                .tenantId(tenantId)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(currency)
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
                .currency(CurrencyIsoCode.USD)
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
                .currency(CurrencyIsoCode.USD)
                .countryCode("US")
                .contextVariables(contextVariables)
                .build();
    }
}
