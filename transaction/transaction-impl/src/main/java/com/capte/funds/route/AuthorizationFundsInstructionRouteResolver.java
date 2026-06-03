package com.capte.funds.route;

import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSpecSupport;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.support.FundsInstructionContextReader;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.capte.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.RouteResolver;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.capte.funds.route.support.RouteSpecSupport.mustNotBeNegative;
import static com.capte.funds.route.support.RouteSpecSupport.routeLeg;
import static com.capte.funds.route.support.RouteSpecSupport.sourceNode;
import static com.capte.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 授权交易 RouteResolver。
 */
@Component
@AllArgsConstructor
public class AuthorizationFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String UNSUPPORTED_EVENT_TYPE_MESSAGE = "unsupported authorization eventType: ";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.AUTHORIZATION_TRANSACTION
                && (instruction.getEventType() == FundsTransactionEventType.AUTHORIZE
                || (instruction.getEventType() == FundsTransactionEventType.SETTLE && isForceSettle(instruction))
                || (instruction.getEventType() == FundsTransactionEventType.AUTH_REFUND
                && isNoAuthRefund(instruction)));
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> resolveAuthorize(instruction);
            case REVERSAL -> resolveReversal(instruction);
            case SETTLE -> isForceSettle(instruction) ? resolveForceSettle(instruction) : resolveSettle(instruction);
            case AUTH_REFUND -> isNoAuthRefund(instruction) ? resolveNoAuthRefund(instruction)
                    : resolveSettleRefund(instruction);
            default -> throw new IllegalArgumentException(UNSUPPORTED_EVENT_TYPE_MESSAGE
                    + instruction.getEventType());
        };
    }

    private ResolvedRouteSpec resolveAuthorize(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        Boolean approved = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.APPROVED);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        List<RouteParticipantSpec> participants = authorizationSubjects.stream()
                .map(subject -> subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                        subject, instruction))
                .toList();
        List<RouteLegSpec> legs = approved ? authorizationLegs(authorizationSubjects, instruction) : List.of();
        PlatformAccountsSnapshotSpec platformAccounts = approved ? settlementAccountSnapshot(instruction) : null;
        return route(instruction, FundsRouteCodes.AUTHORIZATION_STANDARD, participants, legs, platformAccounts);
    }

    private ResolvedRouteSpec resolveReversal(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        List<RouteParticipantSpec> participants = authorizationSubjects.stream()
                .map(subject -> subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                        subject, instruction))
                .toList();
        List<RouteLegSpec> legs = releaseLegs(authorizationSubjects, instruction);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_REVERSAL_STANDARD, participants, legs);
    }

    private ResolvedRouteSpec resolveSettle(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        List<RouteParticipantSpec> participants = new ArrayList<>();
        for (FundsAccountId subject : authorizationSubjects) {
            participants.add(subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                    subject, instruction));
        }
        participants.add(platformParticipant(RouteParticipantRole.PAYEE, settlementAccount, instruction));
        List<RouteLegSpec> legs = captureLegs(authorizationSubjects, settlementAccount, instruction);
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_SETTLE_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveForceSettle(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        List<RouteParticipantSpec> participants = List.of(
                subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                        instruction),
                platformParticipant(RouteParticipantRole.PAYEE, settlementAccount, instruction));
        List<RouteLegSpec> legs = forceSettleLegs(accountId, settlementAccount, instruction);
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_FORCE_SETTLE_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveSettleRefund(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(platformParticipant(RouteParticipantRole.PAYER, settlementAccount, instruction));
        for (FundsAccountId subject : authorizationSubjects) {
            participants.add(subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                    subject, instruction));
        }
        List<RouteLegSpec> legs = refundLegs(authorizationSubjects, settlementAccount, instruction);
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_SETTLE_REFUND_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveNoAuthRefund(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        List<RouteParticipantSpec> participants = List.of(
                platformParticipant(RouteParticipantRole.PAYER, settlementAccount, instruction),
                subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, false), accountId,
                        instruction));
        List<RouteLegSpec> legs = refundLegs(List.of(accountId), settlementAccount, instruction);
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_NO_AUTH_REFUND_STANDARD, participants, legs, snapshot);
    }

    private List<FundsAccountId> resolveAuthorizationSubjects(FundsInstructionSpec instruction,
                                                              FundsAccountId accountId) {
        List<FundsAccountId> subjects = new ArrayList<>();
        subjects.add(accountId);
        FundsAccountId budgetGroupId = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.LINKED_BUDGET_GROUP_ID, FundsAccountId.class);
        if (budgetGroupId != null) {
            subjects.add(budgetGroupId);
        }
        FundsAccountId fundingAccountId = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.LINKED_FUNDING_ACCOUNT_ID, FundsAccountId.class);
        if (fundingAccountId != null) {
            subjects.add(fundingAccountId);
        }
        return subjects.stream().distinct().toList();
    }

    private List<RouteLegSpec> authorizationLegs(List<FundsAccountId> authorizationSubjects,
                                                 FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.AUTHORIZATION_PREFIX + sequence, sequence, RouteLegType.HOLD,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .balanceEffectType(LedgerBalanceEffectType.HOLD)
                    .phaseCode(LedgerPhaseCode.AUTHORIZATION)
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .constraintOverrides(mustNotBeNegative(subject, LedgerSubjectCode.AVAILABLE))
                    .build());
            sequence++;
        }
        return result;
    }

    private List<RouteLegSpec> releaseLegs(List<FundsAccountId> authorizationSubjects,
                                           FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.REVERSAL_PREFIX + sequence, sequence, RouteLegType.RELEASE,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .balanceEffectType(LedgerBalanceEffectType.RELEASE)
                    .phaseCode(LedgerPhaseCode.REVERSAL)
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .constraintOverrides(mustNotBeNegative(subject, LedgerSubjectCode.AUTHORIZATION))
                    .build());
            sequence++;
        }
        return result;
    }

    private List<RouteLegSpec> captureLegs(List<FundsAccountId> authorizationSubjects,
                                           FundsAccountId settlementAccount,
                                           FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        SubjectRef settlementSubject = platformAccountRouteSupport.createSubjectRef(settlementAccount);
        LedgerSubjectCode settlementLedgerSubjectCode = platformAccountRouteSupport.resolveLedgerSubjectCode(
                PlatformFundingAccountRole.SETTLEMENT);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.AUTHORIZATION_SETTLEMENT_PREFIX + sequence, sequence,
                    RouteLegType.CONSUME,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .targetNode(targetNode(settlementSubject, settlementLedgerSubjectCode))
                    .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                    .phaseCode(LedgerPhaseCode.SETTLEMENT)
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .constraintOverrides(mustNotBeNegative(subject, LedgerSubjectCode.AUTHORIZATION))
                    .build());
            sequence++;
        }
        return result;
    }

    private List<RouteLegSpec> forceSettleLegs(FundsAccountId accountId,
                                               FundsAccountId settlementAccount,
                                               FundsInstructionSpec instruction) {
        SubjectRef settlementSubject = platformAccountRouteSupport.createSubjectRef(settlementAccount);
        LedgerSubjectCode settlementLedgerSubjectCode = platformAccountRouteSupport.resolveLedgerSubjectCode(
                PlatformFundingAccountRole.SETTLEMENT);
        RouteLegSpec leg = routeLeg(FundsRouteLegIds.FORCE_SETTLEMENT_PREFIX + 1, 1, RouteLegType.CONSUME,
                instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId),
                        LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(settlementSubject, settlementLedgerSubjectCode))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.NON_REPLAYABLE)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.AVAILABLE))
                .build();
        return List.of(leg);
    }

    private List<RouteLegSpec> refundLegs(List<FundsAccountId> authorizationSubjects,
                                          FundsAccountId settlementAccount,
                                          FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        SubjectRef settlementSubject = platformAccountRouteSupport.createSubjectRef(settlementAccount);
        LedgerSubjectCode settlementLedgerSubjectCode = platformAccountRouteSupport.resolveLedgerSubjectCode(
                PlatformFundingAccountRole.SETTLEMENT);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.SETTLE_REFUND_PREFIX + sequence, sequence, RouteLegType.RESTORE,
                    instruction)
                    .sourceNode(sourceNode(settlementSubject, settlementLedgerSubjectCode))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                    .phaseCode(LedgerPhaseCode.REFUND)
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .build());
            sequence++;
        }
        return result;
    }

    private ResolvedRouteSpec route(FundsInstructionSpec instruction,
                                    String routeCode,
                                    List<RouteParticipantSpec> participants,
                                    List<RouteLegSpec> legs) {
        return route(instruction, routeCode, participants, legs, null);
    }

    private ResolvedRouteSpec route(FundsInstructionSpec instruction,
                                    String routeCode,
                                    List<RouteParticipantSpec> participants,
                                    List<RouteLegSpec> legs,
                                    @Nullable PlatformAccountsSnapshotSpec platformAccounts) {
        List<RouteParticipantSpec> distinctParticipants = routeParticipantFactory.distinct(participants);
        RouteSpecSupport.requireParticipants(distinctParticipants);
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(routeCode)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(distinctParticipants)
                .legs(legs)
                .paymentInstrumentRef(instruction.getInstrumentRef())
                .platformAccounts(platformAccounts)
                .resolvedAt(instruction.getEventTime())
                .description(instruction.getDescription())
                .contextVariables(RouteBenefitSnapshotContextSupport.mergeBenefitSnapshotSummary(instruction))
                .build();
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
    }

    private PlatformAccountsSnapshotSpec settlementAccountSnapshot(FundsInstructionSpec instruction) {
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        return platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
    }

    private RouteParticipantRole resolveAuthorizationParticipantRole(FundsAccountId subject, FundsAccountId primary) {
        if (routeSubjectSupport.isFundingAccount(subject) && !subject.equals(primary)) {
            return RouteParticipantRole.REAL_FUNDING_SOURCE;
        }
        return routeSubjectSupport.resolveParticipantRole(subject, true);
    }

    private RouteParticipantSpec subjectParticipant(RouteParticipantRole role,
                                                    FundsAccountId accountId,
                                                    FundsInstructionSpec instruction) {
        return routeParticipantFactory.createParticipant(role,
                routeSubjectSupport.createSubjectRef(accountId),
                routeSubjectSupport.resolveLedgerProfileCode(accountId).name(), instruction.getAmount(),
                instruction.getDescription(), Map.of());
    }

    private RouteParticipantSpec platformParticipant(RouteParticipantRole role,
                                                     FundsAccountId accountId,
                                                     FundsInstructionSpec instruction) {
        return routeParticipantFactory.createParticipant(role,
                platformAccountRouteSupport.createSubjectRef(accountId),
                platformAccountRouteSupport.resolveLedgerProfileCode(PlatformFundingAccountRole.SETTLEMENT).name(),
                instruction.getAmount(), instruction.getDescription(), Map.of());
    }

    private boolean isForceSettle(FundsInstructionSpec instruction) {
        String settleMode = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.SETTLE_MODE, String.class);
        return FundsAuthorizationTransactionSettleRequest.SETTLE_MODE_FORCE.equalsIgnoreCase(settleMode);
    }

    private boolean isNoAuthRefund(FundsInstructionSpec instruction) {
        String refundMode = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.REFUND_MODE, String.class);
        return FundsInstructionContextKeys.REFUND_MODE_NO_AUTH.equalsIgnoreCase(refundMode);
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
