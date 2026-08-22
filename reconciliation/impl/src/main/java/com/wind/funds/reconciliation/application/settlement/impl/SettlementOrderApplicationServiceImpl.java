package com.wind.funds.reconciliation.application.settlement.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.jackson.WindJson;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.application.settlement.SettlementOrderApplicationService;
import com.wind.funds.reconciliation.dal.entities.ClearingBatch;
import com.wind.funds.reconciliation.dal.entities.PayoutOrder;
import com.wind.funds.reconciliation.dal.entities.SettlementOrder;
import com.wind.funds.reconciliation.dal.entities.SettlementOrderItem;
import com.wind.funds.reconciliation.dal.mapper.ClearingBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.PayoutOrderMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationStageGateEvidenceMapper;
import com.wind.funds.reconciliation.dal.mapper.SettlementOrderItemMapper;
import com.wind.funds.reconciliation.dal.mapper.SettlementOrderMapper;
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.enums.SettlementReleaseCoverageStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import com.wind.funds.reconciliation.enums.SettlementReleaseLateDataStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseResultReplacementStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseLineageSupersessionStatus;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseAuthorityContextDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseDecisionDTO;
import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.dto.SettlementOrderItemDTO;
import com.wind.funds.reconciliation.model.dto.SettlementPolicySnapshotDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReturnSettlementOrderToDraftRequest;
import com.wind.funds.reconciliation.model.request.ReleaseSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.reconciliation.service.SettlementReleaseAuthority;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.application.FundsSettlementTransactionService;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsSettlementReleaseResultDTO;
import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.model.request.FundsSettlementReleaseRequest;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 结算单公共应用服务实现。
 */
@Service
@AllArgsConstructor
public class SettlementOrderApplicationServiceImpl implements SettlementOrderApplicationService {

    private static final WindSequenceType ORDER_SEQUENCE_TYPE =
            WindSequenceType.immutable("SETTLEMENT_ORDER", "STL", 6);

    private static final WindSequenceType ITEM_SEQUENCE_TYPE =
            WindSequenceType.immutable("SETTLEMENT_ORDER_ITEM", "STI", 6);

    private static final String SOURCE_TYPE = "CLEARING_BATCH";

    private static final String ITEM_TYPE = "PRINCIPAL";

    private static final String DIRECTION = "ADD";

    private static final int MAX_FAILURE_REASON_LENGTH = 512;

    private final SettlementOrderMapper settlementOrderMapper;

    private final SettlementOrderItemMapper settlementOrderItemMapper;

    private final PayoutOrderMapper payoutOrderMapper;

    private final ClearingBatchMapper clearingBatchMapper;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    private final ReconciliationStageGateEvidenceMapper stageGateEvidenceMapper;

