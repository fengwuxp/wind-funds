package com.wind.funds.reconciliation.application.payout.impl;

import com.wind.jackson.WindJson;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.application.payout.PayoutOrderApplicationService;
import com.wind.funds.reconciliation.dal.entities.PayoutOrder;
import com.wind.funds.reconciliation.dal.entities.PayoutReceipt;
import com.wind.funds.reconciliation.dal.entities.SettlementOrder;
import com.wind.funds.reconciliation.dal.mapper.PayoutOrderMapper;
import com.wind.funds.reconciliation.dal.mapper.PayoutReceiptMapper;
import com.wind.funds.reconciliation.dal.mapper.SettlementOrderMapper;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutNextAction;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.dto.PayoutSubmissionAdmissionDecisionDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreatePayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.HandlePayoutReceiptRequest;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.funds.reconciliation.service.PayoutSubmissionAuthority;
import com.wind.funds.transaction.application.FundsPayoutTransactionService;
import com.wind.funds.transaction.model.request.FundsPayoutRequest;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 出款事实应用服务实现。
 */
@Service
@AllArgsConstructor
public class PayoutOrderApplicationServiceImpl implements PayoutOrderApplicationService {

    private static final WindSequenceType ORDER_SEQUENCE_TYPE =
            WindSequenceType.immutable("PAYOUT_ORDER", "PYO", 6);

    private static final WindSequenceType RECEIPT_SEQUENCE_TYPE =
            WindSequenceType.immutable("PAYOUT_RECEIPT", "PYR", 6);

    private static final int MAX_FAILURE_CODE_LENGTH = 64;

    private static final int MAX_FAILURE_REASON_LENGTH = 512;

    private final PayoutOrderMapper payoutOrderMapper;

    private final PayoutReceiptMapper payoutReceiptMapper;

    private final SettlementOrderMapper settlementOrderMapper;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    private final ObjectProvider<PayoutSubmissionAuthority> payoutSubmissionAuthorityProvider;

    private final FundsPayoutTransactionService fundsPayoutTransactionService;

