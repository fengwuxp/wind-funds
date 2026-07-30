package com.wind.funds.wallet.application.spend.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.query.SpendRuleBindingQuery;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.service.SpendRuleBindingService;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 支出控制准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Service
@AllArgsConstructor
@Slf4j
public class SpendControlAdmissionApplicationServiceImpl implements SpendControlAdmissionApplicationService {

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final SpendRuleDecisionRecordService spendRuleDecisionRecordService;

    private final SpendRuleBindingService spendRuleBindingService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendControlAdmissionDecisionDTO resolveSpendControlAdmission(
            @NonNull ResolveSpendControlAdmissionRequest request) {
        validateRequest(request);
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toSnapshotRequest(request));
        List<SpendRuleBindingDTO> applicableBindings = resolveApplicableBindings(request, snapshot);
        SpendControlAdmissionDecisionDTO decision = applicableBindings.isEmpty()
                ? resolveNoApplicableRule(request, snapshot)
                : resolveReferencedDecision(request, snapshot, applicableBindings);
        logAfterCommit(() -> log.info("支出控制准入已解析，tenantId={}, businessScene={}, businessSn={}, action={}, amount={}, "
                        + "currency={}, spendRuleId={}, spendRuleVersion={}, spendDecisionSn={}, decisionResult={}, "
                        + "admitted={}, targetAccountType={}",
                request.getTenantId(), request.getBusinessScene(), request.getBusinessSn(), request.getAction(),
                request.getAmount(), request.getCurrency(), decision.getSpendRuleId(), decision.getSpendRuleVersion(),
                decision.getSpendDecisionSn(), decision.getSpendDecisionResult(), decision.getAdmitted(),
                snapshot.getTargetAccountId() == null ? null : snapshot.getTargetAccountId().type()));
        return decision;
    }

    private SpendControlAdmissionDecisionDTO resolveNoApplicableRule(
            ResolveSpendControlAdmissionRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        AssertUtils.isTrue(!hasDecisionEvidence(request),
                "未找到适用 Spend Rule 挂载，不能携带 decisionRef 或裸决策字段");
        return toDecision(request, snapshot, null)
                .setAdmitted(Boolean.TRUE)
                .setSpendDecisionResult(SpendControlDecisionResult.NO_APPLICABLE_RULE);
    }

    private SpendControlAdmissionDecisionDTO resolveReferencedDecision(
            ResolveSpendControlAdmissionRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            List<SpendRuleBindingDTO> applicableBindings) {
        AssertUtils.isTrue(applicableBindings.size() == 1,
                "当前准入契约不支持多个适用 Spend Rule 挂载，请由上游完成多规则裁决并升级证据契约，bindingCount = {}",
                applicableBindings.size());
        SpendRuleBindingDTO binding = applicableBindings.getFirst();
        AssertUtils.hasText(request.getSpendDecisionSn(),
                "适用 Spend Rule 挂载要求 decisionRef，spendRuleBindingSn = {}",
                binding.getSn());
        SpendRuleDecisionRecordDTO decisionRecord = spendRuleDecisionRecordService.findDecisionRecord(
                request.getTenantId(), request.getSpendDecisionSn());
        AssertUtils.notNull(decisionRecord,
                "Spend Rule 决策引用不存在，decisionRef = {}",
                request.getSpendDecisionSn());
        assertDecisionMatches(request, snapshot, binding, decisionRecord);
        return toDecision(request, snapshot, decisionRecord);
    }

    private List<SpendRuleBindingDTO> resolveApplicableBindings(
            ResolveSpendControlAdmissionRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        LocalDateTime effectiveAt = LocalDateTime.now();
        List<SpendRuleBindingDTO> result = new ArrayList<>();
        result.addAll(queryEffectiveBindings(request.getTenantId(),
                SpendRuleScopeType.PAYMENT_INSTRUMENT,
                request.getInstrumentSn(),
                effectiveAt));
        FundsAccountId boundAccountId = resolveBoundAccountId(snapshot);
        result.addAll(queryAccountBindings(request.getTenantId(), boundAccountId, effectiveAt));
        if (!Objects.equals(boundAccountId, snapshot.getTargetAccountId())) {
            result.addAll(queryAccountBindings(request.getTenantId(), snapshot.getTargetAccountId(), effectiveAt));
        }
        result.addAll(queryEffectiveBindings(request.getTenantId(),
                SpendRuleScopeType.BUSINESS_SCENE,
                request.getBusinessScene(),
                effectiveAt));
        if (StringUtils.hasText(request.getControlScopeId())) {
            List<SpendRuleBindingDTO> controlScopeBindings = queryEffectiveBindings(request.getTenantId(),
                    SpendRuleScopeType.SPEND_CONTROL_SCOPE,
                    request.getControlScopeId(),
                    effectiveAt);
            result.addAll(controlScopeBindings);
            if (controlScopeBindings.isEmpty()) {
                assertNoUnresolvedBindings(request.getTenantId(), SpendRuleScopeType.SPEND_CONTROL_SCOPE, effectiveAt);
            }
        } else {
            assertNoUnresolvedBindings(request.getTenantId(), SpendRuleScopeType.SPEND_CONTROL_SCOPE, effectiveAt);
        }
        assertNoUnresolvedBindings(request.getTenantId(), SpendRuleScopeType.ACCOUNT_HIERARCHY, effectiveAt);
        return List.copyOf(result);
    }

    private FundsAccountId resolveBoundAccountId(PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        AssertUtils.notNull(snapshot.getPaymentInstrumentCapability(), "支付工具能力快照不能为空");
        AssertUtils.notNull(snapshot.getPaymentInstrumentCapability().getSubjectType(), "支付工具绑定主体类型不能为空");
        AssertUtils.hasText(snapshot.getPaymentInstrumentCapability().getSubjectId(), "支付工具绑定主体 ID 不能为空");
        return FundsAccountId.immutable(
                snapshot.getPaymentInstrumentCapability().getSubjectId(),
                snapshot.getPaymentInstrumentCapability().getSubjectType());
    }

    private List<SpendRuleBindingDTO> queryAccountBindings(Long tenantId,
                                                           @Nullable FundsAccountId accountId,
                                                           LocalDateTime effectiveAt) {
        SpendRuleScopeType scopeType = accountScopeType(accountId);
        if (scopeType == null) {
            return List.of();
        }
        return queryEffectiveBindings(tenantId, scopeType, accountId.id(), effectiveAt);
    }

    private void assertNoUnresolvedBindings(Long tenantId,
                                            SpendRuleScopeType scopeType,
                                            LocalDateTime effectiveAt) {
        List<SpendRuleBindingDTO> bindings = spendRuleBindingService.querySpendRuleBindings(
                new SpendRuleBindingQuery()
                        .setTenantId(tenantId)
                        .setScopeType(scopeType)
                        .setEffectiveOnly(Boolean.TRUE)
                        .setEffectiveAt(effectiveAt));
        AssertUtils.isTrue(bindings.isEmpty(),
                "{} 挂载无法从可信上下文解析，当前准入必须 fail-closed，bindingCount = {}",
                scopeType,
                bindings.size());
    }

    private List<SpendRuleBindingDTO> queryEffectiveBindings(Long tenantId,
                                                             SpendRuleScopeType scopeType,
                                                             String scopeId,
                                                             LocalDateTime effectiveAt) {
        return spendRuleBindingService.querySpendRuleBindings(new SpendRuleBindingQuery()
                .setTenantId(tenantId)
                .setScopeType(scopeType)
                .setScopeId(scopeId)
                .setEffectiveOnly(Boolean.TRUE)
                .setEffectiveAt(effectiveAt));
    }

    private @Nullable SpendRuleScopeType accountScopeType(@Nullable FundsAccountId accountId) {
        if (accountId == null) {
            return null;
        }
        if (FundsSubjectType.FUNDING_ACCOUNT.name().equals(accountId.type())) {
            return SpendRuleScopeType.FUNDING_ACCOUNT;
        }
        if (FundsSubjectType.CREDIT_ACCOUNT.name().equals(accountId.type())) {
            return SpendRuleScopeType.CREDIT_ACCOUNT;
        }
        return null;
    }

    private void assertDecisionMatches(ResolveSpendControlAdmissionRequest request,
                                       PaymentInstrumentPreTransactionSnapshotDTO snapshot,
                                       SpendRuleBindingDTO binding,
                                       SpendRuleDecisionRecordDTO decisionRecord) {
        AssertUtils.isTrue(Objects.equals(decisionRecord.getSpendRuleBindingSn(), binding.getSn())
                        && Objects.equals(decisionRecord.getRuleId(), binding.getRuleId())
                        && Objects.equals(decisionRecord.getRuleVersion(), binding.getRuleVersion())
                        && decisionRecord.getScopeType() == binding.getScopeType()
                        && Objects.equals(decisionRecord.getScopeId(), binding.getScopeId()),
                "Spend Rule 决策引用与当前适用挂载不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(Objects.equals(decisionRecord.getTenantId(), request.getTenantId())
                        && Objects.equals(decisionRecord.getInstrumentSn(), request.getInstrumentSn())
                        && decisionRecord.getAction() == request.getAction()
                        && Objects.equals(decisionRecord.getAmount(), request.getAmount())
                        && decisionRecord.getCurrency() == request.getCurrency()
                        && Objects.equals(decisionRecord.getBusinessScene(), request.getBusinessScene())
                        && Objects.equals(decisionRecord.getBusinessSn(), request.getBusinessSn()),
                "Spend Rule 决策引用与当前交易上下文不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.notNull(snapshot.getPaymentInstrumentCapability(), "支付工具能力快照不能为空");
        AssertUtils.isTrue(Objects.equals(decisionRecord.getInstrumentBindingVersion(),
                        snapshot.getPaymentInstrumentCapability().getBindingVersion()),
                "Spend Rule 决策引用与当前支付工具绑定版本不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(Objects.equals(decisionRecord.getControlScopeId(), request.getControlScopeId())
                        && Objects.equals(decisionRecord.getPeriodId(), request.getPeriodId()),
                "Spend Rule 决策引用与当前控制窗口不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(decisionRecord.getTargetAccountId() == null
                        || Objects.equals(decisionRecord.getTargetAccountId(), snapshot.getTargetAccountId()),
                "Spend Rule 决策引用与当前目标账户不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(decisionRecord.getDecisionResult() == SpendControlDecisionResult.PASSED
                        || decisionRecord.getDecisionResult() == SpendControlDecisionResult.REJECTED,
                "Spend Rule 决策引用结果不可用于准入，decisionRef = {}, decisionResult = {}",
                request.getSpendDecisionSn(),
                decisionRecord.getDecisionResult());
        assertOptionalEchoMatches(request, decisionRecord);
    }

    private void assertOptionalEchoMatches(ResolveSpendControlAdmissionRequest request,
                                           SpendRuleDecisionRecordDTO decisionRecord) {
        AssertUtils.isTrue(!StringUtils.hasText(request.getSpendRuleId())
                        || Objects.equals(request.getSpendRuleId(), decisionRecord.getRuleId()),
                "Spend Rule 标识回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(!StringUtils.hasText(request.getSpendRuleVersion())
                        || Objects.equals(request.getSpendRuleVersion(), decisionRecord.getRuleVersion()),
                "Spend Rule 版本回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(!StringUtils.hasText(request.getSpendRuleBindingSn())
                        || Objects.equals(request.getSpendRuleBindingSn(), decisionRecord.getSpendRuleBindingSn()),
                "Spend Rule 挂载回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(request.getSpendRuleScopeType() == null
                        || request.getSpendRuleScopeType() == decisionRecord.getScopeType(),
                "Spend Rule 范围类型回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(!StringUtils.hasText(request.getSpendRuleScopeId())
                        || Objects.equals(request.getSpendRuleScopeId(), decisionRecord.getScopeId()),
                "Spend Rule 范围标识回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(request.getSpendDecisionResult() == null
                        || request.getSpendDecisionResult() == decisionRecord.getDecisionResult(),
                "Spend Rule 结果回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(!StringUtils.hasText(request.getSpendDecisionDigest())
                        || Objects.equals(request.getSpendDecisionDigest(), decisionRecord.getDecisionDigest()),
                "Spend Rule 摘要回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
        AssertUtils.isTrue(!StringUtils.hasText(request.getRejectReason())
                        || Objects.equals(request.getRejectReason(), decisionRecord.getRejectReason()),
                "Spend Rule 拒绝原因回显与决策引用不一致，decisionRef = {}",
                request.getSpendDecisionSn());
    }

    private boolean hasDecisionEvidence(ResolveSpendControlAdmissionRequest request) {
        return StringUtils.hasText(request.getSpendRuleId())
                || StringUtils.hasText(request.getSpendRuleVersion())
                || StringUtils.hasText(request.getSpendRuleBindingSn())
                || request.getSpendRuleScopeType() != null
                || StringUtils.hasText(request.getSpendRuleScopeId())
                || StringUtils.hasText(request.getSpendDecisionSn())
                || request.getSpendDecisionResult() != null
                || StringUtils.hasText(request.getSpendDecisionDigest())
                || StringUtils.hasText(request.getRejectReason());
    }

    private void validateRequest(ResolveSpendControlAdmissionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "支出控制准入 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.notNull(request.getBindingRole(), "支付工具绑定角色不能为空");
        AssertUtils.notNull(request.getRelationType(), "资金责任关系类型不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
    }

    private ResolvePaymentInstrumentPreTransactionSnapshotRequest toSnapshotRequest(
            ResolveSpendControlAdmissionRequest request) {
        return new ResolvePaymentInstrumentPreTransactionSnapshotRequest()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(request.getBindingRole())
                .setExpectedBindingVersion(request.getExpectedBindingVersion())
                .setRelationType(request.getRelationType())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn());
    }

    private SpendControlAdmissionDecisionDTO toDecision(
            ResolveSpendControlAdmissionRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendRuleDecisionRecordDTO decisionRecord) {
        SpendControlAdmissionDecisionDTO result = new SpendControlAdmissionDecisionDTO()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(request.getBindingRole())
                .setRelationType(request.getRelationType())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setTargetAccountId(snapshot.getTargetAccountId())
                .setControlScopeId(request.getControlScopeId())
                .setPeriodId(request.getPeriodId())
                .setPreTransactionSnapshot(snapshot);
        if (decisionRecord == null) {
            return result;
        }
        return result
                .setAdmitted(decisionRecord.getDecisionResult() == SpendControlDecisionResult.PASSED)
                .setSpendRuleId(decisionRecord.getRuleId())
                .setSpendRuleVersion(decisionRecord.getRuleVersion())
                .setSpendRuleBindingSn(decisionRecord.getSpendRuleBindingSn())
                .setSpendRuleScopeType(decisionRecord.getScopeType())
                .setSpendRuleScopeId(decisionRecord.getScopeId())
                .setSpendDecisionSn(decisionRecord.getDecisionSn())
                .setSpendDecisionResult(decisionRecord.getDecisionResult())
                .setSpendDecisionDigest(decisionRecord.getDecisionDigest())
                .setSpendDecisionRecordId(decisionRecord.getId())
                .setRejectReason(decisionRecord.getRejectReason());
    }

    private void logAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
