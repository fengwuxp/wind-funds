package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.enums.SpendRuleDefinitionStatus;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
import com.wind.funds.wallet.service.SpendRuleDefinitionDomainService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Spend Rule 定义领域写服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDefinitionDomainServiceImpl implements SpendRuleDefinitionDomainService {

    private final SpendRuleDefinitionService spendRuleDefinitionService;

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendRuleAssignmentService spendRuleAssignmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleDefinitionDTO createDefinition(
            @NonNull CreateSpendRuleDefinitionRequest request) {
        validateCreateDefinitionRequest(request);
        SpendRuleDefinitionDTO existing = spendRuleDefinitionService.findDefinition(
                request.getTenantId(),
                request.getRuleId());
        if (existing != null) {
            assertSameDefinition(request, existing);
            return existing;
        }
        try {
            Long definitionId = spendRuleDefinitionService.createDefinition(request);
            return spendRuleDefinitionService.getDefinitionById(definitionId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDefinitionAfterInsertConflict(request, exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleVersionDTO publishVersion(
            @NonNull PublishSpendRuleVersionRequest request) {
        validatePublishVersionRequest(request);
        SpendRuleDefinitionDTO definition = spendRuleDefinitionService.findDefinition(
                request.getTenantId(),
                request.getRuleId());
        AssertUtils.notNull(definition, "Spend Rule 定义不存在，ruleId = {}", request.getRuleId());
        AssertUtils.isTrue(definition.getStatus() == SpendRuleDefinitionStatus.ACTIVE,
                "Spend Rule 定义不可用，ruleId = {}",
                request.getRuleId());
        SpendRuleVersionDTO existing = spendRuleVersionService.findVersion(
                request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        if (existing != null) {
            assertSamePublishedVersion(request, existing);
            return existing;
        }
        try {
            Long versionId = spendRuleVersionService.createVersion(request);
            return spendRuleVersionService.getVersionById(versionId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentVersionAfterInsertConflict(request, exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleAssignmentDTO assignVersion(
            @NonNull AssignSpendRuleVersionRequest request) {
        validateAssignmentRequest(request);
        spendRuleVersionService.getPublishedVersion(request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        SpendRuleAssignmentDTO existing = spendRuleAssignmentService.findAssignment(
                request.getTenantId(),
                request.getAssignmentSn());
        if (existing != null) {
            assertSameAssignment(request, existing);
            return existing;
        }
        try {
            Long assignmentId = spendRuleAssignmentService.createAssignment(request);
            return spendRuleAssignmentService.getAssignmentById(assignmentId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentAssignmentAfterInsertConflict(request, exception);
        }
    }

    private void validateCreateDefinitionRequest(CreateSpendRuleDefinitionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleName(), "Spend Rule 名称不能为空");
        AssertUtils.notNull(request.getRuleType(), "Spend Rule 类型不能为空");
        AssertUtils.notNull(request.getRuleDomain(), "Spend Rule 规则域不能为空");
    }

    private void validatePublishVersionRequest(PublishSpendRuleVersionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.hasText(request.getRuleSpec(), "Spend Rule 规则规格不能为空");
        AssertUtils.hasText(request.getRuleDigest(), "Spend Rule 规则摘要不能为空");
        AssertUtils.hasText(request.getOperatorId(), "Spend Rule 发布操作者不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "Spend Rule 发布审计引用不能为空");
    }

    private void validateAssignmentRequest(AssignSpendRuleVersionRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getAssignmentSn(), "Spend Rule 挂载流水号不能为空");
        AssertUtils.hasText(request.getRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getScopeType(), "Spend Rule 挂载范围类型不能为空");
        AssertUtils.hasText(request.getScopeId(), "Spend Rule 挂载范围标识不能为空");
        AssertUtils.notNull(request.getPriority(), "Spend Rule 挂载优先级不能为空");
        AssertUtils.notNull(request.getConflictPolicy(), "Spend Rule 挂载冲突策略不能为空");
        AssertUtils.notNull(request.getEffectiveFrom(), "Spend Rule 挂载生效开始时间不能为空");
        AssertUtils.notNull(request.getEffectiveTo(), "Spend Rule 挂载生效结束时间不能为空");
        AssertUtils.isTrue(request.getEffectiveTo().isAfter(request.getEffectiveFrom()),
                "Spend Rule 挂载生效结束时间必须晚于开始时间");
    }

    private SpendRuleDefinitionDTO readIdempotentDefinitionAfterInsertConflict(
            CreateSpendRuleDefinitionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDefinitionDTO existing = spendRuleDefinitionService.findDefinition(
                request.getTenantId(),
                request.getRuleId());
        if (existing == null) {
            throw exception;
        }
        assertSameDefinition(request, existing);
        return existing;
    }

    private SpendRuleVersionDTO readIdempotentVersionAfterInsertConflict(
            PublishSpendRuleVersionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleVersionDTO existing = spendRuleVersionService.findVersion(request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        if (existing == null) {
            throw exception;
        }
        assertSamePublishedVersion(request, existing);
        return existing;
    }

    private SpendRuleAssignmentDTO readIdempotentAssignmentAfterInsertConflict(
            AssignSpendRuleVersionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleAssignmentDTO existing = spendRuleAssignmentService.findAssignment(
                request.getTenantId(),
                request.getAssignmentSn());
        if (existing == null) {
            throw exception;
        }
        assertSameAssignment(request, existing);
        return existing;
    }

    private void assertSameDefinition(CreateSpendRuleDefinitionRequest request,
                                      SpendRuleDefinitionDTO existing) {
        AssertUtils.isTrue(Objects.equals(existing.getRuleName(), request.getRuleName())
                        && existing.getRuleType() == request.getRuleType()
                        && existing.getRuleDomain() == request.getRuleDomain(),
                "Spend Rule 定义已存在但内容不一致，ruleId = {}",
                request.getRuleId());
    }

    private void assertSamePublishedVersion(PublishSpendRuleVersionRequest request,
                                            SpendRuleVersionDTO existing) {
        AssertUtils.isTrue(existing.getStatus() == SpendRuleVersionStatus.PUBLISHED,
                "Spend Rule 版本状态不可重复发布，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
        AssertUtils.isTrue(Objects.equals(existing.getRuleDigest(), request.getRuleDigest())
                        && Objects.equals(existing.getRuleSpec(), request.getRuleSpec()),
                "Spend Rule 版本已发布但内容摘要不一致，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
    }

    private void assertSameAssignment(AssignSpendRuleVersionRequest request,
                                      SpendRuleAssignmentDTO existing) {
        AssertUtils.isTrue(existing.getStatus() == SpendRuleAssignmentStatus.ACTIVE
                        && Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && existing.getScopeType() == request.getScopeType()
                        && Objects.equals(existing.getScopeId(), request.getScopeId())
                        && Objects.equals(existing.getPriority(), request.getPriority())
                        && existing.getConflictPolicy() == request.getConflictPolicy()
                        && Objects.equals(existing.getEffectiveFrom(), request.getEffectiveFrom())
                        && Objects.equals(existing.getEffectiveTo(), request.getEffectiveTo()),
                "Spend Rule 挂载已存在但内容不一致，assignmentSn = {}",
                request.getAssignmentSn());
    }
}
