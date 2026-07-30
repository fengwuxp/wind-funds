package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.spec.transaction.FundsInstructionFieldKeys;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.wind.funds.route.support.RouteSpecSupport.mustNotBeNegative;
import static com.wind.funds.route.support.RouteSpecSupport.routeLeg;
import static com.wind.funds.route.support.RouteSpecSupport.sourceNode;
import static com.wind.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 出款资金结果固定路由。W2 不启用账本在途桶。
 */
@Component
@AllArgsConstructor
public class PayoutFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && (instruction.getEventType() == FundsTransactionEventType.PAYOUT_SUCCEEDED
                || instruction.getEventType() == FundsTransactionEventType.PAYOUT_FAILED);
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        AssertUtils.equals(DefaultFundsTransactionType.PAYOUT, instruction.getTransactionType(),
                "出款资金指令只支持 PAYOUT 交易类型");
        return instruction.getEventType() == FundsTransactionEventType.PAYOUT_SUCCEEDED
                ? successRoute(instruction) : failureRoute(instruction);
    }

    private ResolvedRouteSpec successRoute(FundsInstructionSpec instruction) {
        FundsAccountId accountId = accountId(instruction);
        FundsAccountId cashAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.CASH_MAPPING);
        FundsAccountId prepaymentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.PREPAYMENT);
        var accountSubject = routeSubjectSupport.createSubjectRef(accountId);
        var cashSubject = platformAccountRouteSupport.createSubjectRef(cashAccount);
        var prepaymentSubject = platformAccountRouteSupport.createSubjectRef(prepaymentAccount);
        List<RouteLegSpec> legs = List.of(
                routeLeg(FundsRouteLegIds.PAYOUT_SETTLEMENT, 1, RouteLegType.CONSUME, instruction)
                        .sourceNode(sourceNode(accountSubject, LedgerSubjectCode.SETTLEMENT))
                        .targetNode(targetNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                        .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                        .phaseCode(LedgerPhaseCode.SETTLEMENT)
                        .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.SETTLEMENT))
                        .build(),
                routeLeg(FundsRouteLegIds.PAYOUT_FUND_OUT, 2, RouteLegType.EXTERNAL_OUT, instruction)
                        .sourceNode(sourceNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                        .targetNode(targetNode(cashSubject, LedgerSubjectCode.CASH))
                        .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                        .phaseCode(LedgerPhaseCode.FUND_OUT)
                        .build());
        List<RouteParticipantSpec> participants = List.of(
                routeParticipantFactory.createParticipant(RouteParticipantRole.PAYER, accountSubject,
                        routeSubjectSupport.resolveLedgerProfileCode(accountId).name(), instruction.getAmount(),
                        instruction.getDescription(), Map.of()),
                routeParticipantFactory.createParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                        prepaymentSubject, PlatformFundingAccountRole.PREPAYMENT.getLedgerProfileCode().name(),
                        instruction.getAmount(), instruction.getDescription(), Map.of()),
                routeParticipantFactory.createParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                        cashSubject, PlatformFundingAccountRole.CASH_MAPPING.getLedgerProfileCode().name(),
                        instruction.getAmount(), instruction.getDescription(), Map.of()));
        return route(instruction, FundsRouteCodes.PAYOUT_SUCCESS_STANDARD, participants, legs,
                platformAccountRouteSupport.createExternalFundMovementSnapshot(cashAccount, prepaymentAccount, null));
    }

    private ResolvedRouteSpec failureRoute(FundsInstructionSpec instruction) {
        FundsAccountId accountId = accountId(instruction);
        var subject = routeSubjectSupport.createSubjectRef(accountId);
        RouteLegSpec leg = routeLeg(FundsRouteLegIds.PAYOUT_FAILURE_RETURN, 1, RouteLegType.RESTORE, instruction)
                .sourceNode(sourceNode(subject, LedgerSubjectCode.SETTLEMENT))
                .targetNode(targetNode(subject, LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                .phaseCode(LedgerPhaseCode.REFUND)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.SETTLEMENT))
                .build();
        RouteParticipantSpec participant = routeParticipantFactory.createParticipant(RouteParticipantRole.PAYER,
                subject, routeSubjectSupport.resolveLedgerProfileCode(accountId).name(), instruction.getAmount(),
                instruction.getDescription(), Map.of());
        return route(instruction, FundsRouteCodes.PAYOUT_FAILURE_RETURN, List.of(participant), List.of(leg), null);
    }

    private FundsAccountId accountId(FundsInstructionSpec instruction) {
        return FundsInstructionContextReader.requireFundsAccountId(instruction, FundsInstructionFieldKeys.ACCOUNT_ID);
    }

    private ResolvedRouteSpec route(FundsInstructionSpec instruction,
                                    String routeCode,
                                    List<RouteParticipantSpec> participants,
                                    List<RouteLegSpec> legs,
                                    com.wind.funds.route.spec.PlatformAccountsSnapshotSpec platformAccounts) {
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(routeCode)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(participants)
                .legs(legs)
                .platformAccounts(platformAccounts)
                .resolvedAt(instruction.getEventTime())
                .description(instruction.getDescription())
                .contextVariables(Map.of())
                .build();
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
