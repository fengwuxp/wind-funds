package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
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

import static com.wind.funds.route.support.RouteSpecSupport.routeLeg;
import static com.wind.funds.route.support.RouteSpecSupport.sourceNode;
import static com.wind.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 结算锁定固定路由：同一账户 {@code AVAILABLE -> SETTLEMENT}。
 */
@Component
@AllArgsConstructor
public class SettlementFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && (instruction.getEventType() == FundsTransactionEventType.SETTLEMENT_LOCK
                || instruction.getEventType() == FundsTransactionEventType.SETTLEMENT_RELEASE);
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        AssertUtils.isTrue(instruction.getEventType() == FundsTransactionEventType.SETTLEMENT_LOCK
                        || instruction.getEventType() == FundsTransactionEventType.SETTLEMENT_RELEASE,
                "结算资金指令只支持 SETTLEMENT_LOCK 或 SETTLEMENT_RELEASE 事件");
        AssertUtils.equals(DefaultFundsTransactionType.SETTLEMENT, instruction.getTransactionType(),
                "结算锁定资金指令只支持 SETTLEMENT 交易类型");
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        boolean release = instruction.getEventType() == FundsTransactionEventType.SETTLEMENT_RELEASE;
        RouteLegSpec leg = routeLeg(release ? FundsRouteLegIds.SETTLEMENT_RELEASE : FundsRouteLegIds.SETTLEMENT_LOCK, 1,
                release ? RouteLegType.RELEASE : RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId)))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(accountId)))
                .replayPolicy(RouteReplayPolicy.NON_REPLAYABLE)
                .build();
        RouteParticipantSpec participant = routeParticipantFactory.createParticipant(
                RouteParticipantRole.PAYER,
                routeSubjectSupport.createSubjectRef(accountId),
                routeSubjectSupport.resolveLedgerProfileCode(instruction.getTenantId(), accountId).name(),
                instruction.getAmount(),
                instruction.getDescription(),
                Map.of());
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(release ? FundsRouteCodes.SETTLEMENT_RELEASE_STANDARD
                        : FundsRouteCodes.SETTLEMENT_LOCK_STANDARD)
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
