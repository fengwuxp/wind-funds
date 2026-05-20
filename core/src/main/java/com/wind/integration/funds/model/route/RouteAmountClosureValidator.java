package com.wind.integration.funds.model.route;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
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

    private static final String FUNDING_ACCOUNT_CLOSURE_MESSAGE =
            "funding account allocation amount must equal consume route amount";

    private RouteAmountClosureValidator() {
    }

    static void validateFundingAccountClosure(List<RouteLegSpec> legs,
                                              @Nullable RoutingDecisionSpec routingDecision) {
        if (routingDecision == null) {
            return;
        }
        Map<CurrencyIsoCode, Long> routeAmounts = sumFundingAccountConsumeLegs(legs);
        if (routeAmounts.isEmpty()) {
            return;
        }
        Map<CurrencyIsoCode, Long> allocationAmounts = sumFundingAccountAllocations(routingDecision);
        if (!routeAmounts.equals(allocationAmounts)) {
            throw new IllegalArgumentException(FUNDING_ACCOUNT_CLOSURE_MESSAGE);
        }
    }

    private static Map<CurrencyIsoCode, Long> sumFundingAccountConsumeLegs(List<RouteLegSpec> legs) {
        Map<CurrencyIsoCode, Long> result = new EnumMap<>(CurrencyIsoCode.class);
        for (RouteLegSpec leg : legs) {
            if (!isFundingAccountConsumeLeg(leg)) {
                continue;
            }
            add(result, leg.getAmount());
        }
        return result;
    }

    private static boolean isFundingAccountConsumeLeg(RouteLegSpec leg) {
        return (leg.getBalanceEffectType() == LedgerBalanceEffectType.CONSUME
                || leg.getBalanceEffectType() == LedgerBalanceEffectType.HOLD)
                && leg.getSourceNode().getSubjectRef().getSubjectType() == FundsSubjectType.FUNDING_ACCOUNT;
    }

    private static Map<CurrencyIsoCode, Long> sumFundingAccountAllocations(RoutingDecisionSpec routingDecision) {
        Map<CurrencyIsoCode, Long> result = new EnumMap<>(CurrencyIsoCode.class);
        for (FundingAllocationDecisionSpec allocation : routingDecision.getFundingAllocations()) {
            if (allocation.getSubjectRef().getSubjectType() != FundsSubjectType.FUNDING_ACCOUNT) {
                continue;
            }
            add(result, allocation.getAmount());
        }
        return result;
    }

    private static void add(Map<CurrencyIsoCode, Long> target, Money amount) {
        target.merge(amount.getCurrency(), amount.getAmount(), Math::addExact);
    }
}
