package com.wind.funds.transaction.services.impl;

import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableAccountHierarchyFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutableAccountHierarchySnapshotSpec;
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
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RouteSnapshot JSON 摘要契约测试。
 */
class RouteSnapshotJsonSupportTests {

    /**
     * 场景：含权益交易的 RouteSnapshot 被保存为交易事实 JSON，随后用于退款或回放查询。
     * 输入：RouteSnapshot context 携带原权益快照 ID 和稳定摘要，且 route leg 有确定 sequence。
     * 输出：JSON 往返后的 RouteSnapshot。
     * 预期：权益摘要和 leg sequence 都不丢失。
     * 红线：原路径快照不能在持久化摘要层丢失权益回放依据或 route leg 顺序事实。
     */
    @Test
    void testRouteSnapshotJsonShouldKeepBenefitSummaryAndLegSequence() {
        RouteSnapshotSpec snapshot = routeSnapshot(Map.of(
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-JSON-001",
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST, "sha256:original-benefit-digest"));

        String json = RouteSnapshotJsonSupport.toRouteSnapshotJson(snapshot);
        RouteSnapshotSpec parsed = RouteSnapshotJsonSupport.parseRouteSnapshot(json,
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertThat(parsed.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-JSON-001")
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                        "sha256:original-benefit-digest");
        assertThat(parsed.getLegs()).singleElement()
                .extracting(RouteLegSpec::getSequence)
                .isEqualTo(7);
    }

    /**
     * 场景：VCC 共享卡经支付工具路由后，实际资金责任落到子信用账户。
     * 输入：RouteSnapshot routingDecision 的 funding allocation 携带账户层级快照。
     * 输出：JSON 往返后的 RouteSnapshot。
     * 预期：子账户、父账户、根账户和层级版本不丢失。
     * 红线：交易事实快照必须能支撑共享卡按卡、按子账户、按主账户追溯，且不得保存完整卡号。
     */
    @Test
    void testRouteSnapshotJsonShouldKeepAccountHierarchySnapshotForVccSharedCard() {
        RouteSnapshotSpec snapshot = routeSnapshot(Map.of(), vccSharedCardRoutingDecision());

        String json = RouteSnapshotJsonSupport.toRouteSnapshotJson(snapshot);
        RouteSnapshotSpec parsed = RouteSnapshotJsonSupport.parseRouteSnapshot(json,
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertThat(json)
                .contains("accountHierarchySnapshot")
                .doesNotContain("4111111111111111");
        assertThat(parsed.getRoutingDecision().getFundingAllocations()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.getSubjectRef().getSubjectType())
                            .isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
                    assertThat(allocation.getSubjectRef().getSubjectId()).isEqualTo("VCC-CREDIT-SUB-001");
                    AccountHierarchySnapshotSpec hierarchySnapshot = allocation.getAccountHierarchySnapshot();
                    assertThat(hierarchySnapshot).isNotNull();
                    assertThat(hierarchySnapshot.getAccountRef().getSubjectId()).isEqualTo("VCC-CREDIT-SUB-001");
                    assertThat(hierarchySnapshot.getParentAccountRef()).isNotNull();
                    assertThat(hierarchySnapshot.getParentAccountRef().getSubjectId())
                            .isEqualTo("VCC-CREDIT-MAIN-001");
                    assertThat(hierarchySnapshot.getRootAccountRef()).isNotNull();
                    assertThat(hierarchySnapshot.getRootAccountRef().getSubjectId())
                            .isEqualTo("VCC-CREDIT-MAIN-001");
                    assertThat(hierarchySnapshot.getHierarchyVersion()).isEqualTo("vcc-shared-card-binding-v1");
                    assertThat(hierarchySnapshot.getContextVariables())
                            .containsEntry("instrumentId", "VCC-CARD-001")
                            .containsEntry("instrumentType", "VCC_SHARED_CARD");
                });
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables) {
        return routeSnapshot(contextVariables, null);
    }

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables,
                                            RoutingDecisionSpec routingDecision) {
        ImmutableSubjectRef sourceSubjectRef = sourceSubjectRef(routingDecision);
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("ROUTE-SNAPSHOT-BEN-001")
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .routeCode(FundsRouteCodes.DIRECT_PAY_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene("ORDER_PAY")
                .businessSn("ORDER_PAY_BEN_JSON_001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(ImmutableRouteParticipantSpec.builder()
                        .participantRole(RouteParticipantRole.PAYER)
                        .subjectRef(subject("PAYER-001"))
                        .ledgerProfileCode("FUNDING_BASIC")
                        .currency(CurrencyIsoCode.USD.name())
                        .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                        .contextVariables(Map.of())
                        .build()))
                .legs(List.of(routeLeg(sourceSubjectRef)))
                .routingDecision(routingDecision)
                .resolvedAt(LocalDateTime.of(2026, 5, 24, 10, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RoutingDecisionSpec vccSharedCardRoutingDecision() {
        ImmutableSubjectRef childCreditAccount = subject("VCC-CREDIT-SUB-001", FundsSubjectType.CREDIT_ACCOUNT);
        AccountHierarchySnapshotSpec hierarchySnapshot = ImmutableAccountHierarchySnapshotSpec.builder()
                .accountRef(childCreditAccount)
                .parentAccountRef(subject("VCC-CREDIT-MAIN-001", FundsSubjectType.CREDIT_ACCOUNT))
                .rootAccountRef(subject("VCC-CREDIT-MAIN-001", FundsSubjectType.CREDIT_ACCOUNT))
                .hierarchyVersion("vcc-shared-card-binding-v1")
                .contextVariables(Map.of(
                        "instrumentId", "VCC-CARD-001",
                        "instrumentType", "VCC_SHARED_CARD"))
                .description("VCC shared card resolves to credit sub-account")
                .build();
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("VCC_SHARED_CARD_ROUTE")
                .matchedRules(List.of("VCC_CARD_BINDING", "ACCOUNT_HIERARCHY"))
                .selectedProcessor("VCC_ISSUER")
                .fundingAllocations(List.of(ImmutableAccountHierarchyFundingAllocationDecisionSpec.builder()
                        .allocationId("ALLOC-VCC-CARD-001")
                        .subjectRef(childCreditAccount)
                        .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                        .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                        .accountHierarchySnapshot(hierarchySnapshot)
                        .priority(1)
                        .reason("VCC shared card funding responsibility")
                        .build()))
                .decisionReason("VCC shared card payment instrument resolved to credit sub-account")
                .contextVariables(Map.of("routeSource", "paymentInstrument"))
                .build();
    }

    private RouteLegSpec routeLeg(ImmutableSubjectRef sourceSubjectRef) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(7)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode(sourceSubjectRef, LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE))
                .targetNode(routeNode(subject("PAYEE-001"), LedgerSubjectCode.SETTLEMENT, RouteNodeRole.TARGET))
                .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableSubjectRef sourceSubjectRef(RoutingDecisionSpec routingDecision) {
        if (routingDecision == null || routingDecision.getFundingAllocations().isEmpty()) {
            return subject("PAYER-001");
        }
        SubjectRef subjectRef = routingDecision.getFundingAllocations().get(0).getSubjectRef();
        return subject(subjectRef.getSubjectId(), subjectRef.getSubjectType());
    }

    private ImmutableRouteNodeSpec routeNode(ImmutableSubjectRef subjectRef,
                                             LedgerSubjectCode ledgerSubjectCode,
                                             RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    private ImmutableSubjectRef subject(String subjectId) {
        return subject(subjectId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private ImmutableSubjectRef subject(String subjectId, FundsSubjectType subjectType) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(subjectType)
                .currency(CurrencyIsoCode.USD.name())
                .build();
    }
}
