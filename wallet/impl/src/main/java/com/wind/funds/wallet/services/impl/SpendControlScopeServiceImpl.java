package com.wind.funds.wallet.services.impl;

import com.wind.funds.wallet.dal.entities.SpendControlScope;
import com.wind.funds.wallet.dal.entities.table.SpendControlScopeNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendControlScopeMapper;
import com.wind.funds.wallet.mapstruct.SpendControlScopeConverter;
import com.wind.funds.wallet.model.dto.SpendControlScopeDTO;
import com.wind.funds.wallet.model.query.SpendControlScopeQuery;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.service.SpendControlScopeService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支出控制范围服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class SpendControlScopeServiceImpl implements SpendControlScopeService {

    private final SpendControlScopeMapper spendControlScopeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createSpendControlScope(@NonNull CreateSpendControlScopeRequest request) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        validatePeriodId(request);
        SpendControlScope entity = SpendControlScopeConverter.INSTANCE.convertToSpendControlScope(request);
        spendControlScopeMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支出控制范围失败");
        return entity.getId();
    }

    private void validatePeriodId(CreateSpendControlScopeRequest request) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        if (periodType != AccountBalancePeriodType.LIFETIME) {
            AssertUtils.hasText(request.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        }
        if (periodType == AccountBalancePeriodType.CUSTOM_CYCLE) {
            AssertUtils.hasText(request.getPeriodPolicy(), "自定义周期支出控制范围 periodPolicy 不能为空");
        }
    }

    @Override
    public @NonNull SpendControlScopeDTO getSpendControlScopeById(@NonNull Long id) {
        SpendControlScope result = spendControlScopeMapper.selectOneById(id);
        AssertUtils.notNull(result, "支出控制范围不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull SpendControlScopeDTO getSpendControlScope(@NonNull Long tenantId,
                                                         @NonNull String controlScopeId,
                                                         @NonNull String scopeType) {
        AssertUtils.notNull(tenantId, "租户 ID 不能为空");
        AssertUtils.hasText(controlScopeId, "支出控制范围标识不能为空");
        AssertUtils.hasText(scopeType, "支出控制范围业务类型不能为空");
        return findSpendControlScope(tenantId, controlScopeId, scopeType);
    }

    private SpendControlScopeDTO findSpendControlScope(Long tenantId, String controlScopeId, String scopeType) {
        SpendControlScopeNameRefs ref = SpendControlScopeNameRefs.spendControlScope;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(controlScopeId))
                .and(ref.scopeType.eq(scopeType))
                .and(ref.tenantId.eq(tenantId));
        SpendControlScope result = spendControlScopeMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "支出控制范围不存在，controlScopeId = {}, scopeType = {}",
                controlScopeId, scopeType);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<SpendControlScopeDTO> querySpendControlScopes(
            @NonNull SpendControlScopeQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        SpendControlScopeNameRefs ref = SpendControlScopeNameRefs.spendControlScope;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.scopeType.eq(query.getScopeType()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.periodType.eq(query.getPeriodType()))
                .and(ref.periodId.eq(query.getPeriodId()))
                .and(ref.state.eq(query.getState()));
        return MybatisQueryHelper.<SpendControlScope, SpendControlScopeDTO>query(wrapper)
                .counter(spendControlScopeMapper::selectCountByQuery)
                .resultQueryFunc(spendControlScopeMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendControlScopeDTO toDTO(SpendControlScope entity) {
        return SpendControlScopeConverter.INSTANCE.convertToSpendControlScopeDTO(entity);
    }
}
