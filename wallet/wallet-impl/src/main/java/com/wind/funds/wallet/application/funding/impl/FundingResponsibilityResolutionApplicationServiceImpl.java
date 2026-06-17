package com.wind.funds.wallet.application.funding.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.application.funding.FundingResponsibilityResolutionApplicationService;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import lombok.AllArgsConstructor;
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
                        .setRelationType(request.getRelationType())
                        .setDefaultRelation(Boolean.TRUE)
                        .setStatus(FundsAccountStatus.ACTIVE),
                DefaultPageQueryOptions.defaults(2)).getRecords();
        AssertUtils.isFalse(records.isEmpty(),
                "默认资金责任关系不存在，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
        AssertUtils.isTrue(records.size() == 1,
                "默认资金责任关系不唯一，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
        return toDecision(records.getFirst());
    }

    private void validateRequest(ResolveFundingResponsibilityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
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
                .setFundingAccountId(relation.getFundingAccountId())
                .setTargetSubjectType(relation.getTargetSubjectType())
                .setTargetSubjectId(relation.getTargetSubjectId())
                .setCurrency(relation.getCurrency())
                .setRelationType(relation.getRelationType())
                .setPriority(relation.getPriority())
                .setDefaultRelation(relation.getDefaultRelation());
    }
}
