package com.capte.funds.route;

import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.RouteReplayService;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ReplayRequestSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认 RouteSnapshot 回放服务。
 *
 * <p>职责：基于已保存的 RouteSnapshot 派生撤销、结算、退款、拒付等后续路径。
 * 回放只复用原路径中的主体、平台账户和节点，不重新执行路由选择。</p>
 */
@Component
public class DefaultRouteReplayService implements RouteReplayService {

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private static final String REPLAY_LEG_ID_SEPARATOR = "_";

    @Override
    public @NonNull ResolvedRouteSpec replay(@NonNull RouteSnapshotSpec snapshot,
                                             @NonNull ReplayRequestSpec replayRequest) {
        assertSupportedSnapshotSchemaVersion(snapshot);
        List<RouteLegSpec> sourceLegs = selectReplayLegs(snapshot, replayRequest);
        List<RouteLegSpec> replayLegs = new ArrayList<>(sourceLegs.size());
        int sequence = 1;
        for (RouteLegSpec sourceLeg : sourceLegs) {
            replayLegs.add(replayLeg(snapshot, sourceLeg, replayRequest, sequence++));
        }
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(snapshot.getTenantId())
                .routeCode(resolveRouteCode(replayRequest))
                .routeVersion(snapshot.getRouteVersion())
                .businessScene(replayRequest.getBusinessScene())
                .businessSn(replayRequest.getBusinessSn())
                .instructionType(resolveInstructionType(snapshot, replayRequest))
                .eventType(resolveEventType(replayRequest))
                .transactionType(resolveTransactionType(snapshot, replayRequest))
                .participants(resolveParticipants(snapshot, replayLegs))
                .legs(replayLegs)
                .routingDecision(snapshot.getRoutingDecision())
                .paymentInstrumentRef(snapshot.getPaymentInstrumentRef())
                .externalAccountRef(snapshot.getExternalAccountRef())
                .platformAccounts(snapshot.getPlatformAccounts())
                .resolvedAt(replayRequest.getEventTime())
                .description(replayRequest.getDescription())
                .contextVariables(replayRequest.getContextVariables())
                .build();
    }

    private List<RouteLegSpec> selectReplayLegs(RouteSnapshotSpec snapshot, ReplayRequestSpec replayRequest) {
        AssertUtils.isFalse(snapshot.getLegs().isEmpty(), "RouteSnapshot legs 不能为空");
        Set<String> selectedLegIds = Set.copyOf(replayRequest.getReplayLegIds());
        List<RouteLegSpec> result = snapshot.getLegs().stream()
                .filter(leg -> selectedLegIds.isEmpty() || selectedLegIds.contains(leg.getLegId()))
                .filter(leg -> leg.getReplayPolicy() != RouteReplayPolicy.NON_REPLAYABLE)
                .filter(leg -> shouldReplayLeg(leg, replayRequest))
                .toList();
        AssertUtils.isFalse(result.isEmpty(), "RouteSnapshot 没有可回放的 RouteLeg");
        AssertUtils.isTrue(selectedLegIds.isEmpty() || result.size() == selectedLegIds.size(),
                "RouteSnapshot 回放 leg 不存在或不可回放，legIds = {}", selectedLegIds);
        return result;
    }

