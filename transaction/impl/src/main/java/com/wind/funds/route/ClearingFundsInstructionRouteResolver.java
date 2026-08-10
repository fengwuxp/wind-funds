package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.spec.FundsInstructionFieldKeys;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.wallet.FundsAccountId;
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
 * 清算确认固定路由：同一账户 {@code CLEARING -> AVAILABLE}。
 */
@Component
@AllArgsConstructor
public class ClearingFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && instruction.getEventType() == FundsTransactionEventType.CLEARING_CONFIRM;
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        AssertUtils.equals(FundsTransactionEventType.CLEARING_CONFIRM, instruction.getEventType(),
                "清算资金指令只支持 CLEARING_CONFIRM 事件");
        AssertUtils.equals(DefaultFundsTransactionType.CLEARING, instruction.getTransactionType(),
                "清算资金指令只支持 CLEARING 交易类型");
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        RouteLegSpec leg = routeLeg(FundsRouteLegIds.CLEARING_CONFIRM, 1,
                RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.CLEARING))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.RELEASE)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.NON_REPLAYABLE)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.CLEARING))
                .build();
        RouteParticipantSpec participant = routeParticipantFactory.createParticipant(
                RouteParticipantRole.PAYEE,
                routeSubjectSupport.createSubjectRef(accountId),
                routeSubjectSupport.resolveLedgerProfileCode(accountId).name(),
                instruction.getAmount(),
                instruction.getDescription(),
                Map.of());
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(FundsRouteCodes.CLEARING_CONFIRM_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(List.of(participant))
                .legs(List.of(leg))
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
