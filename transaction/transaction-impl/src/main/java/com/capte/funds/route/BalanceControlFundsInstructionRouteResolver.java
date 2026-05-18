package com.capte.funds.route;

import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSpecSupport;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.support.FundsInstructionContextReader;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 余额控制 RouteResolver。
 */
@Component
@AllArgsConstructor
public class BalanceControlFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String LEG_FREEZE = "FREEZE";

    private static final String LEG_UNFREEZE = "UNFREEZE";

    private static final String LEG_FUNDING_BALANCE_ADJUST = "FUNDING_BALANCE_ADJUST";

    private static final String LEG_LIMIT_ADJUST = "LIMIT_ADJUST";

    private static final String UNSUPPORTED_EVENT_TYPE_MESSAGE = "unsupported balance-control eventType: ";

    private static final String UNSUPPORTED_ADJUST_SUBJECT_TYPE_MESSAGE = "unsupported adjust subject type: ";

    private static final String PARTICIPANTS_REQUIRED_MESSAGE = "ResolvedRoute participants 不能为空";

    private static final String ROUTE_CODE_REQUIRED_MESSAGE = "ResolvedRoute routeCode 不能为空";

    private static final String ROUTE_VERSION_REQUIRED_MESSAGE = "ResolvedRoute routeVersion 不能为空";

    private static final String BUSINESS_SCENE_REQUIRED_MESSAGE = "ResolvedRoute businessScene 不能为空";

    private static final String BUSINESS_SN_REQUIRED_MESSAGE = "ResolvedRoute businessSn 不能为空";

    private static final String INSTRUCTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute instructionType 不能为空";

    private static final String EVENT_TYPE_REQUIRED_MESSAGE = "ResolvedRoute eventType 不能为空";

    private static final String TRANSACTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute transactionType 不能为空";

    private static final String RESOLVED_AT_REQUIRED_MESSAGE = "ResolvedRoute resolvedAt 不能为空";

    private static final String ALLOW_NEGATIVE_BALANCE_POLICY_REQUIRED_MESSAGE =
            "受控负余额调账缺少策略编码";

    private static final String ALLOW_NEGATIVE_BALANCE_APPROVAL_REQUIRED_MESSAGE =
            "受控负余额调账缺少审批或风控依据";

    private static final String ALLOW_NEGATIVE_BALANCE_REASON_REQUIRED_MESSAGE =
            "受控负余额调账缺少原因";

    private static final String ALLOW_NEGATIVE_BALANCE_RISK_STATUS_REQUIRED_MESSAGE =
            "受控负余额调账缺少风险状态";

    private static final String ALLOW_NEGATIVE_BALANCE_SINGLE_LIMIT_REQUIRED_MESSAGE =
            "受控负余额调账缺少单笔上限";

    private static final String ALLOW_NEGATIVE_BALANCE_CUMULATIVE_LIMIT_REQUIRED_MESSAGE =
            "受控负余额调账缺少累计上限";

    private static final String ALLOW_NEGATIVE_BALANCE_AGING_REQUIRED_MESSAGE =
            "受控负余额调账缺少账龄起点";

    private static final String BUDGET_PERIOD_REQUIRED_MESSAGE =
            "预算受控负余额调账缺少预算周期";

    private static final String BUDGET_GOVERNANCE_POLICY_REQUIRED_MESSAGE =
            "预算受控负余额调账缺少治理策略";

    private static final String BUDGET_REPORT_MARKER_REQUIRED_MESSAGE =
            "预算受控负余额调账缺少报表标记";

    private static final String ALLOW_NEGATIVE_BALANCE_LIMIT_CURRENCY_MESSAGE =
            "受控负余额调账上限币种必须与本次金额币种一致";

    private static final String ALLOW_NEGATIVE_BALANCE_LIMIT_AMOUNT_MESSAGE =
            "受控负余额调账上限必须大于 0";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.BALANCE_CONTROL
                && !RouteReplaySupport.isReplayInstruction(instruction);
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case FREEZE -> resolveFreeze(instruction);
            case UNFREEZE -> resolveUnfreeze(instruction);
            case BALANCE_ADJUST, LIMIT_ADJUST -> resolveAdjust(instruction);
            default -> throw new IllegalArgumentException(UNSUPPORTED_EVENT_TYPE_MESSAGE
                    + instruction.getEventType());
        };
    }

    private ResolvedRouteSpec resolveFreeze(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<RouteLegSpec> legs = List.of(RouteSpecSupport.routeLeg(LEG_FREEZE, 1, RouteLegType.HOLD, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.FROZEN))
                .balanceEffectType(LedgerBalanceEffectType.HOLD)
                .phaseCode(LedgerPhaseCode.FREEZE)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .constraintOverrides(RouteSpecSupport.mustNotBeNegative(accountId, LedgerSubjectCode.AVAILABLE))
                .build());
        List<RouteParticipantSpec> participants = List.of(subjectParticipant(accountId, instruction));
        return route(instruction, FundsRouteCodes.BALANCE_FREEZE_STANDARD, participants, legs);
    }

    private ResolvedRouteSpec resolveUnfreeze(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<RouteLegSpec> legs = List.of(RouteSpecSupport.routeLeg(LEG_UNFREEZE, 1, RouteLegType.RELEASE, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.FROZEN))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.RELEASE)
                .phaseCode(LedgerPhaseCode.UNFREEZE)
                .constraintOverrides(RouteSpecSupport.mustNotBeNegative(accountId, LedgerSubjectCode.FROZEN))
                .build());
        List<RouteParticipantSpec> participants = List.of(subjectParticipant(accountId, instruction));
        return route(instruction, FundsRouteCodes.BALANCE_UNFREEZE_STANDARD, participants, legs);
    }

    private ResolvedRouteSpec resolveAdjust(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        if (routeSubjectSupport.isFundingAccount(accountId)) {
            return resolveFundingBalanceAdjust(instruction, accountId);
        }
        if (routeSubjectSupport.isCreditAccount(accountId)) {
            return resolveCreditLimitAdjust(instruction, accountId);
        }
        if (routeSubjectSupport.isBudgetGroup(accountId)) {
            return resolveBudgetLimitAdjust(instruction, accountId);
        }
        throw new IllegalArgumentException(UNSUPPORTED_ADJUST_SUBJECT_TYPE_MESSAGE + accountId.type());
    }

    private ResolvedRouteSpec resolveFundingBalanceAdjust(FundsInstructionSpec instruction, FundsAccountId accountId) {
        boolean increase = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.INCREASE);
        FundsAccountId adjustmentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.ADJUSTMENT);
        RouteLegSpec leg = increase
                ? RouteSpecSupport.routeLeg(LEG_FUNDING_BALANCE_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(platformAccountRouteSupport.createSubjectRef(adjustmentAccount),
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.ADJUSTMENT)))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(Map.of())
                .build()
                : RouteSpecSupport.routeLeg(LEG_FUNDING_BALANCE_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(RouteSpecSupport.targetNode(platformAccountRouteSupport.createSubjectRef(adjustmentAccount),
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.ADJUSTMENT)))
                .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(adjustAvailableBalanceConstraint(instruction, accountId))
                .build();
        List<RouteParticipantSpec> participants = routeParticipantFactory.distinct(List.of(
                subjectParticipant(accountId, instruction),
                routeParticipantFactory.createParticipant(
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                        platformAccountRouteSupport.createSubjectRef(adjustmentAccount),
                        platformAccountRouteSupport.resolveLedgerProfileCode(PlatformFundingAccountRole.ADJUSTMENT).name(),
                        instruction.getAmount(), instruction.getDescription(), Map.of())
        ));
        PlatformAccountsSnapshotSpec snapshot = platformAccountRouteSupport.createAdjustmentSnapshot(adjustmentAccount);
        return route(instruction, FundsRouteCodes.FUNDING_BALANCE_ADJUST_STANDARD, participants, List.of(leg),
                snapshot);
    }

    private ResolvedRouteSpec resolveCreditLimitAdjust(FundsInstructionSpec instruction, FundsAccountId accountId) {
        return resolveInternalLimitAdjust(instruction, accountId, FundsRouteCodes.CREDIT_LIMIT_ADJUST_STANDARD);
    }

    private ResolvedRouteSpec resolveBudgetLimitAdjust(FundsInstructionSpec instruction, FundsAccountId accountId) {
        return resolveInternalLimitAdjust(instruction, accountId, FundsRouteCodes.BUDGET_LIMIT_ADJUST_STANDARD);
    }

    private ResolvedRouteSpec resolveInternalLimitAdjust(FundsInstructionSpec instruction,
                                                         FundsAccountId accountId,
                                                         String routeCode) {
        boolean increase = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.INCREASE);
        RouteLegSpec leg = increase
                ? RouteSpecSupport.routeLeg(LEG_LIMIT_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.LIMIT))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(Map.of())
                .build()
                : RouteSpecSupport.routeLeg(LEG_LIMIT_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.LIMIT))
                .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(adjustAvailableBalanceConstraint(instruction, accountId))
                .build();
        List<RouteParticipantSpec> participants = List.of(subjectParticipant(accountId, instruction));
        return route(instruction, routeCode, participants, List.of(leg));
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
        AssertUtils.isTrue(!participants.isEmpty(), PARTICIPANTS_REQUIRED_MESSAGE);
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

    private RouteParticipantSpec subjectParticipant(FundsAccountId accountId, FundsInstructionSpec instruction) {
        return routeParticipantFactory.createParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true),
                routeSubjectSupport.createSubjectRef(accountId),
                routeSubjectSupport.resolveLedgerProfileCode(accountId).name(), instruction.getAmount(),
                instruction.getDescription(), Map.of());
    }

    private Map<String, LedgerBalanceConstraintType> adjustAvailableBalanceConstraint(FundsInstructionSpec instruction,
                                                                                      FundsAccountId accountId) {
        if (!Boolean.TRUE.equals(FundsInstructionContextReader.getValue(
                instruction,
                FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE,
                Boolean.class))) {
            return RouteSpecSupport.balanceConstraint(
                    accountId,
                    LedgerSubjectCode.AVAILABLE,
                    LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
        }
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE,
                        String.class),
                ALLOW_NEGATIVE_BALANCE_POLICY_REQUIRED_MESSAGE);
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.APPROVAL_REF,
                        String.class),
                ALLOW_NEGATIVE_BALANCE_APPROVAL_REQUIRED_MESSAGE);
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.ADJUST_REASON,
                        String.class),
                ALLOW_NEGATIVE_BALANCE_REASON_REQUIRED_MESSAGE);
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
                        String.class),
                ALLOW_NEGATIVE_BALANCE_RISK_STATUS_REQUIRED_MESSAGE);
        requireLimitEvidence(instruction, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT);
        requireLimitEvidence(instruction, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT);
        AssertUtils.notNull(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT,
                        LocalDateTime.class),
                ALLOW_NEGATIVE_BALANCE_AGING_REQUIRED_MESSAGE);
        requireBudgetGovernance(instruction, accountId);
        return RouteSpecSupport.balanceConstraint(
                accountId, LedgerSubjectCode.AVAILABLE, LedgerBalanceConstraintType.ALLOW_NEGATIVE);
    }

    private void requireBudgetGovernance(FundsInstructionSpec instruction, FundsAccountId accountId) {
        if (!routeSubjectSupport.isBudgetGroup(accountId)) {
            return;
        }
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.BUDGET_PERIOD_ID,
                        String.class),
                BUDGET_PERIOD_REQUIRED_MESSAGE);
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                        String.class),
                BUDGET_GOVERNANCE_POLICY_REQUIRED_MESSAGE);
        AssertUtils.hasText(FundsInstructionContextReader.getValue(
                        instruction,
                        FundsInstructionContextKeys.BUDGET_REPORT_MARKER,
                        String.class),
                BUDGET_REPORT_MARKER_REQUIRED_MESSAGE);
    }

    private void requireLimitEvidence(FundsInstructionSpec instruction, String key) {
        Money limit = FundsInstructionContextReader.getValue(instruction, key, Money.class);
        AssertUtils.notNull(limit, limitRequiredMessage(key));
        AssertUtils.isTrue(limit.getAmount() > 0,
                ALLOW_NEGATIVE_BALANCE_LIMIT_AMOUNT_MESSAGE + "，key = {}", key);
        AssertUtils.isTrue(limit.getCurrency() == instruction.getAmount().getCurrency(),
                ALLOW_NEGATIVE_BALANCE_LIMIT_CURRENCY_MESSAGE + "，key = {}", key);
    }

    private String limitRequiredMessage(String key) {
        if (FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT.equals(key)) {
            return ALLOW_NEGATIVE_BALANCE_SINGLE_LIMIT_REQUIRED_MESSAGE;
        }
        return ALLOW_NEGATIVE_BALANCE_CUMULATIVE_LIMIT_REQUIRED_MESSAGE;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
