package com.capte.funds.route.support;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Route spec 构建辅助。
 */
public final class RouteSpecSupport {

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private static final String PARTICIPANTS_REQUIRED_MESSAGE = "ResolvedRoute participants 不能为空";

    private static final String ROUTE_CODE_REQUIRED_MESSAGE = "ResolvedRoute routeCode 不能为空";

    private static final String ROUTE_VERSION_REQUIRED_MESSAGE = "ResolvedRoute routeVersion 不能为空";

    private static final String BUSINESS_SCENE_REQUIRED_MESSAGE = "ResolvedRoute businessScene 不能为空";

    private static final String BUSINESS_SN_REQUIRED_MESSAGE = "ResolvedRoute businessSn 不能为空";

    private static final String INSTRUCTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute instructionType 不能为空";

    private static final String EVENT_TYPE_REQUIRED_MESSAGE = "ResolvedRoute eventType 不能为空";

    private static final String TRANSACTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute transactionType 不能为空";

    private static final String RESOLVED_AT_REQUIRED_MESSAGE = "ResolvedRoute resolvedAt 不能为空";

    private RouteSpecSupport() {
    }

    public static void requireParticipants(@NonNull List<RouteParticipantSpec> participants) {
        AssertUtils.notEmpty(participants, PARTICIPANTS_REQUIRED_MESSAGE);
    }

    public static void validateResolvedRoute(@NonNull ResolvedRouteSpec route) {
        AssertUtils.hasText(route.getRouteCode(), ROUTE_CODE_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getRouteVersion(), ROUTE_VERSION_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getBusinessScene(), BUSINESS_SCENE_REQUIRED_MESSAGE);
        AssertUtils.hasText(route.getBusinessSn(), BUSINESS_SN_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getInstructionType(), INSTRUCTION_TYPE_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getEventType(), EVENT_TYPE_REQUIRED_MESSAGE);
        AssertUtils.notNull(route.getTransactionType(), TRANSACTION_TYPE_REQUIRED_MESSAGE);
        requireParticipants(route.getParticipants());
        AssertUtils.notNull(route.getResolvedAt(), RESOLVED_AT_REQUIRED_MESSAGE);
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
