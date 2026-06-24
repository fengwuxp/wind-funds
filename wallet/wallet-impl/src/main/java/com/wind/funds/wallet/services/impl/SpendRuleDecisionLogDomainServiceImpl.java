package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
import com.wind.funds.wallet.service.SpendRuleDecisionLogDomainService;
import com.wind.funds.wallet.service.SpendRuleDecisionLogService;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Spend Rule 决策记录领域写服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDecisionLogDomainServiceImpl implements SpendRuleDecisionLogDomainService {

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendRuleAssignmentService spendRuleAssignmentService;

    private final SpendRuleDecisionLogService spendRuleDecisionLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleDecisionLogDTO recordDecision(
            @NonNull RecordSpendRuleDecisionLogRequest request) {
        validateDecisionLogRequest(request);
        spendRuleVersionService.getPublishedVersion(request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        assertAssignmentMatchesIfPresent(request);
        SpendRuleDecisionLogDTO existing =
                spendRuleDecisionLogService.findDecisionLog(request.getTenantId(), request.getDecisionSn());
        if (existing != null) {
            assertSameDecisionLog(request, existing);
            return existing;
        }
        try {
            Long decisionLogId = spendRuleDecisionLogService.createDecisionLog(request);
            return spendRuleDecisionLogService.getDecisionLogById(decisionLogId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDecisionLogAfterInsertConflict(request, exception);
        }
    }

    private void validateDecisionLogRequest(RecordSpendRuleDecisionLogRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getDecisionSn(), "Spend Rule 决策流水号不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getScopeType(), "Spend Rule 决策范围类型不能为空");
        AssertUtils.hasText(request.getScopeId(), "Spend Rule 决策范围标识不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
        AssertUtils.notNull(request.getDecisionResult(), "Spend Rule 决策结果不能为空");
        AssertUtils.hasText(request.getDecisionDigest(), "Spend Rule 决策摘要不能为空");
        if (request.getDecisionResult() == SpendControlDecisionResult.REJECTED) {
            AssertUtils.hasText(request.getRejectReason(), "Spend Rule 拒绝原因不能为空");
        } else {
            AssertUtils.isTrue(request.getRejectReason() == null, "非拒绝 Spend Rule 决策不能携带拒绝原因");
        }
        if (request.getScopeType() == SpendRuleScopeType.PAYMENT_INSTRUMENT) {
            AssertUtils.hasText(request.getInstrumentSn(), "支付工具范围的 Spend Rule 决策必须携带支付工具号");
            AssertUtils.isTrue(Objects.equals(request.getScopeId(), request.getInstrumentSn()),
                "Spend Rule 决策支付工具号与控制范围不一致，decisionSn = {}",
                request.getDecisionSn());
        }
    }

    private void assertAssignmentMatchesIfPresent(RecordSpendRuleDecisionLogRequest request) {
        if (request.getAssignmentSn() == null) {
            return;
        }
        SpendRuleAssignmentDTO assignment =
                spendRuleAssignmentService.getActiveAssignment(request.getTenantId(), request.getAssignmentSn());
        assertAssignmentEffectiveNow(request, assignment);
        AssertUtils.isTrue(Objects.equals(assignment.getRuleId(), request.getRuleId())
                        && Objects.equals(assignment.getRuleVersion(), request.getRuleVersion())
                        && assignment.getScopeType() == request.getScopeType()
                        && Objects.equals(assignment.getScopeId(), request.getScopeId()),
                "Spend Rule 决策记录与挂载不一致，decisionSn = {}",
                request.getDecisionSn());
    }

    private void assertAssignmentEffectiveNow(RecordSpendRuleDecisionLogRequest request,
                                              SpendRuleAssignmentDTO assignment) {
        LocalDateTime now = LocalDateTime.now();
        AssertUtils.isTrue(!now.isBefore(assignment.getEffectiveFrom()) && now.isBefore(assignment.getEffectiveTo()),
                "Spend Rule 挂载未在当前时间生效，assignmentSn = {}",
                request.getAssignmentSn());
    }

    private SpendRuleDecisionLogDTO readIdempotentDecisionLogAfterInsertConflict(
            RecordSpendRuleDecisionLogRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDecisionLogDTO existing =
                spendRuleDecisionLogService.findDecisionLog(request.getTenantId(), request.getDecisionSn());
        if (existing == null) {
            throw exception;
        }
        assertSameDecisionLog(request, existing);
        return existing;
    }

    private void assertSameDecisionLog(RecordSpendRuleDecisionLogRequest request,
                                       SpendRuleDecisionLogDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && Objects.equals(existing.getAssignmentSn(), request.getAssignmentSn())
                        && existing.getScopeType() == request.getScopeType()
                        && Objects.equals(existing.getScopeId(), request.getScopeId())
                        && Objects.equals(existing.getInstrumentSn(), request.getInstrumentSn())
                        && existing.getAction() == request.getAction()
                        && Objects.equals(existing.getAmount(), request.getAmount())
                        && existing.getCurrency() == request.getCurrency()
                        && Objects.equals(existing.getBusinessScene(), request.getBusinessScene())
                        && Objects.equals(existing.getBusinessSn(), request.getBusinessSn())
                        && existing.getDecisionResult() == request.getDecisionResult()
                        && Objects.equals(existing.getRejectReason(), request.getRejectReason())
                        && Objects.equals(existing.getDecisionDigest(), request.getDecisionDigest()),
                "Spend Rule 决策流水已存在但内容不一致，decisionSn = {}",
                request.getDecisionSn());
    }
}
