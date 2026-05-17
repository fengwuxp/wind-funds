package com.capte.funds.route.support;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Route spec 构建辅助。
 */
public final class RouteSpecSupport {

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private RouteSpecSupport() {
    }

    public static ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(@NonNull String legId,
                                                                              int sequence,
                                                                              @NonNull RouteLegType legType,
                                                                              @NonNull FundsInstructionSpec instruction) {
        return routeLeg(legId, sequence, legType, instruction.getAmount(), instruction.getOriginalAmount(),
                instruction.getExchangeRate(), instruction.getDescription());
    }

    public static ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(@NonNull String legId,
                                                                              int sequence,
                                                                              @NonNull RouteLegType legType,
                                                                              @NonNull Money amount,
                                                                              String description) {
        return routeLeg(legId, sequence, legType, amount, amount, BigDecimal.ONE, description);
    }

    public static ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(@NonNull String legId,
                                                                              int sequence,
                                                                              @NonNull RouteLegType legType,
                                                                              @NonNull Money amount,
                                                                              @NonNull Money originalAmount,
                                                                              @NonNull BigDecimal exchangeRate,
                                                                              String description) {
        return ImmutableRouteLegSpec.builder()
                .legId(legId)
                .sequence(sequence)
                .legType(legType)
                .amount(amount)
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .periodType(AccountBalancePeriodType.LIFETIME)
                .periodId(AccountBalancePeriodType.LIFETIME.name())
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .constraintOverrides(Map.of())
                .description(description)
                .contextVariables(Map.of());
    }

    public static RouteNodeSpec sourceNode(@NonNull SubjectRef subjectRef,
                                           @NonNull LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.SOURCE);
    }

    public static RouteNodeSpec targetNode(@NonNull SubjectRef subjectRef,
                                           @NonNull LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.TARGET);
    }

    public static RouteNodeSpec routeNode(@NonNull SubjectRef subjectRef,
                                          @NonNull LedgerSubjectCode ledgerSubjectCode,
                                          @NonNull RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    public static Map<String, LedgerBalanceConstraintType> mustNotBeNegative(
            @NonNull FundsAccountId accountId,
            @NonNull LedgerSubjectCode subjectCode) {
        return balanceConstraint(accountId, subjectCode, LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    public static Map<String, LedgerBalanceConstraintType> balanceConstraint(
            @NonNull FundsAccountId accountId,
            @NonNull LedgerSubjectCode subjectCode,
            @NonNull LedgerBalanceConstraintType constraintType) {
        return Map.of(accountId.type() + CONSTRAINT_KEY_SEPARATOR
                        + accountId.id() + CONSTRAINT_KEY_SEPARATOR
                        + subjectCode.name(),
                constraintType);
    }
}
