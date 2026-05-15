package com.capte.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
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

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return true;
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        List<RouteResolver> candidates = delegates.stream()
                .filter(delegate -> delegate != this)
                .sorted(Comparator.comparingInt(this::orderOf))
                .filter(delegate -> delegate.supports(instruction))
                .toList();
        AssertUtils.isTrue(!candidates.isEmpty(),
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
        return delegate.resolve(instruction);
    }

    private int orderOf(RouteResolver routeResolver) {
        return routeResolver instanceof Ordered ordered ? ordered.getOrder() : 0;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
