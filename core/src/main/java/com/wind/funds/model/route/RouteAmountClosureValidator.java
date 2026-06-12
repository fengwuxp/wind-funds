package com.wind.funds.model.route;

import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Route amount closure checks shared by runtime routes and persisted snapshots.
 */
final class RouteAmountClosureValidator {

    private static final String CORE_ACCOUNT_CLOSURE_MESSAGE =
            "core account allocation amount must equal consume route amount";
    private static final String CORE_ACCOUNT_ROUTE_AMOUNT_OVERFLOW_MESSAGE =
            "core account route amount sum overflow";
    private static final String CORE_ACCOUNT_ALLOCATION_AMOUNT_OVERFLOW_MESSAGE =
            "core account allocation amount sum overflow";

    private RouteAmountClosureValidator() {
    }

    static void validateCoreAccountClosure(List<RouteLegSpec> legs,
                                           @Nullable RoutingDecisionSpec routingDecision) {
        if (routingDecision == null) {
            return;
        }
        Map<CurrencyIsoCode, Long> routeAmounts = sumCoreAccountConsumeLegs(legs);
        if (routeAmounts.isEmpty()) {
            return;
        }
        Map<CurrencyIsoCode, Long> allocationAmounts = sumCoreAccountAllocations(routingDecision);
        if (!routeAmounts.equals(allocationAmounts)) {
            throw new IllegalArgumentException(CORE_ACCOUNT_CLOSURE_MESSAGE);
        }
    }

    private static Map<CurrencyIsoCode, Long> sumCoreAccountConsumeLegs(List<RouteLegSpec> legs) {
        Map<CurrencyIsoCode, Long> result = new EnumMap<>(CurrencyIsoCode.class);
        for (RouteLegSpec leg : legs) {
            if (!isCoreAccountConsumeLeg(leg)) {
                continue;
            }
            add(result, leg.getAmount(), CORE_ACCOUNT_ROUTE_AMOUNT_OVERFLOW_MESSAGE);
        }
        return result;
    }

    private static boolean isCoreAccountConsumeLeg(RouteLegSpec leg) {
        return (leg.getBalanceEffectType() == LedgerBalanceEffectType.CONSUME
                || leg.getBalanceEffectType() == LedgerBalanceEffectType.HOLD)
                && isCoreAccount(leg.getSourceNode().getSubjectRef().getSubjectType());
    }

    private static Map<CurrencyIsoCode, Long> sumCoreAccountAllocations(RoutingDecisionSpec routingDecision) {
        Map<CurrencyIsoCode, Long> result = new EnumMap<>(CurrencyIsoCode.class);
        for (FundingAllocationDecisionSpec allocation : routingDecision.getFundingAllocations()) {
            if (!isCoreAccount(allocation.getSubjectRef().getSubjectType())) {
                continue;
            }
            add(result, allocation.getAmount(), CORE_ACCOUNT_ALLOCATION_AMOUNT_OVERFLOW_MESSAGE);
        }
        return result;
    }

    private static boolean isCoreAccount(FundsSubjectType subjectType) {
        return subjectType == FundsSubjectType.FUNDING_ACCOUNT || subjectType == FundsSubjectType.CREDIT_ACCOUNT;
    }

    private static void add(Map<CurrencyIsoCode, Long> target, Money amount, String overflowMessage) {
        try {
            target.merge(amount.getCurrency(), amount.getAmount(), Math::addExact);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(overflowMessage, ex);
        }
    }
}