    private final FundsAccountQueryService fundsAccountQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayoutOrderDTO createOrder(CreatePayoutOrderRequest request, WindOperator operator) {
        validateCreateRequest(request, operator);
        SettlementOrder settlement = requiredSettlementForUpdate(request.getTenantId(), request.getSettlementOrderSn());
        PayoutOrder existing = payoutOrderMapper.selectBySettlementOrderSn(
                request.getTenantId(), request.getSettlementOrderSn());
        if (existing != null) {
            return toDTO(existing);
        }
        assertSettlementReady(settlement);
        PayoutOrder order = new PayoutOrder();
        order.setSn(TemporalSequenceFactory.hourNext(ORDER_SEQUENCE_TYPE));
        order.setTenantId(settlement.getTenantId());
        order.setSettlementOrderSn(settlement.getSn());
        order.setSettlementSubjectType(settlement.getSettlementSubjectType());
        order.setSettlementSubjectId(settlement.getSettlementSubjectId());
        order.setAmount(settlement.getNetAmount());
        order.setCurrency(settlement.getCurrency());
        order.setState(PayoutOrderState.CREATED);
        order.setCreatedBy(operator.getOperatorAsText());
        order.setVersion(0);
        payoutOrderMapper.insertSelective(order);
        AssertUtils.notNull(order.getId(), "创建出款单失败");
        return toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayoutOrderDTO submitOrder(SubmitPayoutOrderRequest request, WindOperator operator) {
        validateSubmitRequest(request, operator);
        PayoutOrder snapshot = requiredOrder(request.getTenantId(), request.getPayoutOrderSn());
        SettlementOrder settlement = requiredSettlementForUpdate(request.getTenantId(), snapshot.getSettlementOrderSn());
        PayoutOrder order = requiredOrderForUpdate(request.getTenantId(), request.getPayoutOrderSn());
        assertOrderMatchesSettlement(order, settlement);
        String submitDigest = submitDigest(request);
        if (order.getState() != PayoutOrderState.CREATED) {
            AssertUtils.equals(order.getSubmitDigest(), submitDigest, "出款单已使用不同提交参数完成提交");
            return toDTO(order);
        }
        assertSettlementReady(settlement);
        assertWithdrawAllowed(order);
        ReconciliationGateDecisionDTO gateDecision = reconciliationGateApplicationService.checkGate(
                new CheckReconciliationGateRequest()
                        .setTenantId(order.getTenantId())
                        .setGateObjectType(ReconciliationGateObjectType.PAYOUT)
                        .setGateObjectSn(order.getSn())
                        .setReconciliationRunResultSn(request.getReconciliationRunResultSn()), operator);
        AssertUtils.isTrue(gateDecision.isPassed(), "出款对账 Gate 未通过：{}", gateDecision.getExplanation());

        PayoutSubmissionAuthority authority = payoutSubmissionAuthorityProvider.getIfUnique();
        AssertUtils.notNull(authority, "宿主权威出款准入服务未配置或配置不唯一");
        PayoutSubmissionAdmissionDecisionDTO admission = authority.authorize(toDTO(order), request, operator);
        validateAdmissionDecision(admission);

        order.setState(PayoutOrderState.SUBMITTED);
        order.setPayoutAccountRef(request.getPayoutAccountRef());
        order.setPayeeEndpointRef(request.getPayeeEndpointRef());
        order.setChannelRef(request.getChannelRef());
        order.setApprovalRef(request.getApprovalRef());
        order.setExternalRuleEvidenceDigest(FundsStableHashSupport.sha256Json(
                externalRuleEvidenceValues(request.getExternalRuleVerificationEvidence())));
        order.setReconciliationRunResultSn(gateDecision.getReconciliationRunResultSn());
        order.setReconciliationResultDigest(gateDecision.getReconciliationResultDigest());
        order.setAdmissionDecisionDigest(admission.getDecisionDigest());
        order.setAdmissionEvidenceRefs(WindJson.toJsonString(admission.getEvidenceRefs()));
        order.setSubmitDigest(submitDigest);
        order.setSubmittedBy(operator.getOperatorAsText());
        order.setSubmittedTime(LocalDateTime.now());
        update(order, "提交出款单失败");
        return toDTO(order);
    }

    private void assertWithdrawAllowed(PayoutOrder order) {
        FundsAccountId accountId = FundsAccountId.immutable(
                order.getSettlementSubjectId(), order.getSettlementSubjectType());
        FundsAccount account = fundsAccountQueryService.getAccount(accountId);
        AssertUtils.isTrue(account.canWithdraw(),
                "结算资金账户不具备 WITHDRAW 能力，accountId = {}, accountType = {}, capabilitySource = {}",
                accountId.id(), accountId.type(), account.getCapabilitySource());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public PayoutOrderDTO handleReceipt(HandlePayoutReceiptRequest request, WindOperator operator) {
        validateReceiptRequest(request, operator);
        PayoutOrder order = requiredOrderForUpdate(request.getTenantId(), request.getPayoutOrderSn());
        AssertUtils.isTrue(order.getState() != PayoutOrderState.CREATED
                        && order.getState() != PayoutOrderState.CANCELLED,
                "未提交或已取消出款单不能接收外部回单");
        String normalizedDigest = normalizedReceiptDigest(request);
        PayoutReceipt existing = payoutReceiptMapper.selectBySource(
                request.getTenantId(), request.getChannelRef(), request.getExternalReceiptRef());
        PayoutOrderDTO existingResult = handleExistingReceipt(order, existing, normalizedDigest);
        if (existingResult != null) {
            return existingResult;
        }

        try {
            payoutReceiptMapper.insertSelective(newReceipt(request, normalizedDigest, operator));
        } catch (DuplicateKeyException exception) {
            existing = payoutReceiptMapper.selectBySourceForUpdate(
                    request.getTenantId(), request.getChannelRef(), request.getExternalReceiptRef());
            AssertUtils.notNull(existing, "外部回单唯一键冲突但未找到已有回单");
            return handleExistingReceipt(order, existing, normalizedDigest);
        }
        order.setLastReceiptDigest(normalizedDigest);
        if (!Objects.equals(order.getChannelRef(), request.getChannelRef())) {
            return mismatch(order, "回单通道与提交通道不一致");
        }
        if (order.getExternalReference() != null
                && !Objects.equals(order.getExternalReference(), request.getExternalReference())) {
            return mismatch(order, "回单外部出款 reference 不一致");
        }
        if (!Objects.equals(order.getAmount(), request.getAmount())
                || order.getCurrency() != request.getCurrency()) {
            return mismatch(order, "回单金额或币种与出款单不一致");
        }
        if (!claimExternalReference(order, request.getExternalReference())) {
            return mismatch(order, "外部出款 reference 已被其他出款单占用");
        }

        if (order.getState() == PayoutOrderState.MISMATCHED) {
            update(order, "更新出款回单证据失败");
            return toDTO(order);
        }
        if (isTerminal(order.getState())) {
            if (order.getState() != request.getState()) {
                return mismatch(order, "出款终态回单冲突");
            }
            update(order, "更新出款终态回单证据失败");
            return toDTO(order);
        }

        switch (request.getState()) {
            case ACCEPTED, PROCESSING -> advanceNonTerminal(order, request.getState());
            case SUCCEEDED -> applySuccess(order, operator);
            case FAILED -> applyFailure(order, request, operator);
            case RETURNED -> completeWithoutFunds(order, PayoutOrderState.RETURNED, request);
            case MISMATCHED -> markMismatched(order, "宿主归一回单标记为不一致");
            case CREATED, SUBMITTED, CANCELLED ->
                    throw new IllegalArgumentException("外部回单状态不支持：" + request.getState());
        }
        update(order, "更新出款回单结果失败");
        return toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutOrderDTO getOrder(Long tenantId, String payoutOrderSn) {
        validateQuery(tenantId, payoutOrderSn);
        return toDTO(requiredOrder(tenantId, payoutOrderSn));
    }

    private void applySuccess(PayoutOrder order, WindOperator operator) {
        try {
            order.setCompletionFundsTransactionSn(fundsPayoutTransactionService.succeed(
                    fundsRequest(order, "payout succeeded"), operator));
            order.setState(PayoutOrderState.SUCCEEDED);
            order.setCompletedTime(LocalDateTime.now());
        } catch (LedgerPostingRejectedException exception) {
            markMismatched(order, "出款成功回单对应账务入账被拒绝：" + exception.getMessage());
        }
    }

    private void applyFailure(PayoutOrder order,
                              HandlePayoutReceiptRequest request,
                              WindOperator operator) {
        try {
            order.setRollbackFundsTransactionSn(fundsPayoutTransactionService.fail(
                    fundsRequest(order, "payout failed return"), operator));
            completeWithoutFunds(order, PayoutOrderState.FAILED, request);
        } catch (LedgerPostingRejectedException exception) {
            markMismatched(order, "出款失败回退对应账务入账被拒绝：" + exception.getMessage());
        }
    }

    private FundsPayoutRequest fundsRequest(PayoutOrder order, String description) {
        return new FundsPayoutRequest()
                .setAccountId(FundsAccountId.immutable(
                        order.getSettlementSubjectId(), order.getSettlementSubjectType()))
                .setAmount(Money.immutable(order.getAmount(), order.getCurrency()))
                .setPayoutOrderSn(order.getSn())
                .setDescription(description);
    }

    private PayoutOrderDTO handleExistingReceipt(PayoutOrder order,
                                                  PayoutReceipt existing,
                                                  String normalizedDigest) {
        if (existing == null) {
            return null;
        }
        if (Objects.equals(existing.getPayoutOrderSn(), order.getSn())
                && Objects.equals(existing.getNormalizedReceiptDigest(), normalizedDigest)) {
            return toDTO(order);
        }
        order.setLastReceiptDigest(normalizedDigest);
        return mismatch(order, "同一外部回单引用对应不同出款事实");
    }

    private boolean claimExternalReference(PayoutOrder order, String externalReference) {
        if (order.getExternalReference() != null) {
            return true;
        }
        try {
            AssertUtils.isTrue(payoutOrderMapper.claimExternalReference(order.getId(), externalReference) == 1,
                    "认领外部出款 reference 失败");
            order.setExternalReference(externalReference);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private void completeWithoutFunds(PayoutOrder order,
                                      PayoutOrderState state,
                                      HandlePayoutReceiptRequest request) {
        order.setState(state);
        order.setFailureCode(request.getFailureCode());
        order.setFailureReason(request.getFailureReason());
        order.setCompletedTime(LocalDateTime.now());
    }

    private void advanceNonTerminal(PayoutOrder order, PayoutOrderState state) {
        if (state == PayoutOrderState.PROCESSING || order.getState() == PayoutOrderState.SUBMITTED) {
            order.setState(state);
        }
    }

    private PayoutOrderDTO mismatch(PayoutOrder order, String reason) {
        markMismatched(order, reason);
        update(order, "标记出款回单不一致失败");
        return toDTO(order);
    }

    private void markMismatched(PayoutOrder order, String reason) {
        order.setState(PayoutOrderState.MISMATCHED);
        order.setFailureReason(reason);
        order.setCompletedTime(LocalDateTime.now());
    }

    private boolean isTerminal(PayoutOrderState state) {
        return state == PayoutOrderState.SUCCEEDED
                || state == PayoutOrderState.FAILED
                || state == PayoutOrderState.RETURNED;
    }

    private PayoutReceipt newReceipt(HandlePayoutReceiptRequest request,
                                     String normalizedDigest,
                                     WindOperator operator) {
        PayoutReceipt result = new PayoutReceipt();
        result.setSn(TemporalSequenceFactory.hourNext(RECEIPT_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setPayoutOrderSn(request.getPayoutOrderSn());
        result.setChannelRef(request.getChannelRef());
        result.setExternalReceiptRef(request.getExternalReceiptRef());
        result.setExternalReference(request.getExternalReference());
        result.setState(request.getState());
        result.setAmount(request.getAmount());
        result.setCurrency(request.getCurrency());
        result.setSourceReceiptDigest(request.getSourceReceiptDigest());
        result.setNormalizedReceiptDigest(normalizedDigest);
        result.setEvidenceRef(request.getEvidenceRef());
        result.setExternalOccurredAt(request.getExternalOccurredAt());
        result.setReceivedBy(operator.getOperatorAsText());
        return result;
    }

    private String normalizedReceiptDigest(HandlePayoutReceiptRequest request) {
        return FundsStableHashSupport.sha256Json(Map.of(
                "tenantId", request.getTenantId(),
                "payoutOrderSn", request.getPayoutOrderSn(),
                "channelRef", request.getChannelRef(),
                "externalReceiptRef", request.getExternalReceiptRef(),
                "externalReference", request.getExternalReference(),
                "status", request.getState().name(),
                "amount", request.getAmount(),
                "currency", request.getCurrency().name(),
                "sourceReceiptDigest", request.getSourceReceiptDigest()));
    }

    private String submitDigest(SubmitPayoutOrderRequest request) {
        return FundsStableHashSupport.sha256Json(Map.of(
                "tenantId", request.getTenantId(),
                "payoutOrderSn", request.getPayoutOrderSn(),
                "payoutAccountRef", request.getPayoutAccountRef(),
                "payeeEndpointRef", request.getPayeeEndpointRef(),
                "channelRef", request.getChannelRef(),
                "approvalRef", request.getApprovalRef(),
                "externalRuleEvidence", externalRuleEvidenceValues(request.getExternalRuleVerificationEvidence()),
                "reconciliationRunResultSn", request.getReconciliationRunResultSn()));
    }

    private Map<String, Object> externalRuleEvidenceValues(ExternalRuleVerificationEvidenceDTO evidence) {
        return Map.of(
                "evidenceRef", evidence.getEvidenceRef(),
                "ruleSource", evidence.getRuleSource(),
                "versionOrPublishedAt", evidence.getVersionOrPublishedAt(),
                "effectiveDate", evidence.getEffectiveDate(),
                "applicableScope", evidence.getApplicableScope(),
                "jurisdiction", evidence.getJurisdiction(),
                "verifiedAt", evidence.getVerifiedAt(),
                "confirmedBy", evidence.getConfirmedBy(),
                "status", evidence.getStatus().name());
    }

    private void validateCreateRequest(CreatePayoutOrderRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建出款单请求不能为空");
        AssertUtils.notNull(operator, "创建出款单操作人不能为空");
        validateQuery(request.getTenantId(), request.getSettlementOrderSn());
    }

    private void validateSubmitRequest(SubmitPayoutOrderRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "提交出款单请求不能为空");
        AssertUtils.notNull(operator, "提交出款单操作人不能为空");
        validateQuery(request.getTenantId(), request.getPayoutOrderSn());
        AssertUtils.hasText(request.getPayoutAccountRef(), "出款账户引用不能为空");
        AssertUtils.hasText(request.getPayeeEndpointRef(), "收款端点引用不能为空");
        AssertUtils.hasText(request.getChannelRef(), "出款通道引用不能为空");
        AssertUtils.hasText(request.getApprovalRef(), "出款审批引用不能为空");
        AssertUtils.hasText(request.getReconciliationRunResultSn(), "出款对账运行结果流水号不能为空");
        validateExternalRuleEvidence(request.getExternalRuleVerificationEvidence());
    }

    private void validateExternalRuleEvidence(ExternalRuleVerificationEvidenceDTO evidence) {
        AssertUtils.notNull(evidence, "外部规则核验证据不能为空");
        AssertUtils.hasText(evidence.getEvidenceRef(), "外部规则核验证据引用不能为空");
        AssertUtils.hasText(evidence.getRuleSource(), "外部规则来源不能为空");
        AssertUtils.hasText(evidence.getVersionOrPublishedAt(), "外部规则版本不能为空");
        AssertUtils.notNull(evidence.getEffectiveDate(), "外部规则生效日期不能为空");
        AssertUtils.hasText(evidence.getApplicableScope(), "外部规则适用范围不能为空");
        AssertUtils.hasText(evidence.getJurisdiction(), "外部规则适用法域不能为空");
        AssertUtils.notNull(evidence.getVerifiedAt(), "外部规则核验日期不能为空");
        AssertUtils.hasText(evidence.getConfirmedBy(), "外部规则确认方不能为空");
        AssertUtils.equals(ExternalRuleVerificationStatus.VERIFIED, evidence.getStatus(),
                "外部规则必须完成核验");
    }

    private void validateAdmissionDecision(PayoutSubmissionAdmissionDecisionDTO decision) {
        AssertUtils.notNull(decision, "宿主权威出款准入结果不能为空");
        AssertUtils.isTrue(decision.isPassed(), "宿主权威出款准入未通过：{}", decision.getBlockingReason());
        AssertUtils.isTrue(isSha256(decision.getDecisionDigest()), "宿主权威出款准入决策摘要必须是 SHA-256");
        AssertUtils.notEmpty(decision.getEvidenceRefs(), "宿主权威出款准入证据引用不能为空");
        AssertUtils.isTrue(decision.getEvidenceRefs().stream().allMatch(StringUtils::hasText),
                "宿主权威出款准入证据引用不能为空");
        AssertUtils.notNull(decision.getExpiresAt(), "宿主权威出款准入有效期不能为空");
        AssertUtils.isTrue(decision.getExpiresAt().isAfter(LocalDateTime.now()),
                "宿主权威出款准入结果已过期");
    }

    private void validateReceiptRequest(HandlePayoutReceiptRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "出款回单请求不能为空");
        AssertUtils.notNull(operator, "出款回单操作人不能为空");
        validateQuery(request.getTenantId(), request.getPayoutOrderSn());
        AssertUtils.hasText(request.getChannelRef(), "出款回单通道引用不能为空");
        AssertUtils.hasText(request.getExternalReceiptRef(), "外部回单唯一引用不能为空");
        AssertUtils.hasText(request.getExternalReference(), "外部出款 reference 不能为空");
        AssertUtils.notNull(request.getState(), "出款回单状态不能为空");
        AssertUtils.notNull(request.getAmount(), "出款回单金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0, "出款回单金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "出款回单币种不能为空");
        AssertUtils.isTrue(isSha256(request.getSourceReceiptDigest()), "来源回单摘要必须是 SHA-256");
        AssertUtils.hasText(request.getEvidenceRef(), "出款回单证据引用不能为空");
        AssertUtils.notNull(request.getExternalOccurredAt(), "出款回单外部发生时间不能为空");
        if (StringUtils.hasText(request.getFailureCode())) {
            AssertUtils.isTrue(request.getFailureCode().length() <= MAX_FAILURE_CODE_LENGTH,
                    "出款失败码长度不能超过 {}", MAX_FAILURE_CODE_LENGTH);
        }
        if (StringUtils.hasText(request.getFailureReason())) {
            AssertUtils.isTrue(request.getFailureReason().length() <= MAX_FAILURE_REASON_LENGTH,
                    "出款失败原因长度不能超过 {}", MAX_FAILURE_REASON_LENGTH);
        }
    }

    private boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    private void assertSettlementReady(SettlementOrder settlement) {
        AssertUtils.isTrue(settlement.getState() == SettlementOrderState.LOCKED,
                "只有 LOCKED 结算单可以创建或提交出款，status = {}", settlement.getState());
        AssertUtils.isTrue(settlement.getSettlementDestination() == SettlementDestination.EXTERNAL_ENDPOINT,
                "只有外部收款端点结算单可以创建出款");
        AssertUtils.hasText(settlement.getLockFundsTransactionSn(), "结算单缺少资金锁定事实");
        AssertUtils.isTrue(settlement.getNetAmount() > 0, "结算单净额必须大于 0");
    }

    private void assertOrderMatchesSettlement(PayoutOrder order, SettlementOrder settlement) {
        AssertUtils.equals(order.getSettlementOrderSn(), settlement.getSn(), "出款单与结算单引用不一致");
        AssertUtils.equals(order.getSettlementSubjectType(), settlement.getSettlementSubjectType(),
                "出款单与结算主体类型不一致");
        AssertUtils.equals(order.getSettlementSubjectId(), settlement.getSettlementSubjectId(),
                "出款单与结算主体不一致");
        AssertUtils.equals(order.getAmount(), settlement.getNetAmount(), "出款单与结算净额不一致");
        AssertUtils.equals(order.getCurrency(), settlement.getCurrency(), "出款单与结算币种不一致");
    }

    private void validateQuery(Long tenantId, String businessSn) {
        AssertUtils.notNull(tenantId, "出款租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), tenantId, "出款 tenantId 与当前租户不一致");
        AssertUtils.hasText(businessSn, "出款业务流水号不能为空");
    }

    private SettlementOrder requiredSettlementForUpdate(Long tenantId, String settlementOrderSn) {
        SettlementOrder result = settlementOrderMapper.selectBySnForUpdate(tenantId, settlementOrderSn);
        AssertUtils.notNull(result, "结算单不存在，settlementOrderSn = {}", settlementOrderSn);
        return result;
    }

    private PayoutOrder requiredOrder(Long tenantId, String payoutOrderSn) {
        PayoutOrder result = payoutOrderMapper.selectBySn(tenantId, payoutOrderSn);
        AssertUtils.notNull(result, "出款单不存在，payoutOrderSn = {}", payoutOrderSn);
        return result;
    }

    private PayoutOrder requiredOrderForUpdate(Long tenantId, String payoutOrderSn) {
        PayoutOrder result = payoutOrderMapper.selectBySnForUpdate(tenantId, payoutOrderSn);
        AssertUtils.notNull(result, "出款单不存在，payoutOrderSn = {}", payoutOrderSn);
        return result;
    }

    private void update(PayoutOrder order, String message) {
        AssertUtils.isTrue(payoutOrderMapper.update(order) == 1, message);
    }

    private PayoutOrderDTO toDTO(PayoutOrder source) {
        return new PayoutOrderDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setSettlementOrderSn(source.getSettlementOrderSn())
                .setSettlementSubjectType(source.getSettlementSubjectType())
                .setSettlementSubjectId(source.getSettlementSubjectId())
                .setAmount(source.getAmount())
                .setCurrency(source.getCurrency())
                .setState(source.getState())
                .setDisplayStatus(displayStatus(source.getState()))
                .setNextAction(nextAction(source.getState()))
                .setPayoutAccountRef(source.getPayoutAccountRef())
                .setPayeeEndpointRef(source.getPayeeEndpointRef())
                .setChannelRef(source.getChannelRef())
                .setExternalReference(source.getExternalReference())
                .setReconciliationRunResultSn(source.getReconciliationRunResultSn())
                .setReconciliationResultDigest(source.getReconciliationResultDigest())
                .setAdmissionDecisionDigest(source.getAdmissionDecisionDigest())
                .setAdmissionEvidenceRefs(parseEvidenceRefs(source.getAdmissionEvidenceRefs()))
                .setCompletionFundsTransactionSn(source.getCompletionFundsTransactionSn())
                .setRollbackFundsTransactionSn(source.getRollbackFundsTransactionSn())
                .setLastReceiptDigest(source.getLastReceiptDigest())
                .setFailureCode(source.getFailureCode())
                .setFailureReason(source.getFailureReason())
                .setSubmittedTime(source.getSubmittedTime())
                .setCompletedTime(source.getCompletedTime())
                .setCancelledTime(source.getCancelledTime())
                .setCancelReason(source.getCancelReason());
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? WindJson.parseArray(value, String.class) : List.of();
    }

    private PayoutDisplayStatus displayStatus(PayoutOrderState state) {
        return switch (state) {
            case CREATED -> PayoutDisplayStatus.PENDING;
            case SUBMITTED, ACCEPTED, PROCESSING -> PayoutDisplayStatus.PROCESSING;
            case SUCCEEDED -> PayoutDisplayStatus.SUCCEEDED;
            case FAILED -> PayoutDisplayStatus.FAILED;
            case RETURNED -> PayoutDisplayStatus.RETURNED;
            case MISMATCHED -> PayoutDisplayStatus.REVIEW_REQUIRED;
            case CANCELLED -> PayoutDisplayStatus.CANCELLED;
        };
    }

    private PayoutNextAction nextAction(PayoutOrderState state) {
        return switch (state) {
            case CREATED -> PayoutNextAction.SUBMIT_ALLOWED;
            case SUBMITTED, ACCEPTED, PROCESSING -> PayoutNextAction.WAITING_EXTERNAL_RESULT;
            case SUCCEEDED, FAILED -> PayoutNextAction.NO_ACTION_REQUIRED;
            case RETURNED, MISMATCHED -> PayoutNextAction.REVIEW_REQUIRED;
            case CANCELLED -> PayoutNextAction.NO_ACTION_REQUIRED;
        };
    }
}
