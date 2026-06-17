package com.wind.funds.dsl;

import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.funds.route.spec.ReplayRequestSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Route DSL 契约测试。
 */
class RouteDslContractTests {

    /**
     * 场景：支付工具或外部账户参与路由解析。
     * 预期：它们只能保存在工具、外部账户或决策快照中，不能成为 RouteLeg 的账务节点。
     * 红线：支付工具、外部账户或通道引用不得被写成可入账主体。
     */
    @Test
    void testRouteLegShouldRejectPaymentInstrumentOrExternalAccountNode() {
        RouteNodeSpec fundingSource = routeNode(RouteNodeType.SUBJECT,
                fundingAccount("FA-PAYER-001"),
                LedgerSubjectCode.AVAILABLE,
                RouteNodeRole.SOURCE);
        RouteNodeSpec paymentInstrumentNode = routeNode(RouteNodeType.PAYMENT_INSTRUMENT,
                fundingAccount("PI-CARD-001"),
                LedgerSubjectCode.AVAILABLE,
                RouteNodeRole.SOURCE);
        RouteNodeSpec externalAccountNode = routeNode(RouteNodeType.EXTERNAL_ACCOUNT,
                fundingAccount("EA-BANK-001"),
                LedgerSubjectCode.AVAILABLE,
                RouteNodeRole.TARGET);

        assertThatThrownBy(() -> routeLeg(paymentInstrumentNode, fundingSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RouteLeg sourceNode must be ledger-postable");
        assertThatThrownBy(() -> routeLeg(fundingSource, externalAccountNode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RouteLeg targetNode must be ledger-postable");
    }

    /**
     * 场景：平台角色已经解析成具体平台资金账户。
     * 预期：Route DSL 可以表达平台资金账户节点，后续入账仍依赖具体 SubjectRef。
     * 红线：不能把“平台角色不能直接入账”误收窄成“平台资金账户节点也不可表达”。
     */
    @Test
    void testRouteLegShouldAllowResolvedPlatformFundingAccountNode() {
        RouteNodeSpec platformSettlementAccount = routeNode(RouteNodeType.PLATFORM_FUNDING_ACCOUNT,
                fundingAccount("FA-PLATFORM-SETTLEMENT"),
                LedgerSubjectCode.SETTLEMENT,
                RouteNodeRole.TARGET);

        RouteLegSpec leg = routeLeg(routeNode(RouteNodeType.SUBJECT,
                fundingAccount("FA-PAYER-001"),
                LedgerSubjectCode.AVAILABLE,
                RouteNodeRole.SOURCE), platformSettlementAccount);

        assertThat(leg.getTargetNode().getNodeType()).isEqualTo(RouteNodeType.PLATFORM_FUNDING_ACCOUNT);
        assertThat(leg.getTargetNode().getSubjectRef().getSubjectId()).isEqualTo("FA-PLATFORM-SETTLEMENT");
    }

    /**
     * 场景：后续退款、撤销、授权完成、退费、拒付或解冻触发 Route Replay。
     * 预期：ReplayRequest 必须引用原 route snapshot，且声明具体 replay 类型。
     * 红线：缺原快照不得退化为重新路由，也不得把交易投影重放、余额重建或归档续跑伪装成 Route Replay。
     */
    @Test
    void testReplayRequestShouldRequireOriginalRouteSnapshotReference() {
        assertThatThrownBy(() -> replayRequest(null, RouteReplayType.REFUND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenceSnapshotId is required for route replay");
        assertThatThrownBy(() -> replayRequest("RS-PAY-001", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("replayType is required for route replay");

        ReplayRequestSpec replayRequest = replayRequest("RS-PAY-001", RouteReplayType.REFUND);

        assertThat(replayRequest.getReferenceSnapshotId()).isEqualTo("RS-PAY-001");
        assertThat(replayRequest.getReplayType()).isEqualTo(RouteReplayType.REFUND);
    }

    /**
     * 场景：运行态 route 或持久化 route snapshot 缺少身份、类型或时间字段。
     * 预期：构造期给出明确错误，不能让坏快照进入交易事实、账务装配或回放链路。
     * 红线：核心 route 事实不能只依赖注解表达必填，必须有运行期契约校验。
     */
    @Test
    void testRouteFactsShouldRequireIdentityTypeAndResolvedTime() {
        assertThatThrownBy(() -> ImmutableResolvedRouteSpec.builder()
                .routeVersion("1.0")
                .businessScene("ROUTE_DSL")
                .businessSn("BIZ-ROUTE-REQUIRED-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(routeParticipant(Map.of())))
                .legs(List.of(routeLeg(Map.of())))
                .resolvedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .contextVariables(Map.of())
                .build())
                .hasMessageContaining("resolvedRoute.routeCode must not be blank");

        assertThatThrownBy(() -> ImmutableRouteSnapshotSpec.builder()
                .snapshotId("RS-REQUIRED-001")
                .snapshotSchemaVersion("1.0")
                .routeCode("DIRECT_PAY_STANDARD")
                .routeVersion("1.0")
                .businessScene("ROUTE_DSL")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(routeParticipant(Map.of())))
                .legs(List.of(routeLeg(Map.of())))
                .resolvedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .contextVariables(Map.of())
                .build())
                .hasMessageContaining("routeSnapshot.businessSn must not be blank");

        assertThatThrownBy(() -> ImmutableRouteSnapshotSpec.builder()
                .snapshotId("RS-REQUIRED-002")
                .snapshotSchemaVersion("1.0")
                .routeCode("DIRECT_PAY_STANDARD")
                .routeVersion("1.0")
                .businessScene("ROUTE_DSL")
                .businessSn("BIZ-ROUTE-REQUIRED-002")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(routeParticipant(Map.of())))
                .legs(List.of(routeLeg(Map.of())))
                .contextVariables(Map.of())
                .build())
                .hasMessageContaining("routeSnapshot.resolvedAt must not be null");
    }

    /**
     * 场景：路由快照、路由分录或参与方扩展上下文被调用方塞入通道密钥或外部账户原文。
     * 预期：Route DSL 构造期立即拒绝。
     * 红线：route snapshot 会进入交易事实、账务装配和归档重放链路，不能保存 PAN、CVV、密钥或银行账户原文。
     */
    @Test
    void testRouteDslContextVariablesShouldRejectSensitiveValues() {
        assertThatThrownBy(() -> resolvedRoute(Map.of("processorPayload", Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("resolvedRoute.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> routeSnapshot(Map.of("processorPayload", Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("routeSnapshot.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> ImmutableRouteLegSpec.builder()
                .legId("PAY-SENSITIVE")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode(RouteNodeType.SUBJECT,
                        fundingAccount("FA-PAYER-SENSITIVE"),
                        LedgerSubjectCode.AVAILABLE,
                        RouteNodeRole.SOURCE))
                .targetNode(routeNode(RouteNodeType.SUBJECT,
                        fundingAccount("FA-PAYEE-SENSITIVE"),
                        LedgerSubjectCode.SETTLEMENT,
                        RouteNodeRole.TARGET))
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .contextVariables(Map.of("processorPayload", Map.of("cardSecurityCode", "123")))
                .build())
                .hasMessageContaining("routeLeg.contextVariables must not contain sensitive fields");

        assertThatThrownBy(() -> routeParticipant(
                Map.of("externalAccount", Map.of("bankAccountNo", "123456789012"))))
                .hasMessageContaining("routeParticipant.contextVariables must not contain sensitive fields");
    }

    /**
     * 场景：调用方把权益金额、资金责任或当前营销规则藏入 route 事实链上下文。
     * 预期：Route DSL 构造期显式失败，但仍允许只放权益快照引用和稳定摘要。
     * 红线：route snapshot、route leg、route participant、routing decision 和 replay request 不能承载权益核心事实。
     */
    @Test
    void testRouteDslContextShouldRejectCoreBenefitFactsButAllowSummaryRefs() {
        assertThatThrownBy(() -> resolvedRoute(Map.of("benefitPayload", Map.of(
                "amount", Money.immutable(2000L, CurrencyIsoCode.USD),
                "fundingNature", "PLATFORM_BORNE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolvedRoute.contextVariables must not contain core benefit field");

        assertThatThrownBy(() -> routeSnapshot(Map.of("benefitDecisionTrace",
                new Object[] {Map.of("currentMarketingRule", "latest-rule")})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeSnapshot.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        assertThatThrownBy(() -> routeLeg(Map.of("benefitPayload", Map.of("fundingNature", "PLATFORM_BORNE"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeLeg.contextVariables must not contain core benefit field: fundingNature");

        assertThatThrownBy(() -> routeParticipant(Map.of("benefitPayload", Map.of(
                "refundDisposition", "REFUND_TO_PLATFORM"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "routeParticipant.contextVariables must not contain core benefit field: refundDisposition");

        assertThatThrownBy(() -> routingDecision(Map.of("benefitDecisionTrace",
                List.of(Map.of("userCouponBag", "latest-bag")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routingDecision.contextVariables must not contain core benefit field: "
                        + "userCouponBag");

        assertThatThrownBy(() -> replayRequest(Map.of("benefitReplayPayload", Map.of(
                "nonRefundableAmount", Money.immutable(100L, CurrencyIsoCode.USD)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "replayRequest.contextVariables must not contain core benefit field: nonRefundableAmount");

        Map<String, Object> summaryRefs = Map.of(
                "benefitSnapshotId", "BS-ROUTE-SUMMARY-001",
                "stableDigest", "sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                "ruleVersion", "rule-v1",
                "refundDecisionId", "refund-decision-001",
                "externalDecisionId", "pricing-decision-001");

        assertThat(routeSnapshot(summaryRefs).getContextVariables())
                .containsEntry("benefitSnapshotId", "BS-ROUTE-SUMMARY-001")
                .containsEntry("stableDigest",
                        "sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd")
                .containsEntry("ruleVersion", "rule-v1")
                .containsEntry("refundDecisionId", "refund-decision-001")
                .containsEntry("externalDecisionId", "pricing-decision-001");
    }

    /**
     * 场景：调用方在 route participant 构造后继续改写原始嵌套上下文。
     * 预期：已构造的路由参与方上下文保持稳定，不被追加的外部账户原文污染。
     * 红线：route snapshot 会进入交易事实和归档重放，不能因浅拷贝绕过敏感字段校验。
     */
    @Test
    void testRouteDslShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> externalAccount = new HashMap<>();
        externalAccount.put("accountToken", "token:external-account-001");
        RouteParticipantSpec participant = routeParticipant(Map.of("externalAccount", externalAccount));

        externalAccount.put("bankAccountNo", "123456789012");

        Object accountValue = participant.getContextVariables().get("externalAccount");
        assertThat(accountValue).isInstanceOf(Map.class);
        Map<?, ?> account = (Map<?, ?>) accountValue;
        assertThat(account.get("accountToken")).isEqualTo("token:external-account-001");
        assertThat(account.containsKey("bankAccountNo")).isFalse();
    }

    /**
     * 场景：调用方在 route snapshot、route leg 和 replay request 构造后继续改写原始嵌套上下文。
     * 预期：已构造的路由事实和回放请求保持稳定，不被追加的敏感字段污染。
     * 红线：路由快照、账务分录路径和回放请求都会进入交易事实或重放链路，不能因浅拷贝绕过敏感字段校验。
     */
    @Test
    void testRouteFactsShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> resolvedRouteProcessor = new HashMap<>();
        resolvedRouteProcessor.put("processorToken", "token:resolved-route-001");
        ResolvedRouteSpec resolvedRoute = resolvedRoute(Map.of("processorPayload", resolvedRouteProcessor));
        resolvedRouteProcessor.put("secretKey", "secret-after-build");

        Object resolvedRouteValue = resolvedRoute.getContextVariables().get("processorPayload");
        assertThat(resolvedRouteValue).isInstanceOf(Map.class);
        Map<?, ?> resolvedRoutePayload = (Map<?, ?>) resolvedRouteValue;
        assertThat(resolvedRoutePayload.get("processorToken")).isEqualTo("token:resolved-route-001");
        assertThat(resolvedRoutePayload.containsKey("secretKey")).isFalse();

        Map<String, Object> snapshotProcessor = new HashMap<>();
        snapshotProcessor.put("processorToken", "token:route-snapshot-001");
        RouteSnapshotSpec snapshot = routeSnapshot(Map.of("processorPayload", snapshotProcessor));
        snapshotProcessor.put("secretKey", "secret-after-build");

        Object snapshotValue = snapshot.getContextVariables().get("processorPayload");
        assertThat(snapshotValue).isInstanceOf(Map.class);
        Map<?, ?> snapshotPayload = (Map<?, ?>) snapshotValue;
        assertThat(snapshotPayload.get("processorToken")).isEqualTo("token:route-snapshot-001");
        assertThat(snapshotPayload.containsKey("secretKey")).isFalse();

        Map<String, Object> legProcessor = new HashMap<>();
        legProcessor.put("processorToken", "token:route-leg-001");
        RouteLegSpec leg = routeLeg(Map.of("processorPayload", legProcessor));
        legProcessor.put("cardSecurityCode", "123");

        Object legValue = leg.getContextVariables().get("processorPayload");
        assertThat(legValue).isInstanceOf(Map.class);
        Map<?, ?> legPayload = (Map<?, ?>) legValue;
        assertThat(legPayload.get("processorToken")).isEqualTo("token:route-leg-001");
        assertThat(legPayload.containsKey("cardSecurityCode")).isFalse();

        Map<String, Object> replayAudit = new HashMap<>();
        replayAudit.put("snapshotDigest", "digest:route-replay-001");
        ReplayRequestSpec replayRequest = replayRequest(Map.of("replayAudit", replayAudit));
        replayAudit.put("bankAccountNo", "123456789012");

        Object replayValue = replayRequest.getContextVariables().get("replayAudit");
        assertThat(replayValue).isInstanceOf(Map.class);
        Map<?, ?> replayPayload = (Map<?, ?>) replayValue;
        assertThat(replayPayload.get("snapshotDigest")).isEqualTo("digest:route-replay-001");
        assertThat(replayPayload.containsKey("bankAccountNo")).isFalse();
    }

    private ReplayRequestSpec replayRequest(String referenceSnapshotId, RouteReplayType replayType) {
        return replayRequest(referenceSnapshotId, replayType, Map.of("semanticBoundary", "ROUTE_REPLAY_ONLY"));
    }

    private ReplayRequestSpec replayRequest(Map<String, Object> contextVariables) {
        return replayRequest("RS-PAY-001", RouteReplayType.REFUND, contextVariables);
    }

    private ReplayRequestSpec replayRequest(String referenceSnapshotId,
                                            RouteReplayType replayType,
                                            Map<String, Object> contextVariables) {
        return ImmutableReplayRequestSpec.builder()
                .replayType(replayType)
                .eventType(FundsTransactionEventType.REFUND)
                .businessScene("ROUTE_REPLAY_DSL")
                .businessSn("BIZ-ROUTE-REPLAY-001")
                .referenceBusinessSn("BIZ-PAY-001")
                .referenceSnapshotId(referenceSnapshotId)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(100L, CurrencyIsoCode.USD))
                .replayLegIds(java.util.List.of("PAY"))
                .eventTime(LocalDateTime.of(2026, 5, 20, 10, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RouteLegSpec routeLeg(RouteNodeSpec sourceNode, RouteNodeSpec targetNode) {
        return routeLeg(sourceNode, targetNode, Map.of());
    }

    private RouteLegSpec routeLeg(Map<String, Object> contextVariables) {
        return routeLeg(routeNode(RouteNodeType.SUBJECT,
                        fundingAccount("FA-PAYER-LEG"),
                        LedgerSubjectCode.AVAILABLE,
                        RouteNodeRole.SOURCE),
                routeNode(RouteNodeType.SUBJECT,
                        fundingAccount("FA-PAYEE-LEG"),
                        LedgerSubjectCode.SETTLEMENT,
                        RouteNodeRole.TARGET),
                contextVariables);
    }

    private RouteLegSpec routeLeg(RouteNodeSpec sourceNode,
                                  RouteNodeSpec targetNode,
                                  Map<String, Object> contextVariables) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(sourceNode)
                .targetNode(targetNode)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .constraintOverrides(Map.of())
                .contextVariables(contextVariables)
                .build();
    }

    private ResolvedRouteSpec resolvedRoute(Map<String, Object> contextVariables) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(1L)
                .routeCode("DIRECT_PAY_STANDARD")
                .routeVersion("1.0")
                .businessScene("ROUTE_DSL")
                .businessSn("BIZ-ROUTE-SENSITIVE-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(routeParticipant(Map.of())))
                .legs(List.of(routeLeg(routeNode(RouteNodeType.SUBJECT,
                                fundingAccount("FA-PAYER-RESOLVED"),
                                LedgerSubjectCode.AVAILABLE,
                                RouteNodeRole.SOURCE),
                        routeNode(RouteNodeType.SUBJECT,
                                fundingAccount("FA-PAYEE-RESOLVED"),
                                LedgerSubjectCode.SETTLEMENT,
                                RouteNodeRole.TARGET))))
                .resolvedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("RS-SENSITIVE-001")
                .snapshotSchemaVersion("1.0")
                .routeCode("DIRECT_PAY_STANDARD")
                .routeVersion("1.0")
                .businessScene("ROUTE_DSL")
                .businessSn("BIZ-ROUTE-SENSITIVE-001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(routeParticipant(Map.of())))
                .legs(List.of(routeLeg(routeNode(RouteNodeType.SUBJECT,
                                fundingAccount("FA-PAYER-SNAPSHOT"),
                                LedgerSubjectCode.AVAILABLE,
                                RouteNodeRole.SOURCE),
                        routeNode(RouteNodeType.SUBJECT,
                                fundingAccount("FA-PAYEE-SNAPSHOT"),
                                LedgerSubjectCode.SETTLEMENT,
                                RouteNodeRole.TARGET))))
                .resolvedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RouteParticipantSpec routeParticipant(Map<String, Object> contextVariables) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(fundingAccount("FA-PARTICIPANT-001"))
                .ledgerProfileCode("DEFAULT")
                .currency(CurrencyIsoCode.USD.name())
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .contextVariables(contextVariables)
                .build();
    }

    private RoutingDecisionSpec routingDecision(Map<String, Object> contextVariables) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("ROUTE_BENEFIT_CONTEXT")
                .matchedRules(List.of("DIRECT_PAY", "REAL_FUNDING_ACCOUNT"))
                .fundingAllocations(List.of(fundingAllocation()))
                .decisionReason("REAL_FUNDING_ACCOUNT")
                .contextVariables(contextVariables)
                .build();
    }

    private FundingAllocationDecisionSpec fundingAllocation() {
        return ImmutableFundingAllocationDecisionSpec.builder()
                .allocationId("ALLOC-ROUTE-BENEFIT-CONTEXT")
                .subjectRef(fundingAccount("FA-ROUTE-BENEFIT-CONTEXT"))
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .priority(1)
                .reason("REAL_FUNDING_ACCOUNT")
                .build();
    }

    private RouteNodeSpec routeNode(RouteNodeType nodeType,
                                    SubjectRef subjectRef,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(nodeType)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
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
}
