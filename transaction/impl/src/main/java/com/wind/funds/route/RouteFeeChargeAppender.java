package com.wind.funds.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.model.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.transaction.spec.FeeSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.wind.funds.route.support.RouteSpecSupport.routeLeg;
import static com.wind.funds.route.support.RouteSpecSupport.sourceNode;
import static com.wind.funds.route.support.RouteSpecSupport.targetNode;

/**
 * 为已解析的直接资金路径追加本次交易新增手续费腿。
 *
 * <p>调用方通过 {@link FeeSpec} 表达已确认的收费规则；本类只从主路径中选择唯一真实资金责任账户，
 * 不从 CreditAccount 额度扣费，也不重新解释本金路径。</p>
 */
@Component
@AllArgsConstructor
public class RouteFeeChargeAppender {

    private static final String FEE_PAYER_REQUIRED_MESSAGE =
            "随交易手续费扣款账户必须是唯一真实资金账户";

    private final RouteParticipantFactory routeParticipantFactory;

    private final PlatformAccountRouteSupport platformAccountRouteSupport;

    private final FundsAccountQueryService fundsAccountQueryService;

    @NonNull ResolvedRouteSpec append(@NonNull FundsInstructionSpec instruction,
                                      @NonNull ResolvedRouteSpec route) {
        FeeSpec feeChargeSpec = FundsInstructionContextReader.getValue(instruction,
                FundsInstructionContextKeys.FEE_CHARGE_SPEC,
                FeeSpec.class);
        if (feeChargeSpec == null) {
            return route;
        }
        Money feeAmount = feeChargeSpec.calculateFee(instruction.getAmount());
        AssertUtils.isTrue(feeAmount.getAmount() > 0L,
                "手续费计算结果必须大于 0，businessScene = {}，businessSn = {}",
                instruction.getBusinessScene(), instruction.getBusinessSn());
        FeePayer feePayer = resolveFeePayer(instruction, route);
        FundsAccountId feePayerAccountId = FundsAccountId.immutable(
                feePayer.subjectRef().getSubjectId(),
                FundsSubjectType.FUNDING_ACCOUNT.name());
        AssertUtils.isTrue(fundsAccountQueryService.getAccount(feePayerAccountId).getStatus().canDebit(),
                "账户状态不允许扣取随交易手续费，accountId = {}",
                feePayerAccountId);
        FundsAccountId feeAccount = platformAccountRouteSupport.requireAccount(
                feeAmount.getCurrency(), PlatformFundingAccountRole.FEE);

        List<RouteLegSpec> legs = new ArrayList<>(route.getLegs());
        legs.add(routeLeg(FundsRouteLegIds.FEE, nextSequence(legs), RouteLegType.INTERNAL_TRANSFER,
                feeAmount, instruction.getDescription())
                .sourceNode(sourceNode(feePayer.subjectRef()))
                .targetNode(targetNode(platformAccountRouteSupport.createSubjectRef(feeAccount)))
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .build());

        List<RouteParticipantSpec> participants = new ArrayList<>(route.getParticipants());
        participants.add(routeParticipantFactory.createParticipant(
                RouteParticipantRole.FEE_RECEIVER,
                platformAccountRouteSupport.createSubjectRef(feeAccount),
                platformAccountRouteSupport.resolveLedgerProfileCode(PlatformFundingAccountRole.FEE).name(),
                feeAmount,
                instruction.getDescription(),
                Map.of()));
        ResolvedRouteSpec result = copyRoute(route,
                routeParticipantFactory.distinct(participants),
                List.copyOf(legs),
                appendFeeAccount(route.getPlatformAccounts(), feeAccount));
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
    }

