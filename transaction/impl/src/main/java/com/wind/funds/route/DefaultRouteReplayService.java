package com.wind.funds.route;

import com.wind.funds.route.model.ImmutableReplayRequestSpec;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.support.RouteSpecSupport;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.transaction.support.FundsRouteLegIds;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsInstructionContextReader;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ReplayRequestSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteNodeSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionFieldKeys;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 默认 RouteSnapshot 回放解析器。
 *
 * <p>职责：基于已保存的 RouteSnapshot 派生撤销、完成、退款、拒付等后续路径。
 * 回放只复用原路径中的主体、平台账户和节点，不重新执行路由选择。</p>
 */
@Component
public class DefaultRouteReplayService implements RouteResolver, Ordered {

    private static final String CONSTRAINT_KEY_SEPARATOR = ":";

    private static final String REPLAY_LEG_ID_SEPARATOR = "_";

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private static final String REPLAY_REFERENCE_REQUIRED_MESSAGE = "RouteSnapshot 回放事件缺少原路径引用";

    private static final String REPLAY_LEG_REQUIRED_MESSAGE = "RouteSnapshot 没有可回放的 RouteLeg";

    private static final String FEE_REFUND_REPLAY_LEG_REQUIRED_MESSAGE = "手续费退回原交易没有可回放费用路径";

    private static final String FREEZE_ORDER_SUBJECT_MISMATCH_MESSAGE =
            "冻结单引用主体与请求账户不一致";

    private static final String AUTHORIZATION_SUBJECT_MISMATCH_MESSAGE =
            "授权引用主体与请求账户不一致";

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Autowired
    public DefaultRouteReplayService(@NonNull FundsTransactionQueryService fundsTransactionQueryService) {
        this.fundsTransactionQueryService = fundsTransactionQueryService;
    }

