package com.capte.funds.dsl;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ReplayRequestSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

        ReplayRequestSpec replayRequest = replayRequest("RS-PAY-001", RouteReplayType.REFUND);

        assertThat(replayRequest.getReferenceSnapshotId()).isEqualTo("RS-PAY-001");
        assertThat(replayRequest.getReplayType()).isEqualTo(RouteReplayType.REFUND);
    }

    private ReplayRequestSpec replayRequest(String referenceSnapshotId, RouteReplayType replayType) {
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
                .contextVariables(Map.of("semanticBoundary", "ROUTE_REPLAY_ONLY"))
                .build();
    }

    private RouteLegSpec routeLeg(RouteNodeSpec sourceNode, RouteNodeSpec targetNode) {
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
                .contextVariables(Map.of())
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
