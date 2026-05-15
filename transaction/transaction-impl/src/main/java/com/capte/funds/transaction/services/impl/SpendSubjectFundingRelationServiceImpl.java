package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.SpendSubjectFundingRel;
import com.capte.funds.transaction.dal.entities.table.SpendSubjectFundingRelNameRefs;
import com.capte.funds.transaction.dal.mapper.SpendSubjectFundingRelMapper;
import com.capte.funds.transaction.mapstruct.SpendSubjectFundingRelationConverter;
import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.capte.funds.wallet.service.SpendSubjectFundingRelationService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createSpendSubjectFundingRelation(
            @NonNull CreateSpendSubjectFundingRelationRequest request) {
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
        return MybatisQueryHelper.<SpendSubjectFundingRel, SpendSubjectFundingRelationDTO>query(wrapper)
                .counter(spendSubjectFundingRelMapper::selectCountByQuery)
                .resultQueryFunc(spendSubjectFundingRelMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendSubjectFundingRelationDTO toDTO(SpendSubjectFundingRel entity) {
        return SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRelationDTO(entity);
    }
}