    public DefaultRouteReplayService() {
        this.fundsTransactionQueryService = null;
    }

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return RouteReplaySupport.isReplayInstruction(instruction);
    }

    @Override
    public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
        RouteSnapshotSpec snapshot = requireReplaySnapshot(instruction);
        RouteBenefitSnapshotContextSupport.assertOriginalBenefitSnapshotPresent(instruction, snapshot);
        Map<String, Object> replayContext = replayContext(instruction, snapshot);
        ReplayRequestSpec replayRequest = ImmutableReplayRequestSpec.builder()
                .replayType(resolveReplayType(instruction.getEventType()))
                .eventType(instruction.getEventType())
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .referenceBusinessSn(resolveReferenceBusinessSn(instruction.getReference()))
                .referenceSnapshotId(snapshot.getSnapshotId())
                .amount(instruction.getAmount())
                .originalAmount(instruction.getOriginalAmount())
                .exchangeRate(instruction.getExchangeRate())
                .eventTime(instruction.getEventTime())
                .description(instruction.getDescription())
                .operator(instruction.getOperator())
                .contextVariables(replayContext)
                .build();
        assertReplayLegAmountNotOverConsumed(instruction.getReference(), snapshot, replayRequest);
        return replay(snapshot, replayRequest);
    }

    private Map<String, Object> replayContext(@NonNull FundsInstructionSpec instruction,
                                              @NonNull RouteSnapshotSpec snapshot) {
        Map<String, Object> result = new LinkedHashMap<>(
                RouteBenefitSnapshotContextSupport.originalBenefitSnapshotSummary(snapshot));
        if (shouldPropagateDisputeRefundContext(instruction)) {
            result.putAll(disputeRefundContext(instruction));
        }
        return Map.copyOf(result);
    }

    private boolean shouldPropagateDisputeRefundContext(@NonNull FundsInstructionSpec instruction) {
        return instruction.getEventType() == FundsTransactionEventType.AUTH_REFUND
                && Objects.equals(instruction.getContextVariables().get(FundsInstructionContextKeys.REFUND_MODE),
                FundsInstructionContextKeys.REFUND_MODE_DISPUTE);
    }

    private Map<String, Object> disputeRefundContext(@NonNull FundsInstructionSpec instruction) {
        Map<String, Object> result = new LinkedHashMap<>(instruction.getContextVariables());
        result.remove(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN);
        return result;
    }

    public @NonNull ResolvedRouteSpec replay(@NonNull RouteSnapshotSpec snapshot,
                                             @NonNull ReplayRequestSpec replayRequest) {
        assertSupportedSnapshotSchemaVersion(snapshot);
        List<RouteLegSpec> sourceLegs = selectReplayLegs(snapshot, replayRequest);
        List<RouteLegSpec> replayLegs = new ArrayList<>(sourceLegs.size());
        int sequence = 1;
        for (RouteLegSpec sourceLeg : sourceLegs) {
            replayLegs.add(replayLeg(snapshot, sourceLeg, replayRequest, sequence++));
        }
        ResolvedRouteSpec result = ImmutableResolvedRouteSpec.builder()
                .tenantId(snapshot.getTenantId())
                .routeCode(resolveRouteCode(replayRequest))
                .routeVersion(snapshot.getRouteVersion())
                .businessScene(replayRequest.getBusinessScene())
                .businessSn(replayRequest.getBusinessSn())
                .instructionType(resolveInstructionType(snapshot, replayRequest))
                .eventType(resolveEventType(replayRequest))
                .transactionType(resolveTransactionType(snapshot, replayRequest))
                .participants(resolveParticipants(snapshot, replayLegs, replayRequest))
                .legs(replayLegs)
                .routingDecision(snapshot.getRoutingDecision())
                .paymentInstrumentRef(snapshot.getPaymentInstrumentRef())
                .externalAccountRef(snapshot.getExternalAccountRef())
                .platformAccounts(snapshot.getPlatformAccounts())
                .resolvedAt(replayRequest.getEventTime())
                .description(replayRequest.getDescription())
                .contextVariables(replayRequest.getContextVariables())
                .build();
        RouteSpecSupport.validateResolvedRoute(result);
        return result;
    }

    private RouteSnapshotSpec requireReplaySnapshot(FundsInstructionSpec instruction) {
        AssertUtils.notNull(fundsTransactionQueryService, "Route replay resolver requires FundsTransactionQueryService");
        FundsInstructionReferenceSpec reference = requireReplayReference(instruction);
        Optional<RouteSnapshotSpec> routeSnapshot = switch (reference.getReferenceType()) {
            case FREEZE_ORDER -> fundsTransactionQueryService.findRouteSnapshotByFreezeOrderSn(reference.getReferenceSn());
            default -> fundsTransactionQueryService.findRouteSnapshotByTransactionSn(reference.getReferenceSn());
        };
        AssertUtils.isTrue(routeSnapshot.isPresent(), "RouteSnapshot 回放事件未找到原路径快照，referenceSn = {}",
                reference.getReferenceSn());
        RouteSnapshotSpec result = routeSnapshot.get();
        assertFreezeOrderSubjectMatchesInstruction(instruction, reference, result);
        assertAuthorizationSubjectMatchesInstruction(instruction, reference, result);
        assertReplayOnceNotConsumed(instruction, reference, result);
        return result;
    }

    private FundsInstructionReferenceSpec requireReplayReference(FundsInstructionSpec instruction) {
        FundsInstructionReferenceSpec reference = instruction.getReference();
        AssertUtils.notNull(reference, REPLAY_REFERENCE_REQUIRED_MESSAGE);
        AssertUtils.notNull(reference.getReferenceType(), REPLAY_REFERENCE_REQUIRED_MESSAGE);
        AssertUtils.hasText(reference.getReferenceSn(), REPLAY_REFERENCE_REQUIRED_MESSAGE);
        return reference;
    }

    private void assertFreezeOrderSubjectMatchesInstruction(FundsInstructionSpec instruction,
                                                            FundsInstructionReferenceSpec reference,
                                                            RouteSnapshotSpec routeSnapshot) {
        if (reference.getReferenceType() != FundsInstructionReferenceType.FREEZE_ORDER) {
            return;
        }
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        AssertUtils.isTrue(snapshotContainsSubject(routeSnapshot, accountId),
                FREEZE_ORDER_SUBJECT_MISMATCH_MESSAGE + "，referenceSn = {}，accountId = {}，accountType = {}",
                reference.getReferenceSn(), accountId.id(), accountId.type());
    }

    private void assertAuthorizationSubjectMatchesInstruction(FundsInstructionSpec instruction,
                                                               FundsInstructionReferenceSpec reference,
                                                               RouteSnapshotSpec routeSnapshot) {
        if (reference.getReferenceType() != FundsInstructionReferenceType.AUTHORIZATION) {
            return;
        }
        FundsAccountId accountId = FundsInstructionContextReader.requireFundsAccountId(instruction,
                FundsInstructionFieldKeys.ACCOUNT_ID);
        Optional<SubjectRef> originalSubject = routeSnapshot.getLegs().stream()
                .filter(leg -> leg.getLegType() == RouteLegType.HOLD)
                .map(leg -> leg.getSourceNode().getSubjectRef())
                .findFirst();
        AssertUtils.isTrue(originalSubject.isPresent(), REPLAY_LEG_REQUIRED_MESSAGE);
        AssertUtils.isTrue(originalSubject.get().getSubjectId().equals(accountId.id())
                        && originalSubject.get().getSubjectType().name().equals(accountId.type()),
                AUTHORIZATION_SUBJECT_MISMATCH_MESSAGE
                        + "，referenceSn = {}，accountId = {}，accountType = {}",
                reference.getReferenceSn(), accountId.id(), accountId.type());
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
                && subjectRef.getSubjectType() == resolveSubjectType(accountId);
    }

    private FundsSubjectType resolveSubjectType(FundsAccountId accountId) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return FundsSubjectType.CREDIT_ACCOUNT;
        }
        if (Objects.equals(accountId.type(), SPEND_CONTROL_SCOPE_ACCOUNT_TYPE)) {
            throw new IllegalArgumentException("支出控制范围不是核心资金账务主体，不能用于 RouteSnapshot 回放主体匹配，accountId = "
                    + accountId);
        }
        return FundsSubjectType.FUNDING_ACCOUNT;
    }

    private void assertReplayOnceNotConsumed(FundsInstructionSpec instruction,
                                             FundsInstructionReferenceSpec reference,
                                             RouteSnapshotSpec routeSnapshot) {
        for (RouteLegSpec leg : routeSnapshot.getLegs()) {
            if (leg.getReplayPolicy() != RouteReplayPolicy.REPLAY_ONCE) {
                continue;
            }
            AssertUtils.isFalse(fundsTransactionQueryService.hasConsumedReplayLeg(
                            reference.getReferenceSn(), instruction.getEventType(), leg.getLegId()),
                    "RouteSnapshot leg 仅允许成功回放一次，referenceSn = {}，eventType = {}，legId = {}",
                    reference.getReferenceSn(), instruction.getEventType(), leg.getLegId());
        }
    }

    private void assertReplayLegAmountNotOverConsumed(@NonNull FundsInstructionReferenceSpec reference,
                                                      @NonNull RouteSnapshotSpec routeSnapshot,
                                                      @NonNull ReplayRequestSpec replayRequest) {
        FundsTransactionEventType eventType = resolveEventType(replayRequest);
        List<RouteLegSpec> sourceLegs = selectReplayLegs(routeSnapshot, replayRequest);
        for (RouteLegSpec sourceLeg : sourceLegs) {
            Money replayAmount = resolveReplayAmount(sourceLeg, replayRequest);
            Money consumedAmount = fundsTransactionQueryService.sumConsumedReplayLegAmount(
                    reference.getReferenceSn(), eventType, sourceLeg.getLegId(),
                    sourceLeg.getAmount().getCurrency(),
                    replayRequest.getBusinessScene(),
                    replayRequest.getBusinessSn());
            long withdrawConsumedAmount = freezeOrderWithdrawConsumedAmount(reference, eventType, sourceLeg,
                    replayRequest);
            long consumedTotal = consumedAmount.getAmount() + withdrawConsumedAmount + replayAmount.getAmount();
            assertReplayLegAmountWithinSource(reference, eventType, sourceLeg, replayAmount, consumedTotal,
                    withdrawConsumedAmount);
        }
    }

    private void assertReplayLegAmountWithinSource(@NonNull FundsInstructionReferenceSpec reference,
                                                   @NonNull FundsTransactionEventType eventType,
                                                   @NonNull RouteLegSpec sourceLeg,
                                                   @NonNull Money replayAmount,
                                                   long consumedTotal,
                                                   long withdrawConsumedAmount) {
        if (shouldReportFrozenOrderReleaseAmount(reference, eventType, withdrawConsumedAmount)) {
            long remainingAmount = Math.max(0L, sourceLeg.getAmount().getAmount()
                    - (consumedTotal - replayAmount.getAmount()));
            AssertUtils.isTrue(consumedTotal <= sourceLeg.getAmount().getAmount(),
                    "冻结单剩余可释放金额不足，referenceSn = {}，remainingAmount = {}，amount = {}",
                    reference.getReferenceSn(), remainingAmount, replayAmount.getAmount());
            return;
        }
        AssertUtils.isTrue(consumedTotal <= sourceLeg.getAmount().getAmount(),
                "RouteSnapshot leg 回放累计金额不能大于原 RouteLeg 金额，referenceSn = {}，eventType = {}，legId = {}",
                reference.getReferenceSn(), eventType, sourceLeg.getLegId());
    }

    private boolean shouldReportFrozenOrderReleaseAmount(@NonNull FundsInstructionReferenceSpec reference,
                                                         @NonNull FundsTransactionEventType eventType,
                                                         long withdrawConsumedAmount) {
        return reference.getReferenceType() == FundsInstructionReferenceType.FREEZE_ORDER
                && eventType == FundsTransactionEventType.UNFREEZE
                && withdrawConsumedAmount == 0L;
    }

    private long freezeOrderWithdrawConsumedAmount(@NonNull FundsInstructionReferenceSpec reference,
                                                   @NonNull FundsTransactionEventType eventType,
                                                   @NonNull RouteLegSpec sourceLeg,
                                                   @NonNull ReplayRequestSpec replayRequest) {
        if (reference.getReferenceType() != FundsInstructionReferenceType.FREEZE_ORDER
                || eventType != FundsTransactionEventType.UNFREEZE) {
            return 0L;
        }
        return fundsTransactionQueryService.sumConsumedReplayLegAmount(reference.getReferenceSn(),
                FundsTransactionEventType.WITHDRAW, sourceLeg.getLegId(),
                sourceLeg.getAmount().getCurrency(),
                replayRequest.getBusinessScene(),
                replayRequest.getBusinessSn()).getAmount();
    }

    private RouteReplayType resolveReplayType(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case REVERSAL -> RouteReplayType.RELEASE_HOLD;
            case COMPLETE -> RouteReplayType.AUTHORIZATION_COMPLETION;
            case AUTH_REFUND -> RouteReplayType.AUTHORIZATION_REFUND;
            case REFUND -> RouteReplayType.REFUND;
            case FEE_REFUND -> RouteReplayType.FEE_REFUND;
            case UNFREEZE -> RouteReplayType.UNFREEZE;
            default -> throw new IllegalArgumentException("unsupported replay eventType: " + eventType);
        };
    }

    private @Nullable String resolveReferenceBusinessSn(@Nullable FundsInstructionReferenceSpec reference) {
        return reference == null ? null : reference.getReferenceBusinessSn();
    }

    private List<RouteLegSpec> selectReplayLegs(RouteSnapshotSpec snapshot, ReplayRequestSpec replayRequest) {
        AssertUtils.notEmpty(snapshot.getLegs(), "RouteSnapshot legs 不能为空");
        Set<String> selectedLegIds = Set.copyOf(replayRequest.getReplayLegIds());
        List<RouteLegSpec> result = snapshot.getLegs().stream()
                .filter(leg -> selectedLegIds.isEmpty() || selectedLegIds.contains(leg.getLegId()))
                .filter(leg -> leg.getReplayPolicy() != RouteReplayPolicy.NON_REPLAYABLE)
                .filter(leg -> shouldReplayLeg(leg, replayRequest))
                .toList();
        AssertUtils.notEmpty(result, replayLegRequiredMessage(replayRequest));
        AssertUtils.isTrue(selectedLegIds.isEmpty() || result.size() == selectedLegIds.size(),
                "RouteSnapshot 回放 leg 不存在或不可回放，legIds = {}", selectedLegIds);
        assertReplayLegSubjectsPostable(result);
        return result;
    }

    private void assertReplayLegSubjectsPostable(List<RouteLegSpec> replayLegs) {
        for (RouteLegSpec replayLeg : replayLegs) {
            assertReplayNodeSubjectPostable(replayLeg.getSourceNode());
            assertReplayNodeSubjectPostable(replayLeg.getTargetNode());
        }
    }

    private void assertReplayNodeSubjectPostable(RouteNodeSpec routeNode) {
        FundsSubjectType subjectType = routeNode.getSubjectRef().getSubjectType();
        AssertUtils.isTrue(subjectType == FundsSubjectType.FUNDING_ACCOUNT
                        || subjectType == FundsSubjectType.CREDIT_ACCOUNT,
                "支出控制范围不是核心资金账务主体，不能作为 RouteSnapshot 回放参与方");
    }

    private String replayLegRequiredMessage(ReplayRequestSpec replayRequest) {
        if (replayRequest.getReplayType() == RouteReplayType.FEE_REFUND) {
            return FEE_REFUND_REPLAY_LEG_REQUIRED_MESSAGE;
        }
        return REPLAY_LEG_REQUIRED_MESSAGE;
    }

    private boolean shouldReplayLeg(RouteLegSpec leg,
                                    ReplayRequestSpec replayRequest) {
        boolean feeLeg = FundsRouteLegIds.FEE.equals(leg.getLegId());
        return switch (replayRequest.getReplayType()) {
            case AUTHORIZATION_REFUND, REFUND -> !feeLeg;
            case FEE_REFUND -> feeLeg;
            default -> true;
        };
    }

    private RouteLegSpec replayLeg(RouteSnapshotSpec snapshot,
                                   RouteLegSpec sourceLeg,
                                   ReplayRequestSpec replayRequest,
                                   int sequence) {
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD, UNFREEZE -> buildReleaseLeg(sourceLeg, replayRequest, sequence);
            case AUTHORIZATION_COMPLETION -> buildAuthorizationCompletionLeg(snapshot, sourceLeg, replayRequest, sequence);
            case AUTHORIZATION_REFUND, REFUND, FEE_REFUND -> buildRefundLeg(snapshot, sourceLeg, replayRequest,
                    sequence);
        };
    }

    private RouteLegSpec buildReleaseLeg(RouteLegSpec sourceLeg,
                                         ReplayRequestSpec replayRequest,
                                         int sequence) {
        RouteNodeSpec sourceNode = copyNode(sourceLeg.getTargetNode(), RouteNodeRole.SOURCE);
        RouteNodeSpec targetNode = copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET);
        return buildReplayLeg(sourceLeg, replayRequest, sequence, RouteLegType.RELEASE, sourceNode, targetNode,
                replayRequest.getDescription());
    }

    private RouteLegSpec buildAuthorizationCompletionLeg(RouteSnapshotSpec snapshot,
                                                         RouteLegSpec sourceLeg,
                                                         ReplayRequestSpec replayRequest,
                                                         int sequence) {
        RouteNodeSpec sourceNode = copyNode(sourceLeg.getTargetNode(), RouteNodeRole.SOURCE);
        RouteNodeSpec targetNode = resolveCaptureTargetNode(snapshot, sourceLeg);
        return buildReplayLeg(sourceLeg, replayRequest, sequence, RouteLegType.CONSUME, sourceNode, targetNode,
                replayRequest.getDescription());
    }

    private RouteLegSpec buildRefundLeg(RouteSnapshotSpec snapshot,
                                        RouteLegSpec sourceLeg,
                                        ReplayRequestSpec replayRequest,
                                        int sequence) {
        RouteNodeSpec capturedTargetNode = sourceLeg.getLegType() == RouteLegType.HOLD
                ? resolveCaptureTargetNode(snapshot, sourceLeg) : sourceLeg.getTargetNode();
        RouteNodeSpec sourceNode = copyNode(capturedTargetNode, RouteNodeRole.SOURCE);
        RouteNodeSpec targetNode = copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET);
        return buildReplayLeg(sourceLeg, replayRequest, sequence, RouteLegType.RESTORE, sourceNode, targetNode,
                replayRequest.getDescription());
    }

    private void assertSupportedSnapshotSchemaVersion(RouteSnapshotSpec snapshot) {
        AssertUtils.isTrue(FundsRouteCodes.CURRENT_ROUTE_SNAPSHOT_SCHEMA_VERSION
                        .equals(snapshot.getSnapshotSchemaVersion()),
                "RouteSnapshot snapshotSchemaVersion 不支持，snapshotSchemaVersion = {}",
                snapshot.getSnapshotSchemaVersion());
    }

    private RouteNodeSpec resolveCaptureTargetNode(RouteSnapshotSpec snapshot, RouteLegSpec sourceLeg) {
        if (sourceLeg.getSourceNode().getSubjectRef().getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT) {
            return copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET);
        }
        SubjectRef settlementAccount = resolveSettlementAccount(snapshot.getPlatformAccounts());
        if (settlementAccount != null) {
            return ImmutableRouteNodeSpec.builder()
                    .nodeType(RouteNodeType.PLATFORM_FUNDING_ACCOUNT)
                    .subjectRef(settlementAccount)
                    .nodeRole(RouteNodeRole.TARGET)
                    .build();
        }
        return copyNode(sourceLeg.getSourceNode(), RouteNodeRole.TARGET);
    }

    private @Nullable SubjectRef resolveSettlementAccount(@Nullable PlatformAccountsSnapshotSpec platformAccounts) {
        return platformAccounts == null ? null : platformAccounts.getSettlementFundingAccount();
    }

    private RouteLegSpec buildReplayLeg(RouteLegSpec sourceLeg,
                                        ReplayRequestSpec replayRequest,
                                        int sequence,
                                        RouteLegType legType,
                                        RouteNodeSpec sourceNode,
                                        RouteNodeSpec targetNode,
                                        @Nullable String description) {
        Money amount = resolveReplayAmount(sourceLeg, replayRequest);
        Money originalAmount = replayRequest.getOriginalAmount();
        BigDecimal exchangeRate = replayRequest.getExchangeRate();
        AssertUtils.isTrue((originalAmount == null) == (exchangeRate == null),
                "RouteSnapshot 回放原币金额和汇率必须同时提供");
        if (originalAmount == null) {
            originalAmount = sourceLeg.getOriginalAmount();
            exchangeRate = sourceLeg.getExchangeRate();
        } else {
            AssertUtils.isTrue(originalAmount.getCurrency() == sourceLeg.getOriginalAmount().getCurrency(),
                    "RouteSnapshot 回放原币币种必须与原 RouteLeg 一致，legId = {}", sourceLeg.getLegId());
        }
        if (isRefundReplay(replayRequest.getReplayType())) {
            AssertUtils.isTrue(exchangeRate.compareTo(sourceLeg.getExchangeRate()) == 0,
                    "RouteSnapshot 退款汇率必须与原支付快照汇率一致，legId = {}", sourceLeg.getLegId());
            AssertUtils.isTrue(originalAmount.getAmount() <= sourceLeg.getOriginalAmount().getAmount(),
                    "RouteSnapshot 退款原币金额不能大于原支付原币金额，legId = {}", sourceLeg.getLegId());
            AssertUtils.isTrue(amount.getAmount() == sourceLeg.getAmount().getAmount()
                            || originalAmount.getAmount() < sourceLeg.getOriginalAmount().getAmount(),
                    "RouteSnapshot 部分退款原币金额必须小于原支付原币金额，legId = {}", sourceLeg.getLegId());
        }
        return ImmutableRouteLegSpec.builder()
                .legId(legType.name() + REPLAY_LEG_ID_SEPARATOR + sourceLeg.getLegId())
                .sequence(sequence)
                .legType(legType)
                .sourceNode(sourceNode)
                .targetNode(targetNode)
                .amount(amount)
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .replayPolicy(sourceLeg.getReplayPolicy())
                .replayRefLegId(sourceLeg.getLegId())
                .description(description == null ? sourceLeg.getDescription() : description)
                .contextVariables(sourceLeg.getContextVariables())
                .build();
    }

    private boolean isRefundReplay(RouteReplayType replayType) {
        return replayType == RouteReplayType.REFUND
                || replayType == RouteReplayType.AUTHORIZATION_REFUND
                || replayType == RouteReplayType.FEE_REFUND;
    }

    private Money resolveReplayAmount(RouteLegSpec sourceLeg, ReplayRequestSpec replayRequest) {
        Money amount = replayRequest.getAmount() == null ? sourceLeg.getAmount() : replayRequest.getAmount();
        Money sourceAmount = sourceLeg.getAmount();
        AssertUtils.isTrue(amount.getAmount() > 0, "RouteSnapshot 回放金额必须大于 0");
        AssertUtils.isTrue(amount.getCurrency() == sourceAmount.getCurrency(),
                "RouteSnapshot 回放金额币种必须与原 RouteLeg 一致，legId = {}", sourceLeg.getLegId());
        switch (sourceLeg.getReplayPolicy()) {
            case FULL_ONLY, REPLAY_ONCE -> AssertUtils.isTrue(amount.getAmount() == sourceAmount.getAmount(),
                    "RouteSnapshot leg 仅支持全量回放，legId = {}", sourceLeg.getLegId());
            case PARTIAL_ALLOWED -> AssertUtils.isTrue(amount.getAmount() <= sourceAmount.getAmount(),
                    "RouteSnapshot 回放金额不能大于原 RouteLeg 金额，legId = {}", sourceLeg.getLegId());
            case NON_REPLAYABLE -> throw new IllegalArgumentException("RouteSnapshot leg 不支持回放，legId = "
                    + sourceLeg.getLegId());
        }
        return amount;
    }

    private RouteNodeSpec copyNode(RouteNodeSpec node, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(node.getNodeType())
                .subjectRef(node.getSubjectRef())
                .nodeRole(nodeRole)
                .build();
    }

    private List<RouteParticipantSpec> resolveParticipants(RouteSnapshotSpec snapshot,
                                                           List<RouteLegSpec> replayLegs,
                                                           ReplayRequestSpec replayRequest) {
        Map<String, RouteParticipantSpec> participants = new LinkedHashMap<>();
        Set<String> participantSubjects = new HashSet<>();
        Set<String> replaySubjects = replaySubjectKeys(replayLegs);
        for (RouteParticipantSpec participant : snapshot.getParticipants()) {
            if (!shouldReplayParticipant(participant, replayRequest)) {
                continue;
            }
            String participantSubject = subjectKey(participant.getSubjectRef());
            if (!replaySubjects.contains(participantSubject)) {
                continue;
            }
            participants.put(participantKey(participant.getSubjectRef(), participant.getParticipantRole()),
                    replayParticipant(participant, replayAmount(replayLegs, participant.getSubjectRef())));
            participantSubjects.add(participantSubject);
        }
        for (RouteLegSpec leg : replayLegs) {
            putParticipantIfAbsent(participants, participantSubjects, leg.getSourceNode(), RouteParticipantRole.PAYER,
                    leg.getAmount(), leg.getDescription());
            putParticipantIfAbsent(participants, participantSubjects, leg.getTargetNode(), RouteParticipantRole.PAYEE,
                    leg.getAmount(), leg.getDescription());
        }
        return List.copyOf(participants.values());
    }

    private boolean shouldReplayParticipant(RouteParticipantSpec participant,
                                            ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case FEE_REFUND -> participant.getParticipantRole() != RouteParticipantRole.PAYEE;
            case REFUND, AUTHORIZATION_REFUND -> participant.getParticipantRole() != RouteParticipantRole.FEE_RECEIVER;
            default -> true;
        };
    }

    private RouteParticipantSpec replayParticipant(RouteParticipantSpec participant, Money amount) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(participant.getParticipantRole())
                .subjectRef(participant.getSubjectRef())
                .ledgerProfileCode(participant.getLedgerProfileCode())
                .currency(amount.getCurrency())
                .amount(amount)
                .description(participant.getDescription())
                .accountHierarchySnapshot(participant.getAccountHierarchySnapshot())
                .contextVariables(participant.getContextVariables())
                .build();
    }

    private Money replayAmount(List<RouteLegSpec> replayLegs, SubjectRef subjectRef) {
        return replayLegs.stream()
                .filter(leg -> subjectKey(leg.getSourceNode().getSubjectRef()).equals(subjectKey(subjectRef))
                        || subjectKey(leg.getTargetNode().getSubjectRef()).equals(subjectKey(subjectRef)))
                .findFirst()
                .map(RouteLegSpec::getAmount)
                .orElseThrow();
    }

    private Set<String> replaySubjectKeys(List<RouteLegSpec> replayLegs) {
        Set<String> result = new HashSet<>();
        for (RouteLegSpec leg : replayLegs) {
            result.add(subjectKey(leg.getSourceNode().getSubjectRef()));
            result.add(subjectKey(leg.getTargetNode().getSubjectRef()));
        }
        return result;
    }

    private void putParticipantIfAbsent(Map<String, RouteParticipantSpec> participants,
                                        Set<String> participantSubjects,
                                        RouteNodeSpec node,
                                        RouteParticipantRole fallbackRole,
                                        Money amount,
                                        @Nullable String description) {
        String subjectKey = subjectKey(node.getSubjectRef());
        if (participantSubjects.contains(subjectKey)) {
            return;
        }
        RouteParticipantRole role = resolveParticipantRole(node, fallbackRole);
        String key = participantKey(node.getSubjectRef(), role);
        participants.putIfAbsent(key, ImmutableRouteParticipantSpec.builder()
                .participantRole(role)
                .subjectRef(node.getSubjectRef())
                .currency(amount.getCurrency())
                .amount(amount)
                .description(description)
                .contextVariables(Map.of())
                .build());
        participantSubjects.add(subjectKey);
    }

    private RouteParticipantRole resolveParticipantRole(RouteNodeSpec node, RouteParticipantRole fallbackRole) {
        if (node.getNodeType() == RouteNodeType.PLATFORM_FUNDING_ACCOUNT) {
            return RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT;
        }
        FundsSubjectType subjectType = node.getSubjectRef().getSubjectType();
        if (subjectType == FundsSubjectType.CREDIT_ACCOUNT) {
            return RouteParticipantRole.AUTH_HOLDER;
        }
        return fallbackRole;
    }

    private String participantKey(SubjectRef subjectRef, RouteParticipantRole role) {
        return role.name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectType().name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectId();
    }

    private String subjectKey(SubjectRef subjectRef) {
        return subjectRef.getSubjectType().name()
                + CONSTRAINT_KEY_SEPARATOR
                + subjectRef.getSubjectId();
    }

    private String resolveRouteCode(ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD -> FundsRouteCodes.AUTHORIZATION_REVERSAL_REPLAY;
            case AUTHORIZATION_COMPLETION -> FundsRouteCodes.AUTHORIZATION_COMPLETE_REPLAY;
            case AUTHORIZATION_REFUND -> FundsRouteCodes.AUTHORIZATION_REFUND_REPLAY;
            case REFUND, FEE_REFUND -> FundsRouteCodes.DIRECT_REFUND_REPLAY;
            case UNFREEZE -> FundsRouteCodes.BALANCE_UNFREEZE_REPLAY;
        };
    }

    private FundsInstructionType resolveInstructionType(RouteSnapshotSpec snapshot, ReplayRequestSpec replayRequest) {
        return replayRequest.getReplayType() == RouteReplayType.UNFREEZE
                ? FundsInstructionType.BALANCE_CONTROL : snapshot.getInstructionType();
    }

    private FundsTransactionEventType resolveEventType(ReplayRequestSpec replayRequest) {
        FundsTransactionEventType eventType = replayRequest.getEventType();
        if (eventType != null) {
            return eventType;
        }
        return switch (replayRequest.getReplayType()) {
            case RELEASE_HOLD -> FundsTransactionEventType.REVERSAL;
            case AUTHORIZATION_COMPLETION -> FundsTransactionEventType.COMPLETE;
            case AUTHORIZATION_REFUND -> FundsTransactionEventType.AUTH_REFUND;
            case REFUND -> FundsTransactionEventType.REFUND;
            case FEE_REFUND -> FundsTransactionEventType.FEE_REFUND;
            case UNFREEZE -> FundsTransactionEventType.UNFREEZE;
        };
    }

    private DefaultFundsTransactionType resolveTransactionType(RouteSnapshotSpec snapshot,
                                                              ReplayRequestSpec replayRequest) {
        return switch (replayRequest.getReplayType()) {
            case AUTHORIZATION_REFUND, REFUND, FEE_REFUND -> DefaultFundsTransactionType.REFUND;
            case UNFREEZE -> DefaultFundsTransactionType.BALANCE_CONTROL;
            default -> snapshot.getTransactionType();
        };
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 100;
    }
}
