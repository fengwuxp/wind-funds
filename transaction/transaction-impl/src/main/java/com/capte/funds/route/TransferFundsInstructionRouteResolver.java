package com.capte.funds.route;

import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.support.FundsInstructionContextReader;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.FundsAccountTransactionFeeProvider;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 直接交易 RouteResolver。
 */
@Component
@AllArgsConstructor
public class TransferFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String LEG_FUND_IN = "FUND_IN";

    private static final String LEG_TOPUP_SETTLEMENT = "TOPUP_SETTLEMENT";

    private static final String LEG_TRANSFER = "TRANSFER";

    private static final String LEG_PAY = "PAY";

    private static final String LEG_REFUND = "REFUND";

    private static final String LEG_WITHDRAW_SETTLEMENT = "WITHDRAW_SETTLEMENT";

    private static final String LEG_FUND_OUT = "FUND_OUT";

    private static final String LEG_FEE = "FEE";

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private static final String SAME_ACCOUNT_MESSAGE = "付款账号和收款账户不能一致";

    private static final String UNSUPPORTED_ADJUSTMENT_MESSAGE = "adjustment must handled by balance-control resolver";

    private static final String PARTICIPANTS_REQUIRED_MESSAGE = "ResolvedRoute participants 不能为空";

    private static final String ROUTE_CODE_REQUIRED_MESSAGE = "ResolvedRoute routeCode 不能为空";

    private static final String ROUTE_VERSION_REQUIRED_MESSAGE = "ResolvedRoute routeVersion 不能为空";

    private static final String BUSINESS_SCENE_REQUIRED_MESSAGE = "ResolvedRoute businessScene 不能为空";

    private static final String BUSINESS_SN_REQUIRED_MESSAGE = "ResolvedRoute businessSn 不能为空";

    private static final String INSTRUCTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute instructionType 不能为空";

    private static final String EVENT_TYPE_REQUIRED_MESSAGE = "ResolvedRoute eventType 不能为空";

    private static final String TRANSACTION_TYPE_REQUIRED_MESSAGE = "ResolvedRoute transactionType 不能为空";

    private static final String RESOLVED_AT_REQUIRED_MESSAGE = "ResolvedRoute resolvedAt 不能为空";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    private final FundsAccountTransactionFeeProvider fundsAccountTransactionFeeProvider;

    @Override
    public boolean support(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION;
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        return switch (instruction.getTransactionType()) {
            case TOPUP -> resolveTopup(instruction);
            case TRANSFER -> resolveTransfer(instruction);
            case PAY -> resolvePay(instruction);
            case REFUND -> resolveRefund(instruction);
            case WITHDRAW -> resolveWithdraw(instruction);
            case FEE -> resolveFee(instruction);
            case ADJUSTMENT -> throw new IllegalArgumentException(UNSUPPORTED_ADJUSTMENT_MESSAGE);
        };
    }

    private ResolvedRouteSpec resolveTopup(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId cashMappingAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.CASH_MAPPING);
        FundsAccountId prepaymentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.PREPAYMENT);
        SubjectRef cashMappingSubject = platformAccountRouteSupport.createSubjectRef(cashMappingAccount);
        SubjectRef prepaymentSubject = platformAccountRouteSupport.createSubjectRef(prepaymentAccount);
        SubjectRef accountSubject = routeSubjectSupport.createSubjectRef(accountId);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(LEG_FUND_IN, 1, RouteLegType.EXTERNAL_IN, instruction)
                .sourceNode(sourceNode(cashMappingSubject, LedgerSubjectCode.CASH))
                .targetNode(targetNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.FUND_IN)
                .build());
        legs.add(routeLeg(LEG_TOPUP_SETTLEMENT, 2, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                .targetNode(targetNode(accountSubject, LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(platformParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, cashMappingAccount,
                PlatformFundingAccountRole.CASH_MAPPING, instruction.getAmount(), instruction.getDescription()));
        participants.add(platformParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, prepaymentAccount,
                PlatformFundingAccountRole.PREPAYMENT, instruction.getAmount(), instruction.getDescription()));
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, false), accountId,
                instruction.getAmount(), instruction.getDescription()));
        FundsAccountId feeAccount = appendFeeLeg(participants, legs, accountId, instruction, legs.size());
        return route(instruction, FundsRouteCodes.TOPUP_STANDARD, participants, legs,
                instruction.getExternalAccountRef(),
                platformAccountRouteSupport.createExternalFundMovementSnapshot(cashMappingAccount, prepaymentAccount,
                        feeAccount));
    }

    private ResolvedRouteSpec resolveTransfer(FundsInstructionSpec instruction) {
        FundsAccountId payerAccountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.PAYER_ACCOUNT_ID);
        FundsAccountId payeeAccountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.PAYEE_ACCOUNT_ID);
        AssertUtils.isFalse(payerAccountId.equals(payeeAccountId), SAME_ACCOUNT_MESSAGE);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(LEG_TRANSFER, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(payerAccountId),
                        LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(payeeAccountId),
                        LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.TRANSFER)
                .constraintOverrides(mustNotBeNegative(payerAccountId, LedgerSubjectCode.AVAILABLE))
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(payerAccountId, true),
                payerAccountId, instruction.getAmount(), instruction.getDescription()));
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(payeeAccountId, false),
                payeeAccountId, instruction.getAmount(), instruction.getDescription()));
        FundsAccountId feeAccount = appendFeeLeg(participants, legs, payerAccountId, instruction, legs.size());
        return route(instruction, FundsRouteCodes.INTERNAL_TRANSFER_STANDARD, participants, legs,
                platformAccountRouteSupport.createFeeSnapshot(feeAccount));
    }

    private ResolvedRouteSpec resolvePay(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId payeeId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.PAYEE_ID);
        LedgerSubjectCode payeeLedgerSubjectCode = FundsInstructionContextReader.requireLedgerSubjectCode(instruction,
                FundsInstructionContextKeys.PAYEE_LEDGER_SUBJECT_CODE);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(LEG_PAY, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(payeeId), payeeLedgerSubjectCode))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.AVAILABLE))
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                instruction.getAmount(), instruction.getDescription()));
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(payeeId, false), payeeId,
                instruction.getAmount(), instruction.getDescription()));
        FundsAccountId feeAccount = appendFeeLeg(participants, legs, accountId, instruction, legs.size());
        return route(instruction, FundsRouteCodes.DIRECT_PAY_STANDARD, participants, legs,
                platformAccountRouteSupport.createFeeSnapshot(feeAccount));
    }

    private ResolvedRouteSpec resolveRefund(FundsInstructionSpec instruction) {
        FundsAccountId payerId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.PAYER_ID);
        LedgerSubjectCode payerLedgerSubjectCode = FundsInstructionContextReader.requireLedgerSubjectCode(instruction,
                FundsInstructionContextKeys.PAYER_LEDGER_SUBJECT_CODE);
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(LEG_REFUND, 1, RouteLegType.RESTORE, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(payerId), payerLedgerSubjectCode))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .balanceEffectType(LedgerBalanceEffectType.RESTORE)
                .phaseCode(LedgerPhaseCode.REFUND)
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(payerId, true), payerId,
                instruction.getAmount(), instruction.getDescription()));
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, false), accountId,
                instruction.getAmount(), instruction.getDescription()));
        FundsAccountId feeAccount = appendFeeLeg(participants, legs, accountId, instruction, legs.size());
        return route(instruction, FundsRouteCodes.DIRECT_REFUND_STANDARD, participants, legs,
                platformAccountRouteSupport.createFeeSnapshot(feeAccount));
    }

    private ResolvedRouteSpec resolveWithdraw(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId cashMappingAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.CASH_MAPPING);
        FundsAccountId prepaymentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.PREPAYMENT);
        SubjectRef accountSubject = routeSubjectSupport.createSubjectRef(accountId);
        SubjectRef cashMappingSubject = platformAccountRouteSupport.createSubjectRef(cashMappingAccount);
        SubjectRef prepaymentSubject = platformAccountRouteSupport.createSubjectRef(prepaymentAccount);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(LEG_WITHDRAW_SETTLEMENT, 1, RouteLegType.CONSUME, instruction)
                .sourceNode(sourceNode(accountSubject, LedgerSubjectCode.FROZEN))
                .targetNode(targetNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.FROZEN))
                .build());
        legs.add(routeLeg(LEG_FUND_OUT, 2, RouteLegType.EXTERNAL_OUT, instruction)
                .sourceNode(sourceNode(prepaymentSubject, LedgerSubjectCode.PREPAYMENT))
                .targetNode(targetNode(cashMappingSubject, LedgerSubjectCode.CASH))
                .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                .phaseCode(LedgerPhaseCode.FUND_OUT)
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                instruction.getAmount(), instruction.getDescription()));
        participants.add(platformParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, prepaymentAccount,
                PlatformFundingAccountRole.PREPAYMENT, instruction.getAmount(), instruction.getDescription()));
        participants.add(platformParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT, cashMappingAccount,
                PlatformFundingAccountRole.CASH_MAPPING, instruction.getAmount(), instruction.getDescription()));
        FundsAccountId feeAccount = appendFeeLeg(participants, legs, accountId, instruction, legs.size());
        return route(instruction, FundsRouteCodes.WITHDRAW_STANDARD, participants, legs,
                instruction.getExternalAccountRef(),
                platformAccountRouteSupport.createExternalFundMovementSnapshot(cashMappingAccount, prepaymentAccount,
                        feeAccount));
    }

    private ResolvedRouteSpec resolveFee(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionContextKeys.ACCOUNT_ID);
        FundsAccountId feeAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.FEE);
        List<RouteLegSpec> legs = List.of(routeLeg(LEG_FEE, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(platformAccountRouteSupport.createSubjectRef(feeAccount),
                        LedgerSubjectCode.FEE))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.FEE)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.AVAILABLE))
                .build());
        List<RouteParticipantSpec> participants = routeParticipantFactory.distinct(List.of(
                subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                        instruction.getAmount(), instruction.getDescription()),
                platformParticipant(RouteParticipantRole.FEE_RECEIVER, feeAccount,
                        PlatformFundingAccountRole.FEE, instruction.getAmount(), instruction.getDescription())
        ));
        return route(instruction, FundsRouteCodes.FEE_STANDARD, participants, legs,
                platformAccountRouteSupport.createFeeSnapshot(feeAccount));
    }

    private FundsAccountId appendFeeLeg(List<RouteParticipantSpec> participants,
                                        List<RouteLegSpec> legs,
                                        FundsAccountId payerAccountId,
                                        FundsInstructionSpec instruction,
                                        int currentSize) {
        Money feeAmount = calculateFee(payerAccountId, instruction.getBusinessScene(), instruction.getAmount());
        if (feeAmount.getAmount() <= 0L) {
            return null;
        }
        FundsAccountId feeAccount = platformAccountRouteSupport.requireAccount(feeAmount.getCurrency(),
                PlatformFundingAccountRole.FEE);
        legs.add(routeLeg(LEG_FEE, currentSize + 1, RouteLegType.INTERNAL_TRANSFER,
                feeAmount, instruction.getDescription())
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(payerAccountId),
                        LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(platformAccountRouteSupport.createSubjectRef(feeAccount),
                        LedgerSubjectCode.FEE))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.FEE)
                .constraintOverrides(mustNotBeNegative(payerAccountId, LedgerSubjectCode.AVAILABLE))
                .build());
        participants.add(platformParticipant(RouteParticipantRole.FEE_RECEIVER, feeAccount,
                PlatformFundingAccountRole.FEE, feeAmount, instruction.getDescription()));
        return feeAccount;
    }

    private ResolvedRouteSpec route(FundsInstructionSpec instruction,
                                    String routeCode,
                                    List<RouteParticipantSpec> participants,
                                    List<RouteLegSpec> legs,
                                    @Nullable PlatformAccountsSnapshotSpec platformAccounts) {
        return route(instruction, routeCode, participants, legs, null, platformAccounts);
    }

    private ResolvedRouteSpec route(FundsInstructionSpec instruction,
                                    String routeCode,
                                    List<RouteParticipantSpec> participants,
                                    List<RouteLegSpec> legs,
                                    @Nullable ExternalAccountRefSpec externalAccountRef,
                                    @Nullable PlatformAccountsSnapshotSpec platformAccounts) {
        List<RouteParticipantSpec> distinctParticipants = routeParticipantFactory.distinct(participants);
        AssertUtils.isTrue(!distinctParticipants.isEmpty(), PARTICIPANTS_REQUIRED_MESSAGE);
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(instruction.getTenantId())
                .routeCode(routeCode)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(distinctParticipants)
                .legs(legs)
                .paymentInstrumentRef(instruction.getInstrumentRef())
                .externalAccountRef(externalAccountRef)
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

    private ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(String legId,
                                                                        int sequence,
                                                                        RouteLegType legType,
                                                                        FundsInstructionSpec instruction) {
        return routeLeg(legId, sequence, legType, instruction.getAmount(), instruction.getOriginalAmount(),
                instruction.getExchangeRate(), instruction.getDescription());
    }

    private ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(String legId,
                                                                        int sequence,
                                                                        RouteLegType legType,
                                                                        Money amount,
                                                                        String description) {
        return routeLeg(legId, sequence, legType, amount, amount, BigDecimal.ONE, description);
    }

    private ImmutableRouteLegSpec.ImmutableRouteLegSpecBuilder routeLeg(String legId,
                                                                        int sequence,
                                                                        RouteLegType legType,
                                                                        Money amount,
                                                                        Money originalAmount,
                                                                        BigDecimal exchangeRate,
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

    private RouteNodeSpec sourceNode(SubjectRef subjectRef, LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.SOURCE);
    }

    private RouteNodeSpec targetNode(SubjectRef subjectRef, LedgerSubjectCode ledgerSubjectCode) {
        return routeNode(subjectRef, ledgerSubjectCode, RouteNodeRole.TARGET);
    }

    private RouteNodeSpec routeNode(SubjectRef subjectRef,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
                .nodeRole(nodeRole)
                .build();
    }

    private Money calculateFee(FundsAccountId accountId, String businessScene, Money transactionAmount) {
        if (!fundsAccountTransactionFeeProvider.support(accountId)) {
            return Money.immutable(0L, transactionAmount.getCurrency());
        }
        FeeSpec fee = fundsAccountTransactionFeeProvider.apply(accountId, businessScene);
        return fee == null ? Money.immutable(0L, transactionAmount.getCurrency()) : fee.calculateFee(transactionAmount);
    }

    private RouteParticipantSpec subjectParticipant(RouteParticipantRole role,
                                                    FundsAccountId accountId,
                                                    Money amount,
                                                    String description) {
        return routeParticipantFactory.createParticipant(role,
                routeSubjectSupport.createSubjectRef(accountId),
                routeSubjectSupport.resolveLedgerProfileCode(accountId).name(), amount, description, Map.of());
    }

    private RouteParticipantSpec platformParticipant(RouteParticipantRole role,
                                                     FundsAccountId accountId,
                                                     PlatformFundingAccountRole accountRole,
                                                     Money amount,
                                                     String description) {
        return routeParticipantFactory.createParticipant(role,
                platformAccountRouteSupport.createSubjectRef(accountId),
                platformAccountRouteSupport.resolveLedgerProfileCode(accountRole).name(), amount, description,
                Map.of());
    }

    private Map<String, LedgerBalanceConstraintType> mustNotBeNegative(FundsAccountId accountId,
                                                                       LedgerSubjectCode subjectCode) {
        return Map.of(accountId.type() + CONSTRAINT_KEY_SEPARATOR
                        + accountId.id() + CONSTRAINT_KEY_SEPARATOR
                        + subjectCode.name(),
                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
