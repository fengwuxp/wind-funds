package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.dal.entities.SpendControlActivity;
import com.wind.funds.wallet.dal.entities.table.SpendControlActivityNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendControlActivityMapper;
import com.wind.funds.wallet.mapstruct.SpendControlActivityConverter;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.funds.wallet.service.SpendControlActivityService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 控制额度变动流水基础服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendControlActivityServiceImpl implements SpendControlActivityService {

    private final SpendControlActivityMapper spendControlActivityMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = DataIntegrityViolationException.class)
    public @NonNull Long createSpendControlActivity(@NonNull RecordSpendControlActivityRequest request) {
        SpendControlActivity entity = SpendControlActivityConverter.INSTANCE.convertToSpendControlActivity(request);
        spendControlActivityMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "记录控制额度变动流水失败，activitySn = {}", request.getActivitySn());
        return entity.getId();
    }

    @Override
    public @NonNull SpendControlActivityDTO getSpendControlActivityById(@NonNull Long id) {
        SpendControlActivity result = spendControlActivityMapper.selectOneById(id);
        AssertUtils.notNull(result, "控制额度变动流水不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @Nullable SpendControlActivityDTO findSpendControlActivity(@NonNull Long tenantId,
                                                                      @NonNull String activitySn) {
        SpendControlActivity entity = findActivityEntity(tenantId, activitySn);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    @Override
    public @NonNull WindPagination<SpendControlActivityDTO> querySpendControlActivities(
            @NonNull SpendControlActivityQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return MybatisQueryHelper.<SpendControlActivity, SpendControlActivityDTO>query(toQueryWrapper(query, options))
                .counter(spendControlActivityMapper::selectCountByQuery)
                .resultQueryFunc(spendControlActivityMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendControlActivity findActivityEntity(Long tenantId, String activitySn) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.activitySn.eq(activitySn));
        return spendControlActivityMapper.selectOneByQuery(wrapper);
    }

    private QueryWrapper toQueryWrapper(SpendControlActivityQuery query,
                                        WindQuery<? extends QueryOrderField> options) {
        SpendControlActivityNameRefs ref = SpendControlActivityNameRefs.spendControlActivity;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.tenantId.eq(query.getTenantId()))
                .and(ref.activitySn.eq(query.getActivitySn()))
                .and(ref.activityType.eq(query.getActivityType()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
                .and(ref.originalActivitySn.eq(query.getOriginalActivitySn()))
                .and(ref.transactionSn.eq(query.getTransactionSn()))
                .and(ref.instrumentSn.eq(query.getInstrumentSn()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.spendRuleId.eq(query.getSpendRuleId()))
                .and(ref.spendRuleVersion.eq(query.getSpendRuleVersion()))
                .and(ref.budgetGroupSn.eq(query.getBudgetGroupSn()));
        if (query.getTargetAccountId() != null) {
            wrapper.and(ref.targetSubjectId.eq(query.getTargetAccountId().id()))
                    .and(ref.targetSubjectType.eq(targetSubjectType(query.getTargetAccountId())));
        }
        wrapper.orderBy(ref.id.asc());
        return wrapper;
    }

    private SpendControlActivityDTO toDTO(SpendControlActivity entity) {
        return SpendControlActivityConverter.INSTANCE.convertToSpendControlActivityDTO(entity);
    }

    private FundsSubjectType targetSubjectType(FundsAccountId accountId) {
        boolean matched = Arrays.stream(FundsSubjectType.values())
                .anyMatch(type -> type.name().equals(accountId.type()));
        AssertUtils.isTrue(matched, "控制活动目标账户类型非法，targetAccountId = {}", accountId);
        return FundsSubjectType.valueOf(accountId.type());
    }
}
