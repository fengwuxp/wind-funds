package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.dal.entities.SpendRuleDefinition;
import com.wind.funds.wallet.dal.entities.table.SpendRuleDefinitionNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleDefinitionMapper;
import com.wind.funds.wallet.enums.SpendRuleDefinitionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Spend Rule 定义基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDefinitionServiceImpl implements SpendRuleDefinitionService {

    private final SpendRuleDefinitionMapper spendRuleDefinitionMapper;

    @Override
    public @NonNull Long createDefinition(
            @NonNull CreateSpendRuleDefinitionRequest request) {
        SpendRuleDefinition entity = toEntity(request);
        spendRuleDefinitionMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建 Spend Rule 定义失败，ruleId = {}", request.getRuleId());
        return entity.getId();
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

    private SpendRuleDefinition toEntity(CreateSpendRuleDefinitionRequest request) {
        SpendRuleDefinition result = new SpendRuleDefinition();
        result.setTenantId(request.getTenantId());
        result.setRuleId(request.getRuleId());
        result.setRuleName(request.getRuleName());
        result.setRuleType(request.getRuleType());
        result.setRuleDomain(request.getRuleDomain());
        result.setStatus(SpendRuleDefinitionStatus.ACTIVE);
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
                .setStatus(entity.getStatus())
                .setDescription(entity.getDescription());
    }
}
