package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.entities.table.BudgetGroupNameRefs;
import com.capte.funds.wallet.dal.mapper.BudgetGroupMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.wallet.mapstruct.BudgetGroupConverter;
import com.capte.funds.wallet.model.dto.BudgetGroupDTO;
import com.capte.funds.wallet.model.query.BudgetGroupQuery;
import com.capte.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.BudgetGroupService;
import com.capte.funds.wallet.service.SubjectLedgerInitializer;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预算组服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class BudgetGroupServiceImpl implements BudgetGroupService {

    private final BudgetGroupMapper budgetGroupMapper;

    private final SubjectLedgerInitializer subjectLedgerInitializer;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createBudgetGroup(@NonNull CreateBudgetGroupRequest request) {
        validatePeriodId(request);
        BudgetGroup entity = BudgetGroupConverter.INSTANCE.convertToBudgetGroup(request);
        budgetGroupMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建预算组失败");
        subjectLedgerInitializer.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(entity.getTenantId())
                .setSubjectId(entity.getSn())
                .setSubjectType(FundsSubjectType.BUDGET_GROUP)
                .setCurrency(entity.getCurrency())
                .setLedgerProfileCode(entity.getLedgerProfileCode())
                .setPeriodType(entity.getPeriodType())
                .setPeriodId(request.getPeriodId()));
        return entity.getId();
    }

    private void validatePeriodId(CreateBudgetGroupRequest request) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        if (periodType != AccountBalancePeriodType.LIFETIME) {
            AssertUtils.hasText(request.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        }
    }

    @Override
    public @NonNull BudgetGroupDTO getBudgetGroupById(@NonNull Long id) {
        BudgetGroup result = budgetGroupMapper.selectOneById(id);
        AssertUtils.notNull(result, "预算组不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull BudgetGroupDTO getBudgetGroup(@NonNull FundsAccountId accountId) {
        BudgetGroupNameRefs ref = BudgetGroupNameRefs.budgetGroup;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.budgetType.eq(accountId.type()));
        BudgetGroup result = budgetGroupMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "预算组不存在，accountId = {}", accountId);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<BudgetGroupDTO> queryBudgetGroups(
            @NonNull BudgetGroupQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        BudgetGroupNameRefs ref = BudgetGroupNameRefs.budgetGroup;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.budgetType.eq(query.getBudgetType()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.status.eq(query.getStatus()));
        return MybatisQueryHelper.<BudgetGroup, BudgetGroupDTO>query(wrapper)
                .counter(budgetGroupMapper::selectCountByQuery)
                .resultQueryFunc(budgetGroupMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private BudgetGroupDTO toDTO(BudgetGroup entity) {
        BudgetGroupDTO result = BudgetGroupConverter.INSTANCE.convertToBudgetGroupDTO(entity);
        return result.setLedgerIds(loadLedgerIds(entity));
    }

    private Map<LedgerSubjectCode, Long> loadLedgerIds(BudgetGroup entity) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(entity.getTenantId())
                        .setSubjectId(entity.getSn())
                        .setSubjectType(FundsSubjectType.BUDGET_GROUP.name())
                        .setCurrency(entity.getCurrency()),
                DefaultPageQueryOptions.defaults(50)).getRecords().stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, LedgerDTO::getId));
    }
}
