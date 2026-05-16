package com.capte.funds.route;

import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.support.FundsInstructionContextReader;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 授权交易 RouteResolver。
 */
@Component
@AllArgsConstructor
public class AuthorizationFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String LEG_AUTHORIZATION_PREFIX = "AUTHORIZATION_";

    private static final String LEG_REVERSAL_PREFIX = "REVERSAL_";

    private static final String LEG_AUTHORIZATION_SETTLEMENT_PREFIX = "AUTHORIZATION_SETTLEMENT_";

    private static final String LEG_SETTLE_REFUND_PREFIX = "SETTLE_REFUND_";

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private static final String UNSUPPORTED_EVENT_TYPE_MESSAGE = "unsupported authorization eventType: ";

    private static final String PARTICIPANTS_REQUIRED_MESSAGE = "ResolvedRoute participants 不能为空";

    private static final String ROUTE_CODE_REQUIRED_MESSAGE = "ResolvedRoute routeCode 不能为空";

    private static final String ROUTE_VERSION_REQUIRED_MESSAGE = "ResolvedRoute routeVersion 不能为空";

    private static final String BUSINESS_SCENE_REQUIRED_MESSAGE = "ResolvedRoute businessScene 不能为空";

    private static final String BUSINESS_SN_REQUIRED_MESSAGE = "ResolvedRoute businessSn 不能为空";

    private static final String INSTRUCTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute instructionType 不能为空";

    private static final String EVENT_TYPE_REQUIRED_MESSAGE = "ResolvedRoute eventType 不能为空";

    private static final String TRANSACTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute transactionType 不能为空";

    private static final String RESOLVED_AT_REQUIRED_MESSAGE = "ResolvedRoute resolvedAt 不能为空";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.AUTHORIZATION_TRANSACTION
                && !RouteReplaySupport.isReplayInstruction(instruction);
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case AUTHORIZE -> resolveAuthorize(instruction);
            case REVERSAL -> resolveReversal(instruction);
            case SETTLE -> resolveSettle(instruction);
            case AUTH_REFUND -> resolveSettleRefund(instruction);
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
            result.add(routeLeg(LEG_AUTHORIZATION_PREFIX + sequence, sequence, RouteLegType.HOLD,
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
            result.add(routeLeg(LEG_REVERSAL_PREFIX + sequence, sequence, RouteLegType.RELEASE,
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
            result.add(routeLeg(LEG_AUTHORIZATION_SETTLEMENT_PREFIX + sequence, sequence, RouteLegType.CONSUME,
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

    private List<RouteLegSpec> refundLegs(List<FundsAccountId> authorizationSubjects,
                                          FundsAccountId settlementAccount,
                                          FundsInstructionSpec instruction) {
        List<RouteLegSpec> result = new ArrayList<>(authorizationSubjects.size());
        int sequence = 1;
        SubjectRef settlementSubject = platformAccountRouteSupport.createSubjectRef(settlementAccount);
        LedgerSubjectCode settlementLedgerSubjectCode = platformAccountRouteSupport.resolveLedgerSubjectCode(
                PlatformFundingAccountRole.SETTLEMENT);
        for (FundsAccountId subject : authorizationSubjects) {
            result.add(routeLeg(LEG_SETTLE_REFUND_PREFIX + sequence, sequence, RouteLegType.RESTORE,
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
        AssertUtils.isTrue(!distinctParticipants.isEmpty(), PARTICIPANTS_REQUIRED_MESSAGE);
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
                .contextVariables(instruction.getContextVariables())
                .build();
        validate(result);
        return result;
    }

    private void validate(ResolvedRouteSpec route) {
        AssertUtils.hasText(route.getRouteCode(), ROUTE_CODE_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getRouteVersion(), ROUTE_VERSION_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getBusinessScene(), BUSINESS_SCENE_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getBusinessSn(), BUSINESS_SN_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getInstructionType(), INSTRUCTION_TYPE_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getEventType(), EVENT_TYPE_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getTransactionType(), TRANSACTION_TYPE_REQUIRED_MESSAGE);
        AssertUtils.isTrue(!route.getParticipants().isEmpty(), PARTICIPANTS_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getResolvedAt(), RESOLVED_AT_REQUIRED_MESSAGE);
    }

    private ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(String legId,
                                                                        int sequence,
                                                                        RouteLegType legType,
                                                                        FundsInstructionSpec instruction) {
        return ImmutableRouteLegSpec.builder()
                .legId(legId)
                .sequence(sequence)
                .legType(legType)
                .amount(instruction.getAmount())
                .originalAmount(instruction.getOriginalAmount())
                .exchangeRate(instruction.getExchangeRate())
                .periodType(AccountBalancePeriodType.LIFETIME)
                .periodId(AccountBalancePeriodType.LIFETIME.name())
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .constraintOverrides(Map.of())
                .description(instruction.getDescription())
                .contextVariables(Map.of());
    }

    private RouteNodeSpec sourceNode(SubjectRef subjectRef, LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.SOURCE);
    }

    private RouteNodeSpec targetNode(SubjectRef subjectRef, LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.TARGET);
    }

    private RouteNodeSpec routeNode(SubjectRef subjectRef,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
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

    private Map<String, LedgerBalanceConstraintType> mustNotBeNegative(FundsAccountId accountId,
                                                                       LedgerSubjectCode subjectCode) {
        return Map.of(accountId.type() + CONSTRAINT_KEY_SEPARATOR
                        + accountId.id() + CONSTRAINT_KEY_SEPARATOR
                        + subjectCode.name(),
                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