    private final FundsSettlementTransactionService fundsSettlementTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    private final ObjectProvider<SettlementReleaseAuthority> settlementReleaseAuthorityProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO createOrder(CreateSettlementOrderRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        List<String> sourceSns = request.getClearingBatchSns().stream().sorted().toList();
        AssertUtils.isTrue(sourceSns.stream().distinct().count() == sourceSns.size(),
                "结算来源清算批次流水号不能重复");
        List<ClearingBatch> sources = clearingBatchMapper.selectBySnsForUpdate(request.getTenantId(), sourceSns);
        AssertUtils.isTrue(sources.size() == sourceSns.size(), "存在未找到的结算来源清算批次");
        validateSourceBoundary(sources);

        SettlementOrder candidate = newOrder(request, sources, operator);
        SettlementOrder existing = settlementOrderMapper.selectByDigest(
                request.getTenantId(), candidate.getOrderDigest());
        if (existing != null) {
            return toDTO(existing);
        }
        AssertUtils.isTrue(settlementOrderItemMapper.countActiveSourceClaims(
                request.getTenantId(), sourceSns) == 0, "清算来源已被其他有效结算单占用");
        settlementOrderMapper.insertSelective(candidate);
        AssertUtils.notNull(candidate.getId(), "创建结算单失败");
        for (ClearingBatch source : sources) {
            settlementOrderItemMapper.insertSelective(newItem(candidate, source, operator));
        }
        return toDTO(requiredOrder(request.getTenantId(), candidate.getSn()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO submitOrder(SubmitSettlementOrderRequest request, WindOperator operator) {
        validateCommand(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSettlementOrderSn(), operator);
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        if (order.getState() == SettlementOrderState.REVIEWING) {
            return toDTO(order);
        }
        AssertUtils.isTrue(order.getState() == SettlementOrderState.DRAFT,
                "只有 DRAFT 结算单可以提交复核，status = {}", order.getState());
        order.setState(SettlementOrderState.REVIEWING);
        order.setSubmittedBy(operator.getOperatorAsText());
        order.setSubmittedTime(LocalDateTime.now());
        update(order, "提交结算单复核失败");
        return toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO returnToDraft(ReturnSettlementOrderToDraftRequest request, WindOperator operator) {
        validateCommand(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSettlementOrderSn(), operator);
        validateReason(request.getReason(), ReturnSettlementOrderToDraftRequest.MAX_REASON_LENGTH, "结算单退回原因");
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        if (order.getState() == SettlementOrderState.DRAFT) {
            return toDTO(order);
        }
        AssertUtils.isTrue(order.getState() == SettlementOrderState.REVIEWING,
                "只有 REVIEWING 结算单可以退回草稿，status = {}", order.getState());
        order.setState(SettlementOrderState.DRAFT);
        order.setReturnedBy(operator.getOperatorAsText());
        order.setReturnedTime(LocalDateTime.now());
        order.setReturnReason(request.getReason());
        update(order, "退回结算单草稿失败");
        return toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO approveOrder(ApproveSettlementOrderRequest request, WindOperator operator) {
        validateCommand(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSettlementOrderSn(), operator);
        AssertUtils.hasText(request.getSettlementApprovalRef(), "结算审批引用不能为空");
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        if (order.getState() == SettlementOrderState.APPROVED) {
            AssertUtils.equals(order.getSettlementApprovalRef(), request.getSettlementApprovalRef(),
                    "结算单已使用不同审批引用完成审批");
            return toDTO(order);
        }
        AssertUtils.isTrue(order.getState() == SettlementOrderState.REVIEWING,
                "只有 REVIEWING 结算单可以审批，status = {}", order.getState());
        order.setState(SettlementOrderState.APPROVED);
        order.setSettlementApprovalRef(request.getSettlementApprovalRef());
        order.setApprovedBy(operator.getOperatorAsText());
        order.setApprovedTime(LocalDateTime.now());
        update(order, "审批结算单失败");
        return toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO cancelOrder(CancelSettlementOrderRequest request, WindOperator operator) {
        validateCommand(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSettlementOrderSn(), operator);
        validateReason(request.getReason(), CancelSettlementOrderRequest.MAX_REASON_LENGTH, "结算单取消原因");
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        if (order.getState() == SettlementOrderState.CANCELLED) {
            return toDTO(order);
        }
        AssertUtils.isTrue(order.getState() == SettlementOrderState.DRAFT
                        || order.getState() == SettlementOrderState.REVIEWING,
                "只有 DRAFT 或 REVIEWING 结算单可以取消，status = {}", order.getState());
        List<SettlementOrderItem> items = requiredItems(order);
        AssertUtils.isTrue(settlementOrderItemMapper.releaseActiveSourceClaims(
                order.getTenantId(), order.getSn()) == items.size(), "释放结算来源占用失败");
        order.setState(SettlementOrderState.CANCELLED);
        releaseActiveOrderDigest(order);
        order.setCancelledBy(operator.getOperatorAsText());
        order.setCancelledTime(LocalDateTime.now());
        order.setCancelReason(request.getReason());
        update(order, "取消结算单失败");
        return toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public SettlementOrderDTO lockOrder(LockSettlementOrderRequest request, WindOperator operator) {
        validateCommand(request == null ? null : request.getTenantId(),
                request == null ? null : request.getSettlementOrderSn(), operator);
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        if (order.getState() == SettlementOrderState.LOCKED) {
            return toDTO(order);
        }
        AssertUtils.isTrue(order.getState() == SettlementOrderState.APPROVED,
                "只有 APPROVED 结算单可以锁定资金，status = {}", order.getState());
        List<SettlementOrderItem> items = requiredItems(order);
        validateCurrentSources(order, items);
        validateDigests(order, items);
        ReconciliationGateDecisionDTO decision = reconciliationGateApplicationService.checkGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(order.getTenantId())
                        .setStageRef(stageRef("SETTLEMENT_LOCK", order.getSn())),
                operator);
        AssertUtils.isTrue(decision.isPassed(), "结算锁定时对账 Gate 未通过");
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "结算锁定对账证据引用不能为空");
        try {
            String fundsTransactionSn = fundsSettlementTransactionService.lock(new FundsSettlementLockRequest()
                    .setAccountId(FundsAccountId.immutable(
                            order.getSettlementSubjectId(), order.getSettlementSubjectType()))
                    .setAmount(Money.immutable(order.getNetAmount(), order.getCurrency()))
                    .setSettlementOrderSn(order.getSn())
                    .setDescription("settlement order funds lock"), operator);
            applyGateEvidence(order, decision);
            order.setState(SettlementOrderState.LOCKED);
            order.setLockFundsTransactionSn(fundsTransactionSn);
            order.setLockedBy(operator.getOperatorAsText());
            order.setLockedTime(LocalDateTime.now());
            update(order, "锁定结算单资金失败");
            return toDTO(order);
        } catch (LedgerPostingRejectedException exception) {
            recordDeterministicFailure(order, items, exception, operator);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrderDTO releaseOrder(ReleaseSettlementOrderRequest request, WindOperator operator) {
        validateReleaseRequest(request, operator);
        SettlementOrder order = requiredOrderForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        PayoutOrder payoutOrder = payoutOrderMapper.selectBySettlementOrderSnForUpdate(
                order.getTenantId(), order.getSn());
        String releaseDigest = releaseDigest(order, request);
        SettlementOrderDTO replay = completedReleaseReplay(order, releaseDigest);
        if (replay != null) {
            return replay;
        }

        AssertUtils.isTrue(order.getState() == SettlementOrderState.LOCKED,
                "只有 LOCKED 结算单可以释放资金，status = {}", order.getState());
        AssertUtils.hasText(order.getLockFundsTransactionSn(), "结算单缺少原锁定资金交易流水号");
        if (payoutOrder != null) {
            AssertUtils.isTrue(payoutOrder.getState() == PayoutOrderState.CREATED,
                    "出款单状态不允许释放结算资金，payoutOrderSn = {}, status = {}",
                    payoutOrder.getSn(), payoutOrder.getState());
        }

        ReconciliationGateDecisionDTO gateDecision = reconciliationGateApplicationService.checkGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(order.getTenantId())
                        .setStageRef(stageRef("SETTLEMENT_RELEASE", order.getSn())),
                operator);
        validateReleaseGate(request, gateDecision);
        String routeSnapshotDigest = validateOriginalLock(order);

        SettlementReleaseAuthority authority = settlementReleaseAuthorityProvider.getIfUnique();
        AssertUtils.notNull(authority, "宿主权威结算释放授权服务未配置或配置不唯一");
        SettlementReleaseDecisionDTO authorityDecision = authority.authorize(
                new SettlementReleaseAuthorityContextDTO()
                        .setSettlementOrder(toDTO(order))
                        .setPayoutOrder(toPayoutDTO(payoutOrder))
                        .setRequest(request)
                        .setGateDecision(gateDecision)
                        .setOriginalLockRouteSnapshotDigest(routeSnapshotDigest)
                        .setReleaseRequestDigest(releaseDigest),
                operator);
        validateReleaseDecision(authorityDecision);

        FundsSettlementReleaseResultDTO fundsResult = fundsSettlementTransactionService.release(
                new FundsSettlementReleaseRequest()
                        .setLockFundsTransactionSn(order.getLockFundsTransactionSn())
                        .setSettlementOrderSn(order.getSn()),
                operator);
        AssertUtils.hasText(fundsResult.getReleaseFundsTransactionSn(), "结算释放缺少资金交易流水号");
        AssertUtils.hasText(fundsResult.getReleaseFreezeOrderSn(), "结算释放缺少受保护冻结单流水号");

        LocalDateTime releasedAt = LocalDateTime.now();
        if (payoutOrder != null) {
            payoutOrder.setState(PayoutOrderState.CANCELLED);
            payoutOrder.setCancelledBy(operator.getOperatorAsText());
            payoutOrder.setCancelledTime(releasedAt);
            payoutOrder.setCancelReason(request.getReason());
            AssertUtils.isTrue(payoutOrderMapper.update(payoutOrder) == 1, "取消待提交出款单失败");
        }
        List<SettlementOrderItem> items = requiredItems(order);
        AssertUtils.isTrue(settlementOrderItemMapper.releaseActiveSourceClaims(
                order.getTenantId(), order.getSn()) == items.size(), "释放结算来源占用失败");
        releaseActiveOrderDigest(order);

        order.setState(SettlementOrderState.RELEASED);
        order.setReleaseFundsTransactionSn(fundsResult.getReleaseFundsTransactionSn());
        order.setReleaseFreezeOrderSn(fundsResult.getReleaseFreezeOrderSn());
        order.setReleaseDisposition(SettlementReleaseDisposition.FROZEN);
        order.setReleaseDigest(releaseDigest);
        order.setReleaseGateEvidenceRef(requiredStageEvidenceRef(gateDecision));
        order.setReleaseSourceClosureDigest(sourceClosureDigest(request));
        order.setReleaseAuthorityDecisionDigest(authorityDecision.getDecisionDigest());
        order.setReleaseAuthorityEvidenceRefs(WindJson.toJsonString(
                authorityDecision.getEvidenceRefs().stream().sorted().toList()));
        order.setReleaseApprovalRef(request.getApprovalRef());
        order.setReleaseReason(request.getReason().trim());
        order.setReleasedBy(operator.getOperatorAsText());
        order.setReleasedTime(releasedAt);
        update(order, "记录结算释放结果失败");
        return toDTO(order);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public SettlementOrderDTO getOrder(Long tenantId, String settlementOrderSn) {
        validateQuery(tenantId, settlementOrderSn);
        return toDTO(requiredOrder(tenantId, settlementOrderSn));
    }

    private SettlementOrderDTO completedReleaseReplay(SettlementOrder order, String releaseDigest) {
        boolean hasFundsReference = StringUtils.hasText(order.getReleaseFundsTransactionSn());
        boolean hasFreezeReference = StringUtils.hasText(order.getReleaseFreezeOrderSn());
        if (!hasFundsReference && !hasFreezeReference) {
            AssertUtils.isTrue(order.getReleaseDigest() == null && order.getReleaseDisposition() == null,
                    "结算单存在不完整释放审计事实，settlementOrderSn = {}", order.getSn());
            return null;
        }
        AssertUtils.isTrue(hasFundsReference && hasFreezeReference,
                "结算单释放资金事实引用不完整，settlementOrderSn = {}", order.getSn());
        AssertUtils.equals(order.getReleaseDigest(), releaseDigest, "结算单已使用不同释放请求完成释放");
        AssertUtils.isTrue(order.getState() == SettlementOrderState.RELEASED
                        && order.getReleaseDisposition() == SettlementReleaseDisposition.FROZEN,
                "结算单释放完成态事实不一致，settlementOrderSn = {}", order.getSn());
        return toDTO(order);
    }

    private void validateReleaseGate(ReleaseSettlementOrderRequest request,
                                     ReconciliationGateDecisionDTO decision) {
        AssertUtils.isTrue(decision.isPassed(), "结算释放时对账 Gate 未通过：{}", decision.getExplanation());
        AssertUtils.equals(stageRef("SETTLEMENT_RELEASE", request.getSettlementOrderSn()),
                decision.getStageRef(), "结算释放 Gate Stage 不一致");
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "结算释放 Gate 证据引用不能为空");
        AssertUtils.isTrue(decision.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "结算释放 Gate 证据引用不能为空");
    }

    private String validateOriginalLock(SettlementOrder order) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(
                order.getLockFundsTransactionSn()).orElse(null);
        AssertUtils.notNull(transaction, "结算单原锁定资金交易不存在，transactionSn = {}",
                order.getLockFundsTransactionSn());
        AssertUtils.isTrue(Objects.equals(transaction.getTenantId(), order.getTenantId())
                        && transaction.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT
                        && transaction.getState() == FundsTransactionState.CLOSED
                        && FundsTransactionEventType.SETTLEMENT_LOCK.name().equals(transaction.getBusinessScene())
                        && Objects.equals(transaction.getBusinessSn(), order.getSn())
                        && Objects.equals(transaction.getAmount(), order.getNetAmount())
                        && transaction.getCurrency() == order.getCurrency(),
                "结算单与原 SETTLEMENT_LOCK 资金事实不一致，transactionSn = {}", transaction.getSn());
        AssertUtils.hasText(transaction.getRouteSnapshot(), "原 SETTLEMENT_LOCK 缺少 RouteSnapshot");
        RouteSnapshotSpec route = fundsTransactionQueryService.findRouteSnapshotByTransactionSn(transaction.getSn())
                .orElseThrow(() -> new IllegalArgumentException(
                        "原 SETTLEMENT_LOCK RouteSnapshot 不可读取，transactionSn = " + transaction.getSn()));
        AssertUtils.isTrue(route.getEventType() == FundsTransactionEventType.SETTLEMENT_LOCK
                        && route.getTransactionType() == DefaultFundsTransactionType.SETTLEMENT
                        && route.getParticipants().size() == 1
                        && route.getParticipants().getFirst().getParticipantRole() == RouteParticipantRole.PAYER,
                "原 SETTLEMENT_LOCK RouteSnapshot 主体事实不合法，transactionSn = {}", transaction.getSn());
        var participant = route.getParticipants().getFirst();
        AssertUtils.notNull(participant.getAmount(),
                "原 SETTLEMENT_LOCK RouteSnapshot 缺少主体金额，transactionSn = {}", transaction.getSn());
        var subjectRef = participant.getSubjectRef();
        AssertUtils.isTrue(Objects.equals(subjectRef.getSubjectId(), order.getSettlementSubjectId())
                        && subjectRef.getSubjectType().name().equals(order.getSettlementSubjectType())
                        && participant.getAmount().getAmount() == order.getNetAmount()
                        && participant.getAmount().getCurrency() == order.getCurrency(),
                "原 SETTLEMENT_LOCK RouteSnapshot 与结算主体或金额不一致，transactionSn = {}", transaction.getSn());
        AssertUtils.isTrue(route.getLegs().size() == 1,
                "原 SETTLEMENT_LOCK RouteSnapshot 必须包含唯一资金路径，transactionSn = {}", transaction.getSn());
        RouteLegSpec leg = route.getLegs().getFirst();
        AssertUtils.isTrue(sameSubject(leg.getSourceNode().getSubjectRef(), subjectRef)
                        && sameSubject(leg.getTargetNode().getSubjectRef(), subjectRef),
                "原 SETTLEMENT_LOCK RouteSnapshot 资金路径不合法，transactionSn = {}", transaction.getSn());
        return FundsStableHashSupport.sha256(transaction.getRouteSnapshot());
    }

    private boolean sameSubject(SubjectRef left, SubjectRef right) {
        return Objects.equals(left.getSubjectId(), right.getSubjectId())
                && left.getSubjectType() == right.getSubjectType();
    }

    private void validateReleaseDecision(SettlementReleaseDecisionDTO decision) {
        AssertUtils.notNull(decision, "宿主权威结算释放授权结果不能为空");
        AssertUtils.isTrue(decision.isReleaseAllowed(), "宿主权威结算释放授权未通过：{}",
                decision.getBlockingReason());
        AssertUtils.isTrue(decision.getReleaseDisposition() == SettlementReleaseDisposition.FROZEN,
                "结算释放处置必须为 FROZEN");
        AssertUtils.isTrue(isSha256(decision.getDecisionDigest()), "结算释放授权决策摘要必须是 SHA-256");
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "结算释放授权证据引用不能为空");
        AssertUtils.isTrue(decision.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "结算释放授权证据引用不能为空");
        AssertUtils.hasText(decision.getAuthorizedBy(), "结算释放授权人不能为空");
        AssertUtils.notNull(decision.getAuthorizedAt(), "结算释放授权时间不能为空");
        AssertUtils.notNull(decision.getExpiresAt(), "结算释放授权有效期不能为空");
        LocalDateTime now = LocalDateTime.now();
        AssertUtils.isFalse(decision.getAuthorizedAt().isAfter(now), "结算释放授权时间不能晚于当前时间");
        AssertUtils.isTrue(decision.getExpiresAt().isAfter(now), "结算释放授权结果已过期");
        AssertUtils.isFalse(decision.getAuthorizedAt().isAfter(decision.getExpiresAt()),
                "结算释放授权时间不能晚于有效期");
    }

    private String releaseDigest(SettlementOrder order, ReleaseSettlementOrderRequest request) {
        Map<String, Object> facts = releaseRequestFacts(request);
        facts.put("orderDigest", order.getOrderDigest());
        facts.put("lockFundsTransactionSn", order.getLockFundsTransactionSn());
        return FundsStableHashSupport.sha256CanonicalJson("settlement-release-request", facts);
    }

    private String sourceClosureDigest(ReleaseSettlementOrderRequest request) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("coverageStatus", request.getCoverageStatus());
        facts.put("coverageDigest", request.getCoverageDigest());
        facts.put("watermark", request.getWatermark());
        facts.put("cutoff", request.getCutoff());
        facts.put("ruleVersion", request.getRuleVersion());
        facts.put("ruleDecisionDigest", request.getRuleDecisionDigest());
        facts.put("lateDataStatus", request.getLateDataStatus());
        facts.put("resultReplacementStatus", request.getResultReplacementStatus());
        facts.put("lineageSupersessionStatus", request.getLineageSupersessionStatus());
        facts.put("evidenceRefs", request.getEvidenceRefs().stream().sorted().toList());
        return FundsStableHashSupport.sha256CanonicalJson("settlement-release-source-closure", facts);
    }

    private Map<String, Object> releaseRequestFacts(ReleaseSettlementOrderRequest request) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", request.getTenantId());
        facts.put("settlementOrderSn", request.getSettlementOrderSn());
        facts.put("coverageStatus", request.getCoverageStatus());
        facts.put("coverageDigest", request.getCoverageDigest());
        facts.put("watermark", request.getWatermark());
        facts.put("cutoff", request.getCutoff());
        facts.put("ruleVersion", request.getRuleVersion());
        facts.put("ruleDecisionDigest", request.getRuleDecisionDigest());
        facts.put("currentLineageBatchSn", request.getCurrentLineageBatchSn());
        facts.put("lateDataStatus", request.getLateDataStatus());
        facts.put("resultReplacementStatus", request.getResultReplacementStatus());
        facts.put("lineageSupersessionStatus", request.getLineageSupersessionStatus());
        facts.put("approvalRef", request.getApprovalRef());
        facts.put("reason", request.getReason().trim());
        facts.put("evidenceRefs", request.getEvidenceRefs().stream().sorted().toList());
        return facts;
    }

    private void validateReleaseRequest(ReleaseSettlementOrderRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "结算释放请求不能为空");
        validateCommand(request.getTenantId(), request.getSettlementOrderSn(), operator);
        AssertUtils.isTrue(request.getCoverageStatus() == SettlementReleaseCoverageStatus.COMPLETE,
                "结算释放来源覆盖状态必须为 COMPLETE");
        AssertUtils.isTrue(isSha256(request.getCoverageDigest()), "结算释放来源覆盖摘要必须是 SHA-256");
        AssertUtils.notNull(request.getWatermark(), "结算释放来源 watermark 不能为空");
        AssertUtils.notNull(request.getCutoff(), "结算释放来源 cutoff 不能为空");
        AssertUtils.isFalse(request.getWatermark().isAfter(request.getCutoff()),
                "结算释放来源 watermark 不能晚于 cutoff");
        AssertUtils.hasText(request.getRuleVersion(), "结算释放规则版本不能为空");
        AssertUtils.isTrue(isSha256(request.getRuleDecisionDigest()), "结算释放规则决策摘要必须是 SHA-256");
        AssertUtils.hasText(request.getCurrentLineageBatchSn(), "结算释放当前血缘批次流水号不能为空");
        AssertUtils.isTrue(request.getLateDataStatus() == SettlementReleaseLateDataStatus.CLOSED,
                "结算释放迟到数据状态必须为 CLOSED");
        AssertUtils.isTrue(request.getResultReplacementStatus() == SettlementReleaseResultReplacementStatus.CURRENT,
                "结算释放替代状态必须为 CURRENT");
        AssertUtils.isTrue(request.getLineageSupersessionStatus() == SettlementReleaseLineageSupersessionStatus.CURRENT,
                "结算释放取代状态必须为 CURRENT");
        AssertUtils.hasText(request.getApprovalRef(), "结算释放审批引用不能为空");
        validateReason(request.getReason(), ReleaseSettlementOrderRequest.MAX_REASON_LENGTH, "结算释放原因");
        AssertUtils.notEmpty(request.getEvidenceRefs(), "结算释放来源证据引用不能为空");
        AssertUtils.isTrue(request.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "结算释放来源证据引用不能为空");
    }

    private boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    private PayoutOrderDTO toPayoutDTO(PayoutOrder source) {
        if (source == null) {
            return null;
        }
        return new PayoutOrderDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSettlementOrderSn(source.getSettlementOrderSn())
                .setSettlementSubjectType(source.getSettlementSubjectType())
                .setSettlementSubjectId(source.getSettlementSubjectId())
                .setAmount(source.getAmount())
                .setCurrency(source.getCurrency())
                .setState(source.getState());
    }

    private SettlementOrder newOrder(CreateSettlementOrderRequest request,
                                     List<ClearingBatch> sources,
                                     WindOperator operator) {
        ClearingBatch first = sources.getFirst();
        long totalAmount = sources.stream().map(ClearingBatch::getTotalAmount).reduce(0L, Math::addExact);
        SettlementOrder result = new SettlementOrder();
        result.setSn(TemporalSequenceFactory.hourNext(ORDER_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setSettlementSubjectType(first.getSubjectType());
        result.setSettlementSubjectId(first.getSubjectId());
        result.setCurrency(first.getCurrency());
        result.setSettlementPeriod(request.getSettlementPeriod());
        result.setSettlementMode(request.getSettlementMode());
        result.setSettlementDestination(request.getSettlementDestination());
        result.setTriggerMode(request.getTriggerMode());
        result.setTimezone(request.getTimezone());
        result.setCutoff(request.getCutoff());
        result.setTotalAmount(totalAmount);
        result.setAddAmount(totalAmount);
        result.setDeductAmount(0L);
        result.setReserveAmount(0L);
        result.setNetAmount(totalAmount);
        result.setState(SettlementOrderState.DRAFT);
        result.setRuleCode(request.getPolicyCode());
        result.setRuleVersion(request.getPolicyVersion());
        result.setPolicyApprovalRef(request.getPolicyApprovalRef());
        List<SettlementOrderItem> items = sources.stream()
                .map(source -> newItem(result, source, operator))
                .toList();
        result.setAmountDigest(amountDigest(items));
        result.setSourceDigest(sourceDigest(items));
        result.setPolicySnapshotDigest(policySnapshotDigest(result));
        result.setOrderDigest(orderDigest(result));
        result.setActiveOrderDigest(result.getOrderDigest());
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private SettlementOrderItem newItem(SettlementOrder order, ClearingBatch source, WindOperator operator) {
        SettlementOrderItem result = new SettlementOrderItem();
        result.setSn(TemporalSequenceFactory.hourNext(ITEM_SEQUENCE_TYPE));
        result.setTenantId(order.getTenantId());
        result.setSettlementOrderSn(order.getSn());
        result.setItemType(ITEM_TYPE);
        result.setDirection(DIRECTION);
        result.setSourceType(SOURCE_TYPE);
        result.setSourceSn(source.getSn());
        result.setAmount(source.getTotalAmount());
        result.setCurrency(source.getCurrency());
        result.setSourceAmountDigest(source.getAmountDigest());
        result.setActiveSourceClaim(1);
        result.setCreatedBy(operator.getOperatorAsText());
        return result;
    }

    private void validateCurrentSources(SettlementOrder order, List<SettlementOrderItem> items) {
        List<String> sourceSns = items.stream().map(SettlementOrderItem::getSourceSn).sorted().toList();
        List<ClearingBatch> sources = clearingBatchMapper.selectBySnsForUpdate(order.getTenantId(), sourceSns);
        AssertUtils.isTrue(sources.size() == sourceSns.size(), "存在已丢失的结算来源清算批次");
        validateSourceBoundary(sources);
        for (SettlementOrderItem item : items) {
            ClearingBatch source = sources.stream()
                    .filter(candidate -> candidate.getSn().equals(item.getSourceSn()))
                    .findFirst()
                    .orElseThrow();
            AssertUtils.isTrue(Objects.equals(item.getAmount(), source.getTotalAmount())
                            && Objects.equals(item.getSourceAmountDigest(), source.getAmountDigest()),
                    "结算来源金额已变化，clearingBatchSn = {}", item.getSourceSn());
        }
    }

    private void validateDigests(SettlementOrder order, List<SettlementOrderItem> items) {
        AssertUtils.equals(order.getAmountDigest(), amountDigest(items), "结算单金额摘要已变化");
        AssertUtils.equals(order.getSourceDigest(), sourceDigest(items), "结算单来源摘要已变化");
        AssertUtils.equals(order.getPolicySnapshotDigest(), policySnapshotDigest(order), "结算策略快照摘要已变化");
        AssertUtils.equals(order.getOrderDigest(), orderDigest(order), "结算单幂等摘要已变化");
    }

    private void validateSourceBoundary(List<ClearingBatch> sources) {
        AssertUtils.notEmpty(sources, "结算来源清算批次不能为空");
        ClearingBatch first = sources.getFirst();
        for (ClearingBatch source : sources) {
            AssertUtils.isTrue(source.getState() == ClearingBatchState.CONFIRMED,
                    "结算来源清算批次必须为 CONFIRMED，clearingBatchSn = {}", source.getSn());
            AssertUtils.isTrue(source.getTotalAmount() != null && source.getTotalAmount() > 0,
                    "结算来源金额必须大于 0，clearingBatchSn = {}", source.getSn());
            AssertUtils.isTrue(Objects.equals(first.getSubjectType(), source.getSubjectType())
                            && Objects.equals(first.getSubjectId(), source.getSubjectId()),
                    "一个结算单必须属于同一结算主体");
            AssertUtils.isTrue(first.getCurrency() == source.getCurrency(), "一个结算单必须使用同一币种");
        }
    }

    private String amountDigest(List<SettlementOrderItem> items) {
        return FundsStableHashSupport.sha256Json(items.stream()
                .sorted(Comparator.comparing(SettlementOrderItem::getSourceSn))
                .map(item -> Map.of(
                        "itemType", item.getItemType(),
                        "direction", item.getDirection(),
                        "sourceType", item.getSourceType(),
                        "sourceSn", item.getSourceSn(),
                        "amount", item.getAmount(),
                        "currency", item.getCurrency(),
                        "sourceAmountDigest", item.getSourceAmountDigest()))
                .toList());
    }

    private String sourceDigest(List<SettlementOrderItem> items) {
        return FundsStableHashSupport.sha256Json(items.stream()
                .map(SettlementOrderItem::getSourceSn)
                .sorted()
                .toList());
    }

    private String policySnapshotDigest(SettlementOrder order) {
        Map<String, Object> snapshot = new TreeMap<>();
        snapshot.put("policyCode", order.getRuleCode());
        snapshot.put("policyVersion", order.getRuleVersion());
        snapshot.put("settlementMode", order.getSettlementMode());
        snapshot.put("settlementDestination", order.getSettlementDestination());
        snapshot.put("triggerMode", order.getTriggerMode());
        snapshot.put("settlementPeriod", order.getSettlementPeriod());
        snapshot.put("timezone", order.getTimezone());
        snapshot.put("cutoff", order.getCutoff());
        snapshot.put("policyApprovalRef", order.getPolicyApprovalRef());
        return FundsStableHashSupport.sha256Json(snapshot);
    }

    private String orderDigest(SettlementOrder order) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("tenantId", order.getTenantId());
        facts.put("settlementSubjectType", order.getSettlementSubjectType());
        facts.put("settlementSubjectId", order.getSettlementSubjectId());
        facts.put("currency", order.getCurrency());
        facts.put("settlementPeriod", order.getSettlementPeriod());
        facts.put("netAmount", order.getNetAmount());
        facts.put("amountDigest", order.getAmountDigest());
        facts.put("sourceDigest", order.getSourceDigest());
        facts.put("policySnapshotDigest", order.getPolicySnapshotDigest());
        return FundsStableHashSupport.sha256Json(facts);
    }

    private void applyGateEvidence(SettlementOrder order, ReconciliationGateDecisionDTO decision) {
        order.setLockGateEvidenceRef(requiredStageEvidenceRef(decision));
    }

    private String requiredStageEvidenceRef(ReconciliationGateDecisionDTO decision) {
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "Gate 通过时必须持有消费证据");
        return decision.getEvidenceRefs().getFirst();
    }

    private GateStageRef stageRef(String stageKind, String settlementOrderSn) {
        return new GateStageRef()
                .setStageKind(stageKind)
                .setStageIdentity(new StableIdentity()
                        .setOwnerNamespace("settlement-order")
                        .setValue(settlementOrderSn));
    }

    private void recordDeterministicFailure(SettlementOrder order,
                                            List<SettlementOrderItem> items,
                                            LedgerPostingRejectedException exception,
                                            WindOperator operator) {
        FundsTransactionDTO transaction = fundsTransactionQueryService.queryFundsTransaction(
                exception.getFundsTransactionSn()).orElse(null);
        if (transaction == null || transaction.getState() != FundsTransactionState.FAILED) {
            throw new IllegalStateException("结算资金结果未知，不能释放结算来源", exception);
        }
        AssertUtils.isTrue(settlementOrderItemMapper.releaseActiveSourceClaims(
                order.getTenantId(), order.getSn()) == items.size(), "释放结算来源占用失败");
        AssertUtils.isTrue(stageGateEvidenceMapper.deleteByStage(order.getTenantId(),
                        "SETTLEMENT_LOCK", "settlement-order", order.getSn()) == 1,
                "删除失败结算动作的 Gate 成功消费证据失败");
        order.setLockFundsTransactionSn(transaction.getSn());
        order.setState(SettlementOrderState.FAILED);
        releaseActiveOrderDigest(order);
        order.setFailedBy(operator.getOperatorAsText());
        order.setFailedTime(LocalDateTime.now());
        order.setFailureReason(truncate(exception.getMessage(), MAX_FAILURE_REASON_LENGTH));
        update(order, "记录结算单明确失败事实失败");
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void releaseActiveOrderDigest(SettlementOrder order) {
        AssertUtils.isTrue(settlementOrderMapper.releaseActiveOrderDigest(
                order.getTenantId(), order.getSn()) == 1, "释放结算单活动幂等占用失败");
        order.setActiveOrderDigest(null);
    }

    private List<SettlementOrderItem> requiredItems(SettlementOrder order) {
        List<SettlementOrderItem> items = settlementOrderItemMapper.selectByOrderSn(
                order.getTenantId(), order.getSn());
        AssertUtils.notEmpty(items, "结算单金额项不能为空");
        return items;
    }

    private SettlementOrder requiredOrder(Long tenantId, String settlementOrderSn) {
        SettlementOrder result = settlementOrderMapper.selectBySn(tenantId, settlementOrderSn);
        AssertUtils.notNull(result, "结算单不存在，settlementOrderSn = {}", settlementOrderSn);
        return result;
    }

    private SettlementOrder requiredOrderForUpdate(Long tenantId, String settlementOrderSn) {
        SettlementOrder result = settlementOrderMapper.selectBySnForUpdate(tenantId, settlementOrderSn);
        AssertUtils.notNull(result, "结算单不存在，settlementOrderSn = {}", settlementOrderSn);
        return result;
    }

    private void update(SettlementOrder order, String message) {
        AssertUtils.isTrue(settlementOrderMapper.update(order) == 1, message);
    }

    private void validateCreateRequest(CreateSettlementOrderRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建结算单请求不能为空");
        AssertUtils.notNull(operator, "结算单操作人不能为空");
        validateTenant(request.getTenantId());
        AssertUtils.notEmpty(request.getClearingBatchSns(), "结算来源清算批次不能为空");
        AssertUtils.isTrue(request.getClearingBatchSns().size() <= CreateSettlementOrderRequest.MAX_CLEARING_BATCH_COUNT,
                "结算来源清算批次数量不能超过 {}", CreateSettlementOrderRequest.MAX_CLEARING_BATCH_COUNT);
        AssertUtils.isTrue(request.getClearingBatchSns().stream().allMatch(StringUtils::hasText),
                "结算来源清算批次流水号不能为空");
        AssertUtils.hasText(request.getSettlementPeriod(), "结算周期不能为空");
        AssertUtils.isTrue(request.getSettlementMode() == SettlementMode.INTERMEDIARY_ACCOUNT,
                "当前只支持中间户模式");
        AssertUtils.notNull(request.getSettlementDestination(), "结算去向不能为空");
        AssertUtils.isTrue(request.getTriggerMode() == SettlementTriggerMode.HOST_COMMAND,
                "当前只支持宿主命令触发结算");
        AssertUtils.hasText(request.getTimezone(), "结算策略时区不能为空");
        AssertUtils.hasText(request.getCutoff(), "结算 cutoff 不能为空");
        AssertUtils.hasText(request.getPolicyCode(), "结算策略编码不能为空");
        AssertUtils.hasText(request.getPolicyVersion(), "结算策略版本不能为空");
    }

    private void validateCommand(Long tenantId, String settlementOrderSn, WindOperator operator) {
        validateQuery(tenantId, settlementOrderSn);
        AssertUtils.notNull(operator, "结算单操作人不能为空");
    }

    private void validateQuery(Long tenantId, String settlementOrderSn) {
        validateTenant(tenantId);
        AssertUtils.hasText(settlementOrderSn, "结算单流水号不能为空");
    }

    private void validateTenant(Long tenantId) {
        AssertUtils.notNull(tenantId, "结算单租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId,
                "结算单 tenantId 与当前租户不一致");
    }

    private void validateReason(String reason, int maxLength, String fieldName) {
        AssertUtils.hasText(reason, "{}不能为空", fieldName);
        AssertUtils.isTrue(reason.trim().length() <= maxLength, "{}长度不能超过 {}", fieldName, maxLength);
    }

    private SettlementOrderDTO toDTO(SettlementOrder source) {
        List<SettlementOrderItemDTO> items = settlementOrderItemMapper.selectByOrderSn(
                        source.getTenantId(), source.getSn()).stream()
                .map(item -> new SettlementOrderItemDTO()
                        .setSn(item.getSn())
                        .setSourceType(item.getSourceType())
                        .setSourceSn(item.getSourceSn())
                        .setItemType(item.getItemType())
                        .setDirection(item.getDirection())
                        .setAmount(item.getAmount())
                        .setCurrency(item.getCurrency()))
                .toList();
        String reason = switch (source.getState()) {
            case FAILED -> source.getFailureReason();
            case CANCELLED -> source.getCancelReason();
            case RELEASED -> source.getReleaseReason();
            default -> source.getReturnReason();
        };
        return new SettlementOrderDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSettlementSubjectType(source.getSettlementSubjectType())
                .setSettlementSubjectId(source.getSettlementSubjectId())
                .setCurrency(source.getCurrency())
                .setSettlementPeriod(source.getSettlementPeriod())
                .setNetAmount(source.getNetAmount())
                .setState(source.getState())
                .setPolicySnapshot(new SettlementPolicySnapshotDTO()
                        .setPolicyCode(source.getRuleCode())
                        .setPolicyVersion(source.getRuleVersion())
                        .setSettlementMode(source.getSettlementMode())
                        .setSettlementDestination(source.getSettlementDestination())
                        .setTriggerMode(source.getTriggerMode())
                        .setSettlementPeriod(source.getSettlementPeriod())
                        .setTimezone(source.getTimezone())
                        .setCutoff(source.getCutoff())
                        .setPolicyApprovalRef(source.getPolicyApprovalRef()))
                .setItems(items)
                .setSettlementApprovalRef(source.getSettlementApprovalRef())
                .setLockFundsTransactionSn(source.getLockFundsTransactionSn())
                .setReleaseFundsTransactionSn(source.getReleaseFundsTransactionSn())
                .setReleaseFreezeOrderSn(source.getReleaseFreezeOrderSn())
                .setReleaseDisposition(source.getReleaseDisposition())
                .setReleaseDigest(source.getReleaseDigest())
                .setReleaseGateEvidenceRef(source.getReleaseGateEvidenceRef())
                .setReleaseCurrentLineageBatchSn(source.getReleaseCurrentLineageBatchSn())
                .setReleaseSourceClosureDigest(source.getReleaseSourceClosureDigest())
                .setReleaseAuthorityDecisionDigest(source.getReleaseAuthorityDecisionDigest())
                .setReleaseAuthorityEvidenceRefs(parseEvidenceRefs(source.getReleaseAuthorityEvidenceRefs()))
                .setReleaseApprovalRef(source.getReleaseApprovalRef())
                .setLockGateEvidenceRef(source.getLockGateEvidenceRef())
                .setAmountDigest(source.getAmountDigest())
                .setSourceDigest(source.getSourceDigest())
                .setPolicySnapshotDigest(source.getPolicySnapshotDigest())
                .setOrderDigest(source.getOrderDigest())
                .setCreatedTime(source.getGmtCreate())
                .setSubmittedTime(source.getSubmittedTime())
                .setApprovedTime(source.getApprovedTime())
                .setLockedTime(source.getLockedTime())
                .setReleasedTime(source.getReleasedTime())
                .setReturnedTime(source.getReturnedTime())
                .setCancelledTime(source.getCancelledTime())
                .setFailedTime(source.getFailedTime())
                .setReason(reason);
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? WindJson.parseArray(value, String.class) : List.of();
    }
}