    private boolean shouldReplayLeg(RouteLegSpec leg, ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case AUTHORIZATION_REFUND, REFUND -> leg.getPhaseCode() != LedgerPhaseCode.FEE;
            case FEE_REFUND -> leg.getPhaseCode() == LedgerPhaseCode.FEE;
            default -> true;
        };
    }

    private RouteLegSpec replayLeg(RouteSnapshotSpec snapshot,
                                   RouteLegSpec sourceLeg,
                                   ReplayRequestSpec replayRequest,
                                   int sequence) {
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD -> buildReleaseLeg(sourceLeg, replayRequest, sequence, RouteLegType.RELEASE,
                    LedgerPhaseCode.REVERSAL);
            case UNFREEZE -> buildReleaseLeg(sourceLeg, replayRequest, sequence, RouteLegType.RELEASE,
                    LedgerPhaseCode.UNFREEZE);
            case AUTHORIZATION_SETTLEMENT -> buildAuthorizationSettlementLeg(snapshot, sourceLeg, replayRequest, sequence);
            case AUTHORIZATION_REFUND, REFUND, FEE_REFUND -> buildRefundLeg(snapshot, sourceLeg, replayRequest,
                    sequence, RouteLegType.RESTORE, LedgerPhaseCode.REFUND);
            case CHARGEBACK -> buildRefundLeg(snapshot, sourceLeg, replayRequest, sequence, RouteLegType.RESTORE,
                    LedgerPhaseCode.CHARGEBACK);
        };
    }

    private RouteLegSpec buildReleaseLeg(RouteLegSpec sourceLeg,
                                         ReplayRequestSpec replayRequest,
                                         int sequence,
                                         RouteLegType legType,
                                         LedgerPhaseCode phaseCode) {
        RouteNodeSpec sourceNode = copyNode(sourceLeg.getTargetNode(), RouteNodeRole.SOURCE,
                sourceLeg.getTargetNode().getLedgerSubjectCode());
        RouteNodeSpec targetNode = copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET,
                sourceLeg.getSourceNode().getLedgerSubjectCode());
        return buildReplayLeg(sourceLeg, replayRequest, sequence, legType, sourceNode, targetNode,
                LedgerBalanceEffectType.RELEASE, phaseCode, mustNotBeNegative(sourceNode),
                replayRequest.getDescription());
    }

    private RouteLegSpec buildAuthorizationSettlementLeg(RouteSnapshotSpec snapshot,
                                                         RouteLegSpec sourceLeg,
                                                         ReplayRequestSpec replayRequest,
                                                         int sequence) {
        RouteNodeSpec sourceNode = copyNode(sourceLeg.getTargetNode(), RouteNodeRole.SOURCE,
                sourceLeg.getTargetNode().getLedgerSubjectCode());
        RouteNodeSpec targetNode = resolveCaptureTargetNode(snapshot, sourceLeg);
        return buildReplayLeg(sourceLeg, replayRequest, sequence, RouteLegType.CONSUME, sourceNode, targetNode,
                LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT, mustNotBeNegative(sourceNode),
                replayRequest.getDescription());
    }

    private RouteLegSpec buildRefundLeg(RouteSnapshotSpec snapshot,
                                        RouteLegSpec sourceLeg,
                                        ReplayRequestSpec replayRequest,
                                        int sequence,
                                        RouteLegType legType,
                                        LedgerPhaseCode phaseCode) {
        RouteNodeSpec capturedTargetNode = sourceLeg.getPhaseCode() == LedgerPhaseCode.AUTHORIZATION
                ? resolveCaptureTargetNode(snapshot, sourceLeg) : sourceLeg.getTargetNode();
        RouteNodeSpec sourceNode = copyNode(capturedTargetNode, RouteNodeRole.SOURCE,
                capturedTargetNode.getLedgerSubjectCode());
        LedgerSubjectCode targetSubjectCode = sourceLeg.getSourceNode().getLedgerSubjectCode() == LedgerSubjectCode.AUTHORIZATION
                ? LedgerSubjectCode.AVAILABLE : sourceLeg.getSourceNode().getLedgerSubjectCode();
        RouteNodeSpec targetNode = copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET, targetSubjectCode);
        return buildReplayLeg(sourceLeg, replayRequest, sequence, legType, sourceNode, targetNode,
                LedgerBalanceEffectType.RESTORE, phaseCode, Map.of(),
                replayRequest.getDescription());
    }

    private void assertSupportedSnapshotSchemaVersion(RouteSnapshotSpec snapshot) {
        AssertUtils.isTrue(FundsRouteCodes.CURRENT_ROUTE_VERSION.equals(snapshot.getSnapshotSchemaVersion()),
                "RouteSnapshot snapshotSchemaVersion 不支持，snapshotSchemaVersion = {}",
                snapshot.getSnapshotSchemaVersion());
    }

    private RouteNodeSpec resolveCaptureTargetNode(RouteSnapshotSpec snapshot, RouteLegSpec sourceLeg) {
        SubjectRef targetSubject = sourceLeg.getTargetNode().getSubjectRef();
        FundsSubjectType subjectType = targetSubject.getSubjectType();
        if (subjectType == FundsSubjectType.CREDIT_ACCOUNT || subjectType == FundsSubjectType.BUDGET_GROUP) {
            return copyNode(sourceLeg.getTargetNode(), RouteNodeRole.TARGET, LedgerSubjectCode.LIMIT);
        }
        SubjectRef settlementAccount = resolveSettlementAccount(snapshot.getPlatformAccounts());
        if (settlementAccount != null) {
            return ImmutableRouteNodeSpec.builder()
                    .nodeType(RouteNodeType.PLATFORM_FUNDING_ACCOUNT)
                    .subjectRef(settlementAccount)
                    .ledgerSubjectCode(LedgerSubjectCode.SETTLEMENT)
                    .nodeRole(RouteNodeRole.TARGET)
                    .build();
        }
        return copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET, sourceLeg.getSourceNode().getLedgerSubjectCode());
    }

    private @Nullable SubjectRef resolveSettlementAccount(@Nullable PlatformAccountsSnapshotSpec platformAccounts) {
        return platformAccounts == null ? null : platformAccounts.getSettlementFundingAccount();
    }

    private RouteLegSpec buildReplayLeg(RouteLegSpec sourceLeg,
                                        ReplayRequestSpec replayRequest,
                                        int sequence,
                                        RouteLegType legType,
                                        RouteNodeSpec sourceNode,
                                        RouteNodeSpec targetNode,
                                        LedgerBalanceEffectType balanceEffectType,
                                        LedgerPhaseCode phaseCode,
                                        Map<String, LedgerBalanceConstraintType> constraints,
                                        @Nullable String description) {
        Money amount = resolveReplayAmount(sourceLeg, replayRequest);
        return ImmutableRouteLegSpec.builder()
                .legId(legType.name() + REPLAY_LEG_ID_SEPARATOR + sourceLeg.getLegId())
                .sequence(sequence)
                .legType(legType)
                .sourceNode(sourceNode)
                .targetNode(targetNode)
                .amount(amount)
                .originalAmount(sourceLeg.getOriginalAmount())
                .exchangeRate(sourceLeg.getExchangeRate())
                .balanceEffectType(balanceEffectType)
                .phaseCode(phaseCode)
                .periodType(sourceLeg.getPeriodType())
                .periodId(sourceLeg.getPeriodId())
                .replayPolicy(sourceLeg.getReplayPolicy())
                .replayRefLegId(sourceLeg.getLegId())
                .constraintOverrides(constraints)
                .description(description == null ? sourceLeg.getDescription() : description)
                .contextVariables(sourceLeg.getContextVariables())
                .build();
    }

    private Money resolveReplayAmount(RouteLegSpec sourceLeg, ReplayRequestSpec replayRequest) {
        Money amount = replayRequest.getAmount() == null ? sourceLeg.getAmount() : replayRequest.getAmount();
        Money sourceAmount = sourceLeg.getAmount();
        AssertUtils.isTrue(amount.getAmount() > 0, "RouteSnapshot 回放金额必须大于 0");
        AssertUtils.isTrue(amount.getCurrency() == sourceAmount.getCurrency(),
                "RouteSnapshot 回放金额币种必须与原 RouteLeg 一致，legId = {}", sourceLeg.getLegId());
        switch (sourceLeg.getReplayPolicy()) {
            case FULL_ONLY, REPLAY_ONCE -> AssertUtils.isTrue(amount.getAmount() == sourceAmount.getAmount(),
                    "RouteSnapshot leg 仅支持全量回放，legId = {}", sourceLeg.getLegId());
            case PARTIAL_ALLOWED -> AssertUtils.isTrue(amount.getAmount() <= sourceAmount.getAmount(),
                    "RouteSnapshot 回放金额不能大于原 RouteLeg 金额，legId = {}", sourceLeg.getLegId());
            case NON_REPLAYABLE -> throw new IllegalArgumentException("RouteSnapshot leg 不支持回放，legId = "
                    + sourceLeg.getLegId());
        }
        return amount;
    }

    private RouteNodeSpec copyNode(RouteNodeSpec node, RouteNodeRole nodeRole, LedgerSubjectCode subjectCode) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(node.getNodeType())
                .subjectRef(node.getSubjectRef())
                .ledgerSubjectCode(subjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    private Map<String, LedgerBalanceConstraintType> mustNotBeNegative(RouteNodeSpec sourceNode) {
        SubjectRef subjectRef = sourceNode.getSubjectRef();
        String fullKey = subjectRef.getSubjectType().name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectId()
                + CONSTRAINT_KEY_SEPARATOR
                + sourceNode.getLedgerSubjectCode().name();
        String subjectKey = subjectRef.getSubjectId()
                + CONSTRAINT_KEY_SEPARATOR
                + sourceNode.getLedgerSubjectCode().name();
        return Map.of(fullKey, LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE,
                subjectKey, LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    private List<RouteParticipantSpec> resolveParticipants(RouteSnapshotSpec snapshot,
                                                           List<RouteLegSpec> replayLegs) {
        Map<String, RouteParticipantSpec> participants = new LinkedHashMap<>();
        Set<String> participantSubjects = new HashSet<>();
        Set<String> replaySubjects = replaySubjectKeys(replayLegs);
        for (RouteParticipantSpec participant : snapshot.getParticipants()) {
            String participantSubject = subjectKey(participant.getSubjectRef());
            if (!replaySubjects.contains(participantSubject)) {
                continue;
            }
            participants.put(participantKey(participant.getSubjectRef(), participant.getParticipantRole()),
                    replayParticipant(participant, replayAmount(replayLegs, participant.getSubjectRef())));
            participantSubjects.add(participantSubject);
        }
        for (RouteLegSpec leg : replayLegs) {
            putParticipantIfAbsent(participants, participantSubjects, leg.getSourceNode(), RouteParticipantRole.PAYER,
                    leg.getAmount(), leg.getDescription());
            putParticipantIfAbsent(participants, participantSubjects, leg.getTargetNode(), RouteParticipantRole.PAYEE,
                    leg.getAmount(), leg.getDescription());
        }
        return List.copyOf(participants.values());
    }

    private RouteParticipantSpec replayParticipant(RouteParticipantSpec participant, Money amount) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(participant.getParticipantRole())
                .subjectRef(participant.getSubjectRef())
                .ledgerProfileCode(participant.getLedgerProfileCode())
                .currency(amount.getCurrency().name())
                .amount(amount)
                .description(participant.getDescription())
                .contextVariables(participant.getContextVariables())
                .build();
    }

    private Money replayAmount(List<RouteLegSpec> replayLegs, SubjectRef subjectRef) {
        return replayLegs.stream()
                .filter(leg -> subjectKey(leg.getSourceNode().getSubjectRef()).equals(subjectKey(subjectRef))
                        || subjectKey(leg.getTargetNode().getSubjectRef()).equals(subjectKey(subjectRef)))
                .findFirst()
                .map(RouteLegSpec::getAmount)
                .orElseThrow();
    }

    private Set<String> replaySubjectKeys(List<RouteLegSpec> replayLegs) {
        Set<String> result = new HashSet<>();
        for (RouteLegSpec leg : replayLegs) {
            result.add(subjectKey(leg.getSourceNode().getSubjectRef()));
            result.add(subjectKey(leg.getTargetNode().getSubjectRef()));
        }
        return result;
    }

    private void putParticipantIfAbsent(Map<String, RouteParticipantSpec> participants,
                                        Set<String> participantSubjects,
                                        RouteNodeSpec node,
                                        RouteParticipantRole fallbackRole,
                                        Money amount,
                                        @Nullable String description) {
        String subjectKey = subjectKey(node.getSubjectRef());
        if (participantSubjects.contains(subjectKey)) {
            return;
        }
        RouteParticipantRole role = resolveParticipantRole(node, fallbackRole);
        String key = participantKey(node.getSubjectRef(), role);
        participants.putIfAbsent(key, ImmutableRouteParticipantSpec.builder()
                .participantRole(role)
                .subjectRef(node.getSubjectRef())
                .currency(amount.getCurrency().name())
                .amount(amount)
                .description(description)
                .contextVariables(Map.of())
                .build());
        participantSubjects.add(subjectKey);
    }

    private RouteParticipantRole resolveParticipantRole(RouteNodeSpec node, RouteParticipantRole fallbackRole) {
        if (node.getNodeType() == RouteNodeType.PLATFORM_FUNDING_ACCOUNT) {
            return RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT;
        }
        FundsSubjectType subjectType = node.getSubjectRef().getSubjectType();
        if (subjectType == FundsSubjectType.CREDIT_ACCOUNT) {
            return RouteParticipantRole.AUTH_HOLDER;
        }
        if (subjectType == FundsSubjectType.BUDGET_GROUP) {
            return RouteParticipantRole.BUDGET_CONTROLLER;
        }
        return fallbackRole;
    }

    private String participantKey(SubjectRef subjectRef, RouteParticipantRole role) {
        return role.name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectType().name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectId();
    }

    private String subjectKey(SubjectRef subjectRef) {
        return subjectRef.getSubjectType().name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectId();
    }

    private String resolveRouteCode(ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD -> FundsRouteCodes.AUTHORIZATION_REVERSAL_REPLAY;
            case AUTHORIZATION_SETTLEMENT -> FundsRouteCodes.AUTHORIZATION_SETTLE_REPLAY;
            case AUTHORIZATION_REFUND -> FundsRouteCodes.AUTHORIZATION_REFUND_REPLAY;
            case REFUND, FEE_REFUND -> FundsRouteCodes.DIRECT_REFUND_REPLAY;
            case CHARGEBACK -> FundsRouteCodes.CHARGEBACK_REPLAY;
            case UNFREEZE -> FundsRouteCodes.BALANCE_UNFREEZE_REPLAY;
        };
    }

    private FundsInstructionType resolveInstructionType(RouteSnapshotSpec snapshot, ReplayRequestSpec replayRequest) {
        return replayRequest.getReplayType() == RouteReplayType.UNFREEZE
                ? FundsInstructionType.BALANCE_CONTROL : snapshot.getInstructionType();
    }

    private FundsTransactionEventType resolveEventType(ReplayRequestSpec replayRequest) {
        FundsTransactionEventType eventType = replayRequest.getEventType();
        if (eventType != null) {
            return eventType;
        }
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD -> FundsTransactionEventType.REVERSAL;
            case AUTHORIZATION_SETTLEMENT -> FundsTransactionEventType.SETTLE;
            case AUTHORIZATION_REFUND -> FundsTransactionEventType.AUTH_REFUND;
            case REFUND -> FundsTransactionEventType.REFUND;
            case FEE_REFUND -> FundsTransactionEventType.FEE_REFUND;
            case CHARGEBACK -> FundsTransactionEventType.CHARGEBACK;
            case UNFREEZE -> FundsTransactionEventType.UNFREEZE;
        };
    }

    private DefaultFundsTransactionType resolveTransactionType(RouteSnapshotSpec snapshot,
                                                              ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case AUTHORIZATION_REFUND, REFUND, FEE_REFUND, CHARGEBACK -> DefaultFundsTransactionType.REFUND;
            default -> snapshot.getTransactionType();
        };
    }
}