    private FeePayer resolveFeePayer(FundsInstructionSpec instruction, ResolvedRouteSpec route) {
        List<FeePayer> candidates = switch (instruction.getEventType()) {
            case TOPUP, REFUND -> targetFundingAccounts(route);
            case TRANSFER, PAY -> availableSourceFundingAccounts(route);
            case WITHDRAW -> withdrawSourceFundingAccounts(route);
            default -> throw new IllegalArgumentException(
                    "当前资金动作不支持随交易手续费，eventType = " + instruction.getEventType());
        };
        Map<String, FeePayer> distinct = new LinkedHashMap<>();
        candidates.forEach(candidate -> distinct.putIfAbsent(candidate.subjectRef().getSubjectId(), candidate));
        AssertUtils.isTrue(distinct.size() == 1,
                FEE_PAYER_REQUIRED_MESSAGE + "，eventType = {}，count = {}",
                instruction.getEventType(), distinct.size());
        return distinct.values().iterator().next();
    }

    private List<FeePayer> targetFundingAccounts(ResolvedRouteSpec route) {
        return route.getLegs().stream()
                .map(RouteLegSpec::getTargetNode)
                .filter(node -> !isPlatformParticipant(route, node.getSubjectRef()))
                .map(this::feePayer)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<FeePayer> availableSourceFundingAccounts(ResolvedRouteSpec route) {
        return route.getLegs().stream()
                .map(RouteLegSpec::getSourceNode)
                .filter(node -> !isPlatformParticipant(route, node.getSubjectRef()))
                .map(this::feePayer)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<FeePayer> withdrawSourceFundingAccounts(ResolvedRouteSpec route) {
        return route.getLegs().stream()
                .map(RouteLegSpec::getSourceNode)
                .filter(node -> !isPlatformParticipant(route, node.getSubjectRef()))
                .map(this::feePayer)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private boolean isPlatformParticipant(ResolvedRouteSpec route, SubjectRef subjectRef) {
        return route.getParticipants().stream()
                .anyMatch(participant -> participant.getParticipantRole() == RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT
                        && participant.getSubjectRef().getSubjectId().equals(subjectRef.getSubjectId())
                        && participant.getSubjectRef().getSubjectType() == subjectRef.getSubjectType());
    }

    private FeePayer feePayer(RouteNodeSpec node) {
        if (node.getSubjectRef().getSubjectType() != FundsSubjectType.FUNDING_ACCOUNT) {
            return null;
        }
        return new FeePayer(node.getSubjectRef());
    }

    private int nextSequence(List<RouteLegSpec> legs) {
        return legs.stream().mapToInt(RouteLegSpec::getSequence).max().orElse(0) + 1;
    }

    private PlatformAccountsSnapshotSpec appendFeeAccount(PlatformAccountsSnapshotSpec snapshot,
                                                           FundsAccountId feeAccount) {
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .cashFundingAccount(snapshot == null ? null : snapshot.getCashFundingAccount())
                .prepaymentFundingAccount(snapshot == null ? null : snapshot.getPrepaymentFundingAccount())
                .clearingFundingAccount(snapshot == null ? null : snapshot.getClearingFundingAccount())
                .settlementFundingAccount(snapshot == null ? null : snapshot.getSettlementFundingAccount())
                .feeFundingAccount(platformAccountRouteSupport.createSubjectRef(feeAccount))
                .adjustmentFundingAccount(snapshot == null ? null : snapshot.getAdjustmentFundingAccount())
                .build();
    }

    private ResolvedRouteSpec copyRoute(ResolvedRouteSpec route,
                                        List<RouteParticipantSpec> participants,
                                        List<RouteLegSpec> legs,
                                        PlatformAccountsSnapshotSpec platformAccounts) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(route.getTenantId())
                .routeCode(route.getRouteCode())
                .routeVersion(route.getRouteVersion())
                .businessScene(route.getBusinessScene())
                .businessSn(route.getBusinessSn())
                .instructionType(route.getInstructionType())
                .eventType(route.getEventType())
                .transactionType(route.getTransactionType())
                .participants(participants)
                .legs(legs)
                .routingDecision(route.getRoutingDecision())
                .paymentInstrumentRef(route.getPaymentInstrumentRef())
                .externalAccountRef(route.getExternalAccountRef())
                .platformAccounts(platformAccounts)
                .resolvedAt(route.getResolvedAt())
                .expiresAt(route.getExpiresAt())
                .description(route.getDescription())
                .contextVariables(route.getContextVariables())
                .build();
    }

    private record FeePayer(SubjectRef subjectRef) {
    }
}
