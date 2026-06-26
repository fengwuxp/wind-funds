package com.wind.funds.wallet.application.spend.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支出控制准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Service
@AllArgsConstructor
public class SpendControlAdmissionApplicationServiceImpl implements SpendControlAdmissionApplicationService {

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final SpendRuleDecisionRecordService spendRuleDecisionRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendControlAdmissionDecisionDTO resolveSpendControlAdmission(
            @NonNull ResolveSpendControlAdmissionRequest request) {
        validateRequest(request);
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                preTransactionSnapshotApplicationService.resolvePreTransactionSnapshot(toSnapshotRequest(request));
        SpendRuleDecisionRecordDTO decisionRecord =
                spendRuleDecisionRecordService.recordDecision(toDecisionRecordRequest(request));
        return toDecision(request, snapshot, decisionRecord);
    }

    private void validateRequest(ResolveSpendControlAdmissionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.notNull(request.getBindingRole(), "支付工具绑定角色不能为空");
        AssertUtils.notNull(request.getRelationType(), "资金责任关系类型不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getSpendRuleScopeType(), "Spend Rule 控制范围类型不能为空");
        AssertUtils.hasText(request.getSpendRuleScopeId(), "Spend Rule 控制范围标识不能为空");
        AssertUtils.hasText(request.getSpendDecisionSn(), "Spend Rule 决策流水号不能为空");
        AssertUtils.notNull(request.getSpendDecisionResult(), "Spend Rule 决策结果不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getSpendDecisionDigest(), "Spend Rule 决策摘要");
        if (request.getSpendDecisionResult() == SpendControlDecisionResult.REJECTED) {
            AssertUtils.hasText(request.getRejectReason(), "Spend Rule 拒绝原因不能为空");
        }
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

    private RecordSpendRuleDecisionRecordRequest toDecisionRecordRequest(ResolveSpendControlAdmissionRequest request) {
        return new RecordSpendRuleDecisionRecordRequest()
                .setTenantId(request.getTenantId())
                .setDecisionSn(request.getSpendDecisionSn())
                .setRuleId(request.getSpendRuleId())
                .setRuleVersion(request.getSpendRuleVersion())
                .setAssignmentSn(request.getSpendRuleAssignmentSn())
                .setScopeType(request.getSpendRuleScopeType())
                .setScopeId(request.getSpendRuleScopeId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setDecisionResult(request.getSpendDecisionResult())
                .setRejectReason(request.getRejectReason())
                .setDecisionDigest(request.getSpendDecisionDigest());
    }

    private SpendControlAdmissionDecisionDTO toDecision(ResolveSpendControlAdmissionRequest request,
                                                        PaymentInstrumentPreTransactionSnapshotDTO snapshot,
                                                        SpendRuleDecisionRecordDTO decisionRecord) {
        return new SpendControlAdmissionDecisionDTO()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(request.getBindingRole())
                .setRelationType(request.getRelationType())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setAdmitted(request.getSpendDecisionResult() == SpendControlDecisionResult.PASSED)
                .setTargetAccountId(snapshot.getTargetAccountId())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setSpendRuleAssignmentSn(request.getSpendRuleAssignmentSn())
                .setSpendRuleScopeType(request.getSpendRuleScopeType())
                .setSpendRuleScopeId(request.getSpendRuleScopeId())
                .setSpendDecisionSn(request.getSpendDecisionSn())
                .setSpendDecisionResult(request.getSpendDecisionResult())
                .setSpendDecisionDigest(request.getSpendDecisionDigest())
                .setSpendDecisionRecordId(decisionRecord.getId())
                .setBudgetGroupSn(request.getBudgetGroupSn())
                .setRejectReason(request.getRejectReason())
                .setPreTransactionSnapshot(snapshot);
    }
}
