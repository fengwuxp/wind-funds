package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
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

    private RouteSnapshotSpec routeSnapshot(Map<String, Object> contextVariables) {
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
                .legs(List.of(routeLeg()))
                .resolvedAt(LocalDateTime.of(2026, 5, 24, 10, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RouteLegSpec routeLeg() {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(7)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode(subject("PAYER-001"), LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE))
                .targetNode(routeNode(subject("PAYEE-001"), LedgerSubjectCode.SETTLEMENT, RouteNodeRole.TARGET))
                .amount(Money.immutable(900L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .contextVariables(Map.of())
                .build();
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
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .build();
    }
}
