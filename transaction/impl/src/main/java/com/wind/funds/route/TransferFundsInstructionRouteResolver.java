package com.wind.funds.route;

import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.spec.transaction.FundsInstructionFieldKeys;
import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.wind.funds.route.support.RouteSpecSupport.mustNotBeNegative;
import static com.wind.funds.route.support.RouteSpecSupport.routeLeg;
import static com.wind.funds.route.support.RouteSpecSupport.sourceNode;
import static com.wind.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 直接交易 RouteResolver。
 */
@Component
@AllArgsConstructor
public class TransferFundsInstructionRouteResolver implements RouteResolver, Ordered {

    private static final String SAME_ACCOUNT_MESSAGE = "付款账户和收款账户不能一致";

    private static final String SAME_PAY_ACCOUNT_MESSAGE = "付款账户和收款主体不能一致";

    private static final String SAME_REFUND_ACCOUNT_MESSAGE = "退款到账账户和退款出资主体不能一致";

    private static final String UNSUPPORTED_ADJUSTMENT_MESSAGE = "adjustment must handled by balance-control resolver";

    private final RouteParticipantFactory routeParticipantFactory;

    private final RouteSubjectSupport routeSubjectSupport;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getInstructionType() == FundsInstructionType.DIRECT_TRANSACTION
                && !RouteReplaySupport.isReplayInstruction(instruction);
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
                FundsInstructionFieldKeys.ACCOUNT_ID);
        FundsAccountId cashMappingAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.CASH_MAPPING);
        FundsAccountId prepaymentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.PREPAYMENT);
        SubjectRef cashMappingSubject = platformAccountRouteSupport.createSubjectRef(cashMappingAccount);
        SubjectRef prepaymentSubject = platformAccountRouteSupport.createSubjectRef(prepaymentAccount);
        SubjectRef accountSubject = routeSubjectSupport.createSubjectRef(accountId);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(FundsRouteLegIds.FUND_IN, 1, RouteLegType.EXTERNAL_IN, instruction)
                .sourceNode(sourceNode(cashMappingSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.CASH_MAPPING)))
                .targetNode(targetNode(prepaymentSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.PREPAYMENT)))
                .balanceEffectType(LedgerBalanceEffectType.INCREASE)
                .phaseCode(LedgerPhaseCode.FUND_IN)
                .build());
        legs.add(routeLeg(FundsRouteLegIds.TOPUP_SETTLEMENT, 2, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(prepaymentSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.PREPAYMENT)))
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
        return route(instruction, FundsRouteCodes.TOPUP_STANDARD, participants, legs,
                instruction.getExternalAccountRef(),
                platformAccountRouteSupport.createExternalFundMovementSnapshot(cashMappingAccount, prepaymentAccount,
                        null));
    }

    private ResolvedRouteSpec resolveTransfer(FundsInstructionSpec instruction) {
        FundsAccountId payerAccountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.PAYER_ACCOUNT_ID);
        FundsAccountId payeeAccountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.PAYEE_ACCOUNT_ID);
        AssertUtils.isFalse(payerAccountId.equals(payeeAccountId), SAME_ACCOUNT_MESSAGE);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(FundsRouteLegIds.TRANSFER, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
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
        return route(instruction, FundsRouteCodes.INTERNAL_TRANSFER_STANDARD, participants, legs, null);
    }

    private ResolvedRouteSpec resolvePay(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        FundsAccountId payeeId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.PAYEE_ID);
        LedgerSubjectCode payeeLedgerSubjectCode = FundsInstructionContextReader.requireLedgerSubjectCode(instruction,
                FundsInstructionFieldKeys.PAYEE_LEDGER_SUBJECT_CODE);
        AssertUtils.isFalse(accountId.equals(payeeId), SAME_PAY_ACCOUNT_MESSAGE);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(FundsRouteLegIds.PAY, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(routeSubjectSupport.createSubjectRef(payeeId), payeeLedgerSubjectCode))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.AVAILABLE))
                .build());
        List<RouteParticipantSpec> participants = new ArrayList<>();
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(accountId, true), accountId,
                instruction.getAmount(), instruction.getDescription()));
        participants.add(subjectParticipant(routeSubjectSupport.resolveParticipantRole(payeeId, false), payeeId,
                instruction.getAmount(), instruction.getDescription()));
        return route(instruction, FundsRouteCodes.DIRECT_PAY_STANDARD, participants, legs, null);
    }

    private ResolvedRouteSpec resolveRefund(FundsInstructionSpec instruction) {
        FundsAccountId payerId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.PAYER_ID);
        LedgerSubjectCode payerLedgerSubjectCode = FundsInstructionContextReader.requireLedgerSubjectCode(instruction,
                FundsInstructionFieldKeys.PAYER_LEDGER_SUBJECT_CODE);
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        AssertUtils.isFalse(accountId.equals(payerId), SAME_REFUND_ACCOUNT_MESSAGE);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(FundsRouteLegIds.REFUND, 1, RouteLegType.RESTORE, instruction)
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
        return route(instruction, FundsRouteCodes.DIRECT_REFUND_STANDARD, participants, legs, null);
    }

    private ResolvedRouteSpec resolveWithdraw(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        assertWithdrawReferenceMatchesAccount(instruction, accountId);
        FundsAccountId cashMappingAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.CASH_MAPPING);
        FundsAccountId prepaymentAccount = platformAccountRouteSupport.requireAccount(
                instruction.getAmount().getCurrency(), PlatformFundingAccountRole.PREPAYMENT);
        SubjectRef accountSubject = routeSubjectSupport.createSubjectRef(accountId);
        SubjectRef cashMappingSubject = platformAccountRouteSupport.createSubjectRef(cashMappingAccount);
        SubjectRef prepaymentSubject = platformAccountRouteSupport.createSubjectRef(prepaymentAccount);
        List<RouteLegSpec> legs = new ArrayList<>();
        legs.add(routeLeg(FundsRouteLegIds.WITHDRAW_SETTLEMENT, 1, RouteLegType.CONSUME, instruction)
                .sourceNode(sourceNode(accountSubject, LedgerSubjectCode.FROZEN))
                .targetNode(targetNode(prepaymentSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.PREPAYMENT)))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .constraintOverrides(mustNotBeNegative(accountId, LedgerSubjectCode.FROZEN))
                .build());
        legs.add(routeLeg(FundsRouteLegIds.FUND_OUT, 2, RouteLegType.EXTERNAL_OUT, instruction)
                .sourceNode(sourceNode(prepaymentSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.PREPAYMENT)))
                .targetNode(targetNode(cashMappingSubject,
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.CASH_MAPPING)))
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
        return route(instruction, FundsRouteCodes.WITHDRAW_STANDARD, participants, legs,
                instruction.getExternalAccountRef(),
                platformAccountRouteSupport.createExternalFundMovementSnapshot(cashMappingAccount, prepaymentAccount,
                        null));
    }

    private void assertWithdrawReferenceMatchesAccount(FundsInstructionSpec instruction, FundsAccountId accountId) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        AssertUtils.notNull(reference, "提现必须引用冻结单");
        AssertUtils.isTrue(reference.getReferenceType() == FundsInstructionReferenceType.FREEZE_ORDER,
                "提现必须引用冻结单");
        AssertUtils.hasText(reference.getReferenceSn(), "提现必须引用冻结单");
        Optional<RouteSnapshotSpec> routeSnapshot =
                fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(reference.getReferenceSn());
        AssertUtils.isTrue(routeSnapshot.isPresent(),
                "提现引用冻结单不存在或缺少原冻结路径，referenceSn = {}", reference.getReferenceSn());
        RouteSnapshotSpec snapshot = routeSnapshot.get();
        AssertUtils.isTrue(snapshotContainsSubject(snapshot, accountId),
                "提现引用冻结单主体与请求账户不一致，referenceSn = {}，accountId = {}，accountType = {}",
                reference.getReferenceSn(), accountId.id(), accountId.type());
        assertFreezeOrderWithdrawAmountAvailable(instruction, reference, snapshot);
    }

    private void assertFreezeOrderWithdrawAmountAvailable(FundsInstructionSpec instruction,
                                                          FundsInstructionReferenceSpec reference,
                                                          RouteSnapshotSpec routeSnapshot) {
        RouteLegSpec freezeLeg = routeSnapshot.getLegs().stream()
                .filter(leg -> FundsRouteLegIds.FREEZE.equals(leg.getLegId()))
                .findFirst()
                .orElse(null);
        AssertUtils.notNull(freezeLeg, "提现引用冻结单缺少原冻结路径，referenceSn = {}", reference.getReferenceSn());
        Money freezeAmount = freezeLeg.getAmount();
        AssertUtils.isTrue(instruction.getAmount().getCurrency() == freezeAmount.getCurrency(),
                "提现引用冻结单币种与请求金额不一致，referenceSn = {}", reference.getReferenceSn());
        long totalUsedAmount = sumFreezeOrderUsedAmount(reference.getReferenceSn(), freezeAmount);
        long usedAmount = sumFreezeOrderUsedAmount(instruction, reference.getReferenceSn(), freezeAmount);
        if (totalUsedAmount > usedAmount) {
            return;
        }
        long remainingAmount = freezeAmount.getAmount() - usedAmount;
        AssertUtils.isTrue(instruction.getAmount().getAmount() <= remainingAmount,
                "冻结单剩余可提现金额不足，referenceSn = {}，remainingAmount = {}，amount = {}",
                reference.getReferenceSn(), remainingAmount, instruction.getAmount().getAmount());
    }

    private long sumFreezeOrderUsedAmount(String freezeOrderSn, Money freezeAmount) {
        Money withdrawAmount = fundsTransactionQueryService.sumConsumedReplayLegAmount(freezeOrderSn,
                FundsTransactionEventType.WITHDRAW,
                FundsRouteLegIds.FREEZE,
                freezeAmount.getCurrency());
        Money unfreezeAmount = fundsTransactionQueryService.sumConsumedReplayLegAmount(freezeOrderSn,
                FundsTransactionEventType.UNFREEZE,
                FundsRouteLegIds.FREEZE,
                freezeAmount.getCurrency());
        return withdrawAmount.getAmount() + unfreezeAmount.getAmount();
    }

    private long sumFreezeOrderUsedAmount(FundsInstructionSpec instruction, String freezeOrderSn, Money freezeAmount) {
        Money withdrawAmount = fundsTransactionQueryService.sumConsumedReplayLegAmount(freezeOrderSn,
                FundsTransactionEventType.WITHDRAW,
                FundsRouteLegIds.FREEZE,
                freezeAmount.getCurrency(),
                instruction.getBusinessScene(),
                instruction.getBusinessSn());
        Money unfreezeAmount = fundsTransactionQueryService.sumConsumedReplayLegAmount(freezeOrderSn,
                FundsTransactionEventType.UNFREEZE,
                FundsRouteLegIds.FREEZE,
                freezeAmount.getCurrency(),
                instruction.getBusinessScene(),
                instruction.getBusinessSn());
        return withdrawAmount.getAmount() + unfreezeAmount.getAmount();
    }

    private boolean snapshotContainsSubject(RouteSnapshotSpec routeSnapshot, FundsAccountId accountId) {
        for (RouteParticipantSpec participant : routeSnapshot.getParticipants()) {
            if (sameSubject(participant.getSubjectRef(), accountId)) {
                return true;
            }
        }
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            if (sameSubject(leg.getSourceNode().getSubjectRef(), accountId)
                    || sameSubject(leg.getTargetNode().getSubjectRef(), accountId)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameSubject(SubjectRef subjectRef, FundsAccountId accountId) {
        return Objects.equals(subjectRef.getSubjectId(), accountId.id())
                && subjectRef.getSubjectType() == routeSubjectSupport.resolveSubjectType(accountId);
    }

    private ResolvedRouteSpec resolveFee(FundsInstructionSpec instruction) {
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        FundsAccountId feeAccount = platformAccountRouteSupport.requireAccount(instruction.getAmount().getCurrency(),
                PlatformFundingAccountRole.FEE);
        List<RouteLegSpec> legs = List.of(routeLeg(FundsRouteLegIds.FEE, 1, RouteLegType.INTERNAL_TRANSFER, instruction)
                .sourceNode(sourceNode(routeSubjectSupport.createSubjectRef(accountId), LedgerSubjectCode.AVAILABLE))
                .targetNode(targetNode(platformAccountRouteSupport.createSubjectRef(feeAccount),
                        platformAccountRouteSupport.resolveLedgerSubjectCode(PlatformFundingAccountRole.FEE)))
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
        RouteSpecSupport.requireParticipants(distinctParticipants);
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
                .contextVariables(Map.of())
                .build();
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
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

    @Override
    public int getOrder() {
        return 0;
    }

}
