package com.capte.funds.wallet.services.impl;

import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.entities.SpendSubjectFundingRel;
import com.capte.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.wallet.dal.entities.table.SpendSubjectFundingRelNameRefs;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.capte.funds.wallet.dal.mapper.SpendSubjectFundingRelMapper;
import com.capte.funds.wallet.mapstruct.SpendSubjectFundingRelationConverter;
import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.capte.funds.wallet.service.SpendSubjectFundingRelationService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支出主体资金关系服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class SpendSubjectFundingRelationServiceImpl implements SpendSubjectFundingRelationService {

    private final SpendSubjectFundingRelMapper spendSubjectFundingRelMapper;

    private final FundingAccountMapper fundingAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createSpendSubjectFundingRelation(
            @NonNull CreateSpendSubjectFundingRelationRequest request) {
        FundingAccount fundingAccount = getFundingAccount(request.getTenantId(), request.getFundingAccountId());
        assertFundingAccountCanBind(fundingAccount, request);
        assertNoDuplicateActiveDefaultRelation(request);
        assertNoDuplicateActivePriorityRelation(request);
        SpendSubjectFundingRel entity =
                SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRel(request);
        spendSubjectFundingRelMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支出主体资金关系失败");
        return entity.getId();
    }

    @Override
    public @NonNull SpendSubjectFundingRelationDTO getSpendSubjectFundingRelationById(@NonNull Long id) {
        SpendSubjectFundingRel result = spendSubjectFundingRelMapper.selectOneById(id);
        AssertUtils.notNull(result, "支出主体资金关系不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<SpendSubjectFundingRelationDTO> querySpendSubjectFundingRelations(
            @NonNull SpendSubjectFundingRelationQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.spendSubjectId.eq(query.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(query.getSpendSubjectType()))
                .and(ref.fundingAccountId.eq(query.getFundingAccountId()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.relationType.eq(query.getRelationType()))
                .and(ref.defaultRelation.eq(query.getDefaultRelation()))
                .and(ref.status.eq(query.getStatus()));
        applyCurrentEffectiveWindow(wrapper, ref, query.getStatus());
        wrapper.orderBy(ref.priority.asc(), ref.id.asc());
        return MybatisQueryHelper.<SpendSubjectFundingRel, SpendSubjectFundingRelationDTO>query(wrapper)
                .counter(spendSubjectFundingRelMapper::selectCountByQuery)
                .resultQueryFunc(spendSubjectFundingRelMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendSubjectFundingRelationDTO toDTO(SpendSubjectFundingRel entity) {
        return SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRelationDTO(entity);
    }

    private FundingAccount getFundingAccount(Long tenantId, String fundingAccountId) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(fundingAccountId));
        FundingAccount result = fundingAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "资金账户不存在，fundingAccountId = {}", fundingAccountId);
        return result;
    }

    private void assertFundingAccountCanBind(FundingAccount fundingAccount,
                                             CreateSpendSubjectFundingRelationRequest request) {
        AssertUtils.isTrue(fundingAccount.getStatus().canDebit(),
                "资金账户不可作为资金来源，fundingAccountId = {}", request.getFundingAccountId());
        AssertUtils.equals(fundingAccount.getCurrency(), request.getCurrency(),
                "资金账户币种与资金来源关系币种不一致，fundingAccountId = {}", request.getFundingAccountId());
    }

    private void assertNoDuplicateActiveDefaultRelation(CreateSpendSubjectFundingRelationRequest request) {
        FundsAccountStatus status = request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus();
        if (!Boolean.TRUE.equals(request.getDefaultRelation()) || status != FundsAccountStatus.ACTIVE) {
            return;
        }
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(request.getTenantId()))
                .and(ref.spendSubjectId.eq(request.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(request.getSpendSubjectType()))
                .and(ref.currency.eq(request.getCurrency()))
                .and(ref.relationType.eq(request.getRelationType()))
                .and(ref.defaultRelation.eq(Boolean.TRUE))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = spendSubjectFundingRelMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        request.getValidFrom(),
                        request.getValidTo()));
        AssertUtils.isFalse(duplicated,
                "默认资金来源关系不唯一，spendSubjectId = {}, relationType = {}, currency = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency());
    }

    private void assertNoDuplicateActivePriorityRelation(CreateSpendSubjectFundingRelationRequest request) {
        FundsAccountStatus status = request.getStatus() == null ? FundsAccountStatus.ACTIVE : request.getStatus();
        if (status != FundsAccountStatus.ACTIVE) {
            return;
        }
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        int priority = request.getPriority() == null ? 0 : request.getPriority();
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(request.getTenantId()))
                .and(ref.spendSubjectId.eq(request.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(request.getSpendSubjectType()))
                .and(ref.currency.eq(request.getCurrency()))
                .and(ref.relationType.eq(request.getRelationType()))
                .and(ref.priority.eq(priority))
                .and(ref.status.eq(FundsAccountStatus.ACTIVE));
        boolean duplicated = spendSubjectFundingRelMapper.selectListByQuery(wrapper).stream()
                .anyMatch(existing -> validityWindowsOverlap(existing.getValidFrom(),
                        existing.getValidTo(),
                        request.getValidFrom(),
                        request.getValidTo()));
        AssertUtils.isFalse(duplicated,
                "资金来源关系优先级冲突，spendSubjectId = {}, relationType = {}, currency = {}, priority = {}",
                request.getSpendSubjectId(),
                request.getRelationType(),
                request.getCurrency(),
                priority);
    }

    private void applyCurrentEffectiveWindow(QueryWrapper wrapper,
                                             SpendSubjectFundingRelNameRefs ref,
                                             FundsAccountStatus status) {
        if (status != FundsAccountStatus.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(ref.validFrom.isNull().or(ref.validFrom.le(now)))
                .and(ref.validTo.isNull().or(ref.validTo.gt(now)));
    }

    private boolean validityWindowsOverlap(LocalDateTime leftFrom,
                                           LocalDateTime leftTo,
                                           LocalDateTime rightFrom,
                                           LocalDateTime rightTo) {
        boolean leftEndsAfterRightStarts = leftTo == null || rightFrom == null || leftTo.isAfter(rightFrom);
        boolean rightEndsAfterLeftStarts = rightTo == null || leftFrom == null || rightTo.isAfter(leftFrom);
        return leftEndsAfterRightStarts && rightEndsAfterLeftStarts;
    }
}
