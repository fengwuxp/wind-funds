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
        FundingAllocationDecisionSpec allocation = ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId("ALLOC-001")
                .subjectRef(fundingAccount("FA-PAYER-001"))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .priority(10)
                .reason("DEFAULT_PAYMENT_INSTRUMENT")
                .build();

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

    private RouteNodeSpec routeNode(SubjectRef subjectRef, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private SubjectRef fundingAccount(String subjectId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .ledgerProfileCode("DEFAULT")
                .build();
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId, String instrumentNo) {
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(1L)
                .instrumentId(instrumentId)
                .instrumentType("CARD")
                .instrumentNo(instrumentNo)
                .ownerId("USER-001")
                .ownerType("USER")
                .currency(CurrencyIsoCode.USD.name())
                .status("ACTIVE")
                .bindingSnapshot(Map.of("bindingId", "BINDING-001", "bindingVersion", 3))
                .build();
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId, String externalAccountNo) {
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId)
                .externalAccountType("BANK_ACCOUNT")
                .externalAccountNo(externalAccountNo)
                .providerCode("BANK")
                .channelCode("ACH")
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(Map.of("externalAccountVersion", 2))
                .build();
    }
}
