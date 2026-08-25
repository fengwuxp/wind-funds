package com.wind.funds.wallet.application.funding.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.application.funding.FundingResponsibilityResolutionApplicationService;
import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资金责任解析应用服务实现。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Slf4j
@Service
@AllArgsConstructor
public class FundingResponsibilityResolutionApplicationServiceImpl
        implements FundingResponsibilityResolutionApplicationService {

    private final SpendSubjectFundingRelationService fundingRelationService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull FundingResponsibilityDecisionDTO resolveFundingResponsibility(
            @NonNull ResolveFundingResponsibilityRequest request) {
        validateRequest(request);
        List<SpendSubjectFundingRelationDTO> records = fundingRelationService.querySpendSubjectFundingRelations(
                new SpendSubjectFundingRelationQuery()
                        .setTenantId(request.getTenantId())
                        .setSpendSubjectId(request.getSpendSubjectId())
                        .setSpendSubjectType(request.getSpendSubjectType())
                        .setCurrency(request.getCurrency())
                        .setRelationType(request.getRelationType()),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isFalse(records.isEmpty(),
                "资金责任关系不存在，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
        AssertUtils.isTrue(records.size() == 1,
                "资金责任关系不唯一，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
        FundingResponsibilityDecisionDTO decision = toDecision(records.getFirst());
        log.info("资金责任解析完成，tenantId={}, spendSubjectType={}, spendSubjectId={}, relationType={}, "
                        + "targetSubjectType={}, targetSubjectId={}, currency={}, relationSn={}",
                request.getTenantId(), request.getSpendSubjectType(), request.getSpendSubjectId(),
                request.getRelationType(), decision.getTargetSubjectType(), decision.getTargetSubjectId(),
                decision.getCurrency(), decision.getRelationSn());
        return decision;
    }

    private void validateRequest(ResolveFundingResponsibilityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "资金责任解析 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getSpendSubjectId(), "支出控制主体 ID 不能为空");
        AssertUtils.notNull(request.getSpendSubjectType(), "支出控制主体类型不能为空");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.notNull(request.getRelationType(), "资金责任关系类型不能为空");
    }

    private FundingResponsibilityDecisionDTO toDecision(SpendSubjectFundingRelationDTO relation) {
        return new FundingResponsibilityDecisionDTO()
                .setRelationId(relation.getId())
                .setRelationSn(relation.getSn())
                .setTenantId(relation.getTenantId())
                .setSpendSubjectId(relation.getSpendSubjectId())
                .setSpendSubjectType(relation.getSpendSubjectType())
                .setTargetSubjectType(relation.getTargetSubjectType())
                .setTargetSubjectId(relation.getTargetSubjectId())
                .setCurrency(relation.getCurrency())
                .setRelationType(relation.getRelationType());
    }
}
