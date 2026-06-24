package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.dal.entities.SpendRuleVersion;
import com.wind.funds.wallet.dal.entities.table.SpendRuleVersionNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendRuleVersionMapper;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.service.SpendRuleVersionService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Spend Rule 版本基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleVersionServiceImpl implements SpendRuleVersionService {

    private final SpendRuleVersionMapper spendRuleVersionMapper;

    @Override
    public @NonNull Long createVersion(
            @NonNull PublishSpendRuleVersionRequest request) {
        SpendRuleVersion entity = toEntity(request);
        spendRuleVersionMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "发布 Spend Rule 版本失败，ruleId = {}, ruleVersion = {}",
                request.getRuleId(),
                request.getRuleVersion());
        return entity.getId();
    }

    @Override
    public @NonNull SpendRuleVersionDTO getVersionById(@NonNull Long id) {
        SpendRuleVersion entity = spendRuleVersionMapper.selectOneById(id);
        AssertUtils.notNull(entity, "Spend Rule 版本不存在，id = {}", id);
        return toDTO(entity);
    }

    @Override
    public @Nullable SpendRuleVersionDTO findVersion(@NonNull Long tenantId,
                                                     @NonNull String ruleId,
                                                     @NonNull String ruleVersion) {
        SpendRuleVersion entity = findVersionEntity(tenantId, ruleId, ruleVersion);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull SpendRuleVersionDTO getPublishedVersion(@NonNull Long tenantId,
                                                            @NonNull String ruleId,
                                                            @NonNull String ruleVersion) {
        SpendRuleVersionDTO version = findVersion(tenantId, ruleId, ruleVersion);
        AssertUtils.notNull(version, "Spend Rule 版本不存在，ruleId = {}, ruleVersion = {}", ruleId, ruleVersion);
        AssertUtils.isTrue(version.getStatus() == SpendRuleVersionStatus.PUBLISHED,
                "Spend Rule 版本未发布，ruleId = {}, ruleVersion = {}",
                ruleId,
                ruleVersion);
        return version;
    }

    private SpendRuleVersion findVersionEntity(Long tenantId, String ruleId, String ruleVersion) {
        SpendRuleVersionNameRefs ref = SpendRuleVersionNameRefs.spendRuleVersion;
        return spendRuleVersionMapper.selectOneByQuery(QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.ruleId.eq(ruleId))
                .and(ref.ruleVersion.eq(ruleVersion)));
    }

    private SpendRuleVersion toEntity(PublishSpendRuleVersionRequest request) {
        SpendRuleVersion result = new SpendRuleVersion();
        result.setTenantId(request.getTenantId());
        result.setRuleId(request.getRuleId());
        result.setRuleVersion(request.getRuleVersion());
        result.setRuleSpec(request.getRuleSpec());
        result.setRuleDigest(request.getRuleDigest());
        result.setStatus(SpendRuleVersionStatus.PUBLISHED);
        result.setOperatorId(request.getOperatorId());
        result.setAuditReferenceSn(request.getAuditReferenceSn());
        result.setDescription(request.getDescription());
        return result;
    }

    private SpendRuleVersionDTO toDTO(SpendRuleVersion entity) {
        return new SpendRuleVersionDTO()
                .setId(entity.getId())
                .setGmtCreate(entity.getGmtCreate())
                .setGmtModified(entity.getGmtModified())
                .setTenantId(entity.getTenantId())
                .setRuleId(entity.getRuleId())
                .setRuleVersion(entity.getRuleVersion())
                .setRuleSpec(entity.getRuleSpec())
                .setRuleDigest(entity.getRuleDigest())
                .setStatus(entity.getStatus())
                .setOperatorId(entity.getOperatorId())
                .setAuditReferenceSn(entity.getAuditReferenceSn())
                .setDescription(entity.getDescription());
    }
}
