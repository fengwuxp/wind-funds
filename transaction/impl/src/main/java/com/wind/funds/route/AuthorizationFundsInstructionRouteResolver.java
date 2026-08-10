package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionFieldKeys;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wind.funds.route.support.RouteSpecSupport.mustNotBeNegative;
import static com.wind.funds.route.support.RouteSpecSupport.routeLeg;
import static com.wind.funds.route.support.RouteSpecSupport.sourceNode;
import static com.wind.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 授权交易 RouteResolver。
 */
@Component
public class AuthorizationFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String UNSUPPORTED_EVENT_TYPE_MESSAGE = "unsupported authorization eventType: ";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    @Autowired
    public AuthorizationFundsInstructionRouteResolver(RouteParticipantFactory routeParticipantFactory,
                                                      RouteSubjectSupport routeSubjectSupport,
                                                      PlatformAccountRouteSupport platformAccountRouteSupport) {
        this.routeParticipantFactory = routeParticipantFactory;
        this.routeSubjectSupport = routeSubjectSupport;
        this.platformAccountRouteSupport = platformAccountRouteSupport;
    }

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.AUTHORIZATION_TRANSACTION
                && (instruction.getEventType() == FundsTransactionEventType.AUTHORIZE
                || (instruction.getEventType() == FundsTransactionEventType.COMPLETE && isForceCompletion(instruction))
                || (instruction.getEventType() == FundsTransactionEventType.AUTH_REFUND
                && isNoAuthRefund(instruction)));
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> resolveAuthorize(instruction);
            case REVERSAL -> resolveReversal(instruction);
            case COMPLETE -> isForceCompletion(instruction) ? resolveForceCompletion(instruction) : resolveComplete(instruction);
            case AUTH_REFUND -> isNoAuthRefund(instruction) ? resolveNoAuthRefund(instruction)
                    : resolveRefund(instruction);
            default -> throw new IllegalArgumentException(UNSUPPORTED_EVENT_TYPE_MESSAGE
                    + instruction.getEventType());
        };
    }

    private ResolvedRouteSpec resolveAuthorize(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        Boolean approved = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.APPROVED);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        List<RouteParticipantSpec> participants = authorizationSubjects.stream()
                .map(subject -> subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                        subject, instruction))
                .toList();
        List<RouteLegSpec> legs = approved ? authorizationLegs(authorizationSubjects, instruction) : List.of();
        PlatformAccountsSnapshotSpec platformAccounts = approved ? settlementAccountSnapshot(instruction) : null;
        return route(instruction,
                FundsRouteCodes.AUTHORIZATION_STANDARD,
                participants,
                legs,
                platformAccounts);
    }

    private ResolvedRouteSpec resolveReversal(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        List<FundsAccountId> authorizationSubjects = resolveAuthorizationSubjects(instruction, accountId);
        List<RouteParticipantSpec> participants = authorizationSubjects.stream()
                .map(subject -> subjectParticipant(resolveAuthorizationParticipantRole(subject, accountId),
                        subject, instruction))
                .toList();
        List<RouteLegSpec> legs = releaseLegs(authorizationSubjects, instruction);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_REVERSAL_STANDARD, participants, legs);
    }

    private ResolvedRouteSpec resolveComplete(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
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
        return route(instruction, FundsRouteCodes.AUTHORIZATION_COMPLETE_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveForceCompletion(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        FundsAccountId settlementAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.SETTLEMENT);
        List<RouteParticipantSpec> participants = List.of(
                subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                        instruction),
                platformParticipant(RouteParticipantRole.PAYEE, settlementAccount, instruction));
        List<RouteLegSpec> legs = forceCompletionLegs(accountId, settlementAccount, instruction);
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createSettlementSnapshot(settlementAccount);
        return route(instruction, FundsRouteCodes.AUTHORIZATION_FORCE_COMPLETION_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveRefund(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
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
        return route(instruction, FundsRouteCodes.AUTHORIZATION_REFUND_STANDARD, participants, legs, snapshot);
    }

    private ResolvedRouteSpec resolveNoAuthRefund(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
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
        FundsAccountId fundingAccountId = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionFieldKeys.LINKED_FUNDING_ACCOUNT_ID, FundsAccountId.class);
        if (fundingAccountId != null) {
            subjects.add(fundingAccountId);
        }
        return subjects.stream().distinct().toList();
    }

    private List<RouteLegSpec> authorizationLegs(List<FundsAccountId> authorizationSubjects,
                                                 FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        RoutePeriod period = routePeriod(instruction);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.AUTHORIZATION_PREFIX + sequence, sequence, RouteLegType.HOLD,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .balanceEffectType(LedgerBalanceEffectType.HOLD)
                    .phaseCode(LedgerPhaseCode.AUTHORIZATION)
                    .periodType(period.periodType())
                    .periodId(period.periodId())
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
        RoutePeriod period = routePeriod(instruction);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.REVERSAL_PREFIX + sequence, sequence, RouteLegType.RELEASE,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .balanceEffectType(LedgerBalanceEffectType.RELEASE)
                    .phaseCode(LedgerPhaseCode.REVERSAL)
                    .periodType(period.periodType())
                    .periodId(period.periodId())
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
        RoutePeriod period = routePeriod(instruction);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.AUTHORIZATION_COMPLETION_PREFIX + sequence, sequence,
                    RouteLegType.CONSUME,
                    instruction)
                    .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(subject),
                            LedgerSubjectCode.AUTHORIZATION))
                    .targetNode(targetNode(settlementSubject, settlementLedgerSubjectCode))
                    .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                    .phaseCode(LedgerPhaseCode.COMPLETION)
                    .periodType(period.periodType())
                    .periodId(period.periodId())
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .constraintOverrides(mustNotBeNegative(subject, LedgerSubjectCode.AUTHORIZATION))
                    .build());
            sequence++;
        }
        return result;
    }

    private List<RouteLegSpec> forceCompletionLegs(FundsAccountId accountId,
                                               FundsAccountId settlementAccount,
                                               FundsInstructionSpec instruction) {
        SubjectRef settlementSubject = platformAccountRouteSupport.createSubjectRef(settlementAccount);
        LedgerSubjectCode settlementLedgerSubjectCode = platformAccountRouteSupport.resolveLedgerSubjectCode(
                PlatformFundingAccountRole.SETTLEMENT);
        RouteLegSpec leg = routeLeg(FundsRouteLegIds.FORCE_COMPLETION_PREFIX + 1, 1, RouteLegType.CONSUME,
                instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId),
                        LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(settlementSubject, settlementLedgerSubjectCode))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.COMPLETION)
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
        RoutePeriod period = routePeriod(instruction);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(FundsRouteLegIds.AUTHORIZATION_REFUND_PREFIX + sequence, sequence, RouteLegType.RESTORE,
                    instruction)
                    .sourceNode(sourceNode(settlementSubject, settlementLedgerSubjectCode))
                    .targetNode(targetNode(routeSubjectSupport.createSubjectRef(subject), LedgerSubjectCode.AVAILABLE))
                    .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                    .phaseCode(LedgerPhaseCode.REFUND)
                    .periodType(period.periodType())
                    .periodId(period.periodId())
                    .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                    .build());
            sequence++;
        }
        return result;
    }

    private RoutePeriod routePeriod(FundsInstructionSpec instruction) {
        AccountBalancePeriodType periodType = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionFieldKeys.LEDGER_PERIOD_TYPE, AccountBalancePeriodType.class);
        String periodId = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionFieldKeys.LEDGER_PERIOD_ID, String.class);
        if (periodType == null) {
            AssertUtils.isTrue(periodId == null, "账本周期类型不能为空，periodId = {}", periodId);
            return new RoutePeriod(AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
        }
        if (periodType == AccountBalancePeriodType.LIFETIME) {
            return new RoutePeriod(AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
        }
        AssertUtils.hasText(periodId, "非生命周期账本周期 periodId 不能为空，periodType = {}", periodType);
        return new RoutePeriod(periodType, periodId);
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
                .contextVariables(Map.of())
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

    private boolean isForceCompletion(FundsInstructionSpec instruction) {
        String completionMode = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.COMPLETION_MODE, String.class);
        return FundsAuthorizationTransactionCompleteRequest.COMPLETION_MODE_FORCE.equalsIgnoreCase(completionMode);
    }

    private boolean isNoAuthRefund(FundsInstructionSpec instruction) {
        String refundMode = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.REFUND_MODE, String.class);
        if (FundsInstructionContextKeys.REFUND_MODE_NO_AUTH.equalsIgnoreCase(refundMode)) {
            return true;
        }
        if (refundMode != null) {
            return false;
        }
        return isExternalTransactionReference(instruction);
    }

    private boolean isExternalTransactionReference(FundsInstructionSpec instruction) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        return reference != null
                && reference.getReferenceType() == FundsInstructionReferenceType.EXTERNAL_TRANSACTION;
    }

    @Override
    public int getOrder() {
        return 20;
    }

    private record RoutePeriod(AccountBalancePeriodType periodType, String periodId) {
    }
}
