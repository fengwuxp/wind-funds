package com.wind.funds.route;

import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 余额控制 RouteResolver。
 */
@Component
@AllArgsConstructor
public class BalanceControlFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String UNSUPPORTED_EVENT_TYPE_MESSAGE = "unsupported balance-control eventType: ";

    private static final String UNSUPPORTED_ADJUST_SUBJECT_TYPE_MESSAGE = "unsupported adjust subject type: ";

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

    private static final String BUDGET_GROUP_ADJUST_FORBIDDEN_MESSAGE =
            "预算组额度调整已迁移到预算控制活动，不允许通过余额控制路由入账";

    private static final String ALLOW_NEGATIVE_BALANCE_LIMIT_CURRENCY_MESSAGE =
            "受控负余额调账上限币种必须与本次金额币种一致";

    private static final String ALLOW_NEGATIVE_BALANCE_LIMIT_AMOUNT_MESSAGE =
            "受控负余额调账上限必须大于 0";

    private static final String FREEZE_AVAILABLE_BALANCE_NOT_ENOUGH_MESSAGE =
            "账本余额不足，subjectId = {}, subjectType = {}, ledgerSubjectCode = {}, beforeBalance = {}, balanceDelta = {}, afterBalance = {}";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    private final FundsAccountQueryService fundsAccountQueryService;

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
        assertEnoughAvailableBalance(accountId, instruction.getAmount());
        List<RouteLegSpec> legs = List.of(RouteSpecSupport.routeLeg(
                FundsRouteLegIds.FREEZE, 1, RouteLegType.HOLD, instruction)
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

    private void assertEnoughAvailableBalance(FundsAccountId accountId, Money amount) {
        Money beforeBalance = fundsAccountQueryService.getBalance(accountId).getAvailableBalance();
        long beforeAmount = beforeBalance.getAmount();
        long balanceDelta = -amount.getAmount();
        long afterAmount = beforeAmount + balanceDelta;
        AssertUtils.isTrue(afterAmount >= 0,
                FREEZE_AVAILABLE_BALANCE_NOT_ENOUGH_MESSAGE,
                accountId.id(),
                accountId.type(),
                LedgerSubjectCode.AVAILABLE,
                beforeAmount,
                balanceDelta,
                afterAmount);
    }

    private ResolvedRouteSpec resolveUnfreeze(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<RouteLegSpec> legs = List.of(RouteSpecSupport.routeLeg(
                FundsRouteLegIds.UNFREEZE, 1, RouteLegType.RELEASE, instruction)
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
            throw new IllegalArgumentException(BUDGET_GROUP_ADJUST_FORBIDDEN_MESSAGE);
        }
        throw new IllegalArgumentException(UNSUPPORTED_ADJUST_SUBJECT_TYPE_MESSAGE + accountId.type());
    }

    private ResolvedRouteSpec resolveFundingBalanceAdjust(FundsInstructionSpec instruction, FundsAccountId accountId) {
        boolean increase = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.INCREASE);
        FundsAccountId adjustmentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.ADJUSTMENT);
        RouteLegSpec leg = increase
                ? RouteSpecSupport.routeLeg(
                        FundsRouteLegIds.FUNDING_BALANCE_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(platformAccountRouteSupport.createSubjectRef(adjustmentAccount),
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.ADJUSTMENT)))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(Map.of())
                .build()
                : RouteSpecSupport.routeLeg(
                        FundsRouteLegIds.FUNDING_BALANCE_ADJUST, 1, RouteLegType.ADJUST, instruction)
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

    private ResolvedRouteSpec resolveInternalLimitAdjust(FundsInstructionSpec instruction,
                                                         FundsAccountId accountId,
                                                         String routeCode) {
        boolean increase = FundsInstructionContextReader.requireBoolean(instruction, FundsInstructionContextKeys.INCREASE);
        RouteLegSpec leg = increase
                ? RouteSpecSupport.routeLeg(FundsRouteLegIds.LIMIT_ADJUST, 1, RouteLegType.ADJUST, instruction)
                .sourceNode(RouteSpecSupport.sourceNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.LIMIT))
                .targetNode(RouteSpecSupport.targetNode(
                        routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.ADJUSTMENT)
                .constraintOverrides(Map.of())
                .build()
                : RouteSpecSupport.routeLeg(FundsRouteLegIds.LIMIT_ADJUST, 1, RouteLegType.ADJUST, instruction)
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
        RouteSpecSupport.requireParticipants(participants);
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
                .contextVariables(routeContextVariables(instruction))
                .build();
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
    }

    private Map<String, Object> routeContextVariables(FundsInstructionSpec instruction) {
        return switch (instruction.getEventType()) {
            case BALANCE_ADJUST, LIMIT_ADJUST -> adjustmentRouteContextVariables(instruction);
            default -> Map.of();
        };
    }

    private Map<String, Object> adjustmentRouteContextVariables(FundsInstructionSpec instruction) {
        Map<String, Object> result = new LinkedHashMap<>();
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.SOURCE_TYPE, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.SOURCE_SN, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.REASON_CODE, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.ADJUST_REASON, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.APPROVAL_REF, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.EXTERNAL_FINAL_EVENT_REF, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.EXTERNAL_BALANCE_SNAPSHOT_REF,
                String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF,
                String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.RECONCILIATION_RERUN_REF,
                String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.RESPONSIBILITY_REF, String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE, Boolean.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE,
                String.class);
        putContextIfPresent(result, instruction, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
                String.class);
        return Collections.unmodifiableMap(result);
    }

    private <T> void putContextIfPresent(Map<String, Object> result,
                                         FundsInstructionSpec instruction,
                                         String key,
                                         Class<T> valueType) {
        T value = FundsInstructionContextReader.getValue(instruction, key, valueType);
        if (value != null) {
            result.put(key, value);
        }
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
        return RouteSpecSupport.balanceConstraint(
                accountId, LedgerSubjectCode.AVAILABLE, LedgerBalanceConstraintType.ALLOW_NEGATIVE);
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
