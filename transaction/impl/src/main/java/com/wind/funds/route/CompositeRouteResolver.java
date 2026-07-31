package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 组合 RouteResolver。
 */
@Component
@Primary
@Slf4j
@AllArgsConstructor
public class CompositeRouteResolver implements RouteResolver, Ordered {

    private final List<RouteResolver> delegates;

    private final RefundRouteAdmission refundRouteAdmission;

    private final RouteFeeChargeAppender routeFeeChargeAppender;

    private final RouteAccountHierarchySnapshotAppender routeAccountHierarchySnapshotAppender;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return true;
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        FundsInstructionType instructionType = instruction.getInstructionType();
        FundsTransactionEventType eventType = instruction.getEventType();
        DefaultFundsTransactionType transactionType = instruction.getTransactionType();
        AssertUtils.notNull(instructionType, "fundsInstruction.instructionType must not be null");
        AssertUtils.notNull(eventType, "fundsInstruction.eventType must not be null");
        AssertUtils.notNull(transactionType, "fundsInstruction.transactionType must not be null");
        AssertUtils.isTrue(DefaultFundsTransactionType.isValidInstructionCombination(
                        instructionType, eventType, transactionType),
                "fundsInstruction instructionType/eventType/transactionType combination is invalid, "
                        + "instructionType = {}, eventType = {}, transactionType = {}",
                instructionType, eventType, transactionType);
        List<RouteResolver> candidates = delegates.stream()
                .filter(delegate -> delegate != this)
                .sorted(Comparator.comparingInt(this::orderOf))
                .filter(delegate -> delegate.supports(instruction))
                .toList();
        AssertUtils.notEmpty(candidates,
                "未找到匹配的 RouteResolver，instructionType = {}, eventType = {}, transactionType = {}",
                instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType());
        AssertUtils.isTrue(candidates.size() == 1,
                "RouteResolver 命中不唯一，instructionType = {}, eventType = {}, transactionType = {}, count = {}",
                instruction.getInstructionType(), instruction.getEventType(), instruction.getTransactionType(),
                candidates.size());
        RouteResolver delegate = candidates.getFirst();
        log.debug("resolved route by resolver, resolver = {}, instructionType = {}, eventType = {}, transactionType = {}, businessSn = {}",
                delegate.getClass().getSimpleName(), instruction.getInstructionType(), instruction.getEventType(),
                instruction.getTransactionType(), instruction.getBusinessSn());
        ResolvedRouteSpec result = delegate.resolve(instruction);
        refundRouteAdmission.validate(instruction, result);
        ResolvedRouteSpec routeWithFee = routeFeeChargeAppender.append(instruction, result);
        return routeAccountHierarchySnapshotAppender.append(instruction, routeWithFee);
    }

    private int orderOf(RouteResolver routeResolver) {
        return routeResolver instanceof Ordered ordered ? ordered.getOrder() : 0;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
