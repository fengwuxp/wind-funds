package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.dal.entities.SpendRuleDefinition;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDefinitionNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleDefinitionMapper;
import com.wind.funds.wallet.enums.SpendRuleBindingState;
import com.wind.funds.wallet.enums.SpendRuleDefinitionState;
import com.wind.funds.wallet.enums.SpendRuleVersionState;
import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.query.SpendRuleBindingQuery;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.service.SpendRuleBindingService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Spend Rule 定义服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDefinitionServiceImpl implements SpendRuleDefinitionService {

    private final SpendRuleDefinitionMapper spendRuleDefinitionMapper;

    private final SpendRuleVersionService spendRuleVersionService;

    private final SpendRuleBindingService spendRuleBindingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createDefinition(
            @NonNull CreateSpendRuleDefinitionRequest request) {
        validateCreateDefinitionRequest(request);
        SpendRuleDefinitionDTO existing = findDefinition(request.getTenantId(), request.getRuleId());
        if (existing != null) {
            assertSameDefinition(request, existing);
            return existing.getId();
        }
        try {
            return insertDefinition(request);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentDefinitionAfterInsertConflict(request, exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull SpendRuleVersionDTO publishVersion(
            @NonNull PublishSpendRuleVersionRequest request) {
        validatePublishVersionRequest(request);
        SpendRuleDefinitionDTO definition = findDefinition(request.getTenantId(), request.getRuleId());
        AssertUtils.notNull(definition, "Spend Rule 定义不存在，ruleId = {}", request.getRuleId());
        AssertUtils.isTrue(definition.getState() == SpendRuleDefinitionState.ACTIVE,
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
    public @NonNull SpendRuleBindingDTO createSpendRuleBinding(
            @NonNull CreateSpendRuleBindingRequest request) {
        validateBindingRequest(request);
        spendRuleVersionService.getPublishedVersion(request.getTenantId(),
                request.getRuleId(),
                request.getRuleVersion());
        SpendRuleBindingDTO existing = findBindingByBusinessKey(request);
        if (existing != null) {
            assertSameBinding(request, existing);
            return existing;
        }
        try {
            Long bindingId = spendRuleBindingService.createSpendRuleBinding(request);
            return spendRuleBindingService.getSpendRuleBindingById(bindingId);
        } catch (DataIntegrityViolationException exception) {
            return readIdempotentBindingAfterInsertConflict(request, exception);
        }
    }

    @Override
    public @NonNull SpendRuleDefinitionDTO getDefinitionById(@NonNull Long id) {
        SpendRuleDefinition entity = spendRuleDefinitionMapper.selectOneById(id);
        AssertUtils.notNull(entity, "Spend Rule 定义不存在，id = {}", id);
        return toDTO(entity);
    }

    @Override
    public @Nullable SpendRuleDefinitionDTO findDefinition(@NonNull Long tenantId,
                                                           @NonNull String ruleId) {
        SpendRuleDefinition entity = findDefinitionEntity(tenantId, ruleId);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    private SpendRuleDefinition findDefinitionEntity(Long tenantId, String ruleId) {
        SpendRuleDefinitionNameRefs ref = SpendRuleDefinitionNameRefs.spendRuleDefinition;
        return spendRuleDefinitionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ruleId.eq(ruleId)));
    }

    private Long insertDefinition(CreateSpendRuleDefinitionRequest request) {
        SpendRuleDefinition entity = toEntity(request);
        spendRuleDefinitionMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建 Spend Rule 定义失败，ruleId = {}", request.getRuleId());
        return entity.getId();
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
        SpendRuleDigestValidator.assertSha256Digest(request.getRuleDigest(), "Spend Rule 规则摘要");
        AssertUtils.hasText(request.getOperatorId(), "Spend Rule 发布操作者不能为空");
        AssertUtils.hasText(request.getAuditReferenceSn(), "Spend Rule 发布审计引用不能为空");
    }

    private void validateBindingRequest(CreateSpendRuleBindingRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
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
        AssertUtils.hasText(request.getAuditReferenceSn(), "Spend Rule 挂载审计引用不能为空");
    }

    private Long readIdempotentDefinitionAfterInsertConflict(
            CreateSpendRuleDefinitionRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleDefinitionDTO existing = findDefinition(request.getTenantId(), request.getRuleId());
        if (existing == null) {
            throw exception;
        }
        assertSameDefinition(request, existing);
        return existing.getId();
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

    private SpendRuleBindingDTO readIdempotentBindingAfterInsertConflict(
            CreateSpendRuleBindingRequest request,
            DataIntegrityViolationException exception) {
        SpendRuleBindingDTO existing = findBindingByBusinessKey(request);
        if (existing == null) {
            throw exception;
        }
        assertSameBinding(request, existing);
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
        AssertUtils.isTrue(existing.getState() == SpendRuleVersionState.PUBLISHED,
                "Spend Rule 版本状态不可重复发布，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
        AssertUtils.isTrue(Objects.equals(existing.getRuleDigest(), request.getRuleDigest())
                        && Objects.equals(existing.getRuleSpec(), request.getRuleSpec()),
                "Spend Rule 版本已发布但内容摘要不一致，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
    }

    private void assertSameBinding(CreateSpendRuleBindingRequest request,
                                      SpendRuleBindingDTO existing) {
        AssertUtils.isTrue(existing.getState() == SpendRuleBindingState.ACTIVE
                        && Objects.equals(existing.getRuleId(), request.getRuleId())
                        && Objects.equals(existing.getRuleVersion(), request.getRuleVersion())
                        && existing.getScopeType() == request.getScopeType()
                        && Objects.equals(existing.getScopeId(), request.getScopeId())
                        && Objects.equals(existing.getPriority(), request.getPriority())
                        && existing.getConflictPolicy() == request.getConflictPolicy()
                        && Objects.equals(existing.getEffectiveFrom(), request.getEffectiveFrom())
                        && Objects.equals(existing.getEffectiveTo(), request.getEffectiveTo())
                        && Objects.equals(existing.getAuditReferenceSn(), request.getAuditReferenceSn()),
                "Spend Rule 挂载已存在但内容不一致，ruleId = {}, ruleVersion = {}, scopeType = {}, scopeId = {}, auditReferenceSn = {}",
                request.getRuleId(),
                request.getRuleVersion(),
                request.getScopeType(),
                request.getScopeId(),
                request.getAuditReferenceSn());
    }

    private SpendRuleBindingDTO findBindingByBusinessKey(CreateSpendRuleBindingRequest request) {
        return spendRuleBindingService.querySpendRuleBindings(new SpendRuleBindingQuery()
                        .setTenantId(request.getTenantId())
                        .setRuleId(request.getRuleId())
                        .setRuleVersion(request.getRuleVersion())
                        .setScopeType(request.getScopeType())
                        .setScopeId(request.getScopeId())
                        .setAuditReferenceSn(request.getAuditReferenceSn()))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private SpendRuleDefinition toEntity(CreateSpendRuleDefinitionRequest request) {
        SpendRuleDefinition result = new SpendRuleDefinition();
        result.setTenantId(request.getTenantId());
        result.setRuleId(request.getRuleId());
        result.setRuleName(request.getRuleName());
        result.setRuleType(request.getRuleType());
        result.setRuleDomain(request.getRuleDomain());
        result.setState(SpendRuleDefinitionState.ACTIVE);
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleDefinitionDTO toDTO(SpendRuleDefinition entity) {
        return new SpendRuleDefinitionDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setRuleId(entity.getRuleId())
                .setRuleName(entity.getRuleName())
                .setRuleType(entity.getRuleType())
                .setRuleDomain(entity.getRuleDomain())
                .setState(entity.getState())
                .setDescription(entity.getDescription());
    }
}
