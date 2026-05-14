package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.mapstruct.FundsFrozenOrderConverter;
import com.capte.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.capte.funds.transaction.model.query.FundsFrozenOrderQuery;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.capte.funds.transaction.services.FundsFrozenOrderService;
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
 * 资金冻结订单服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class FundsFrozenOrderServiceImpl implements FundsFrozenOrderService {

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createFundsFrozenOrder(@NonNull CreateFundsFrozenOrderRequest request) {
        FundsFrozenOrder entity = FundsFrozenOrderConverter.INSTANCE.convertToFundsFrozenOrder(request);
        fundsFrozenOrderMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金冻结订单失败");
        return entity.getId();
    }

    @Override
    public @NonNull FundsFrozenOrderDTO getFundsFrozenOrderById(@NonNull Long id) {
        FundsFrozenOrder result = fundsFrozenOrderMapper.selectOneById(id);
        AssertUtils.notNull(result, "资金冻结订单不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<FundsFrozenOrderDTO> queryFundsFrozenOrders(
            @NonNull FundsFrozenOrderQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.subjectId.eq(query.getSubjectId()))
                .and(ref.subjectType.eq(query.getSubjectType()))
                .and(ref.freezeType.eq(query.getFreezeType()))
                .and(ref.businessScene.eq(query.getBusinessScene()))
                .and(ref.businessSn.eq(query.getBusinessSn()))
                .and(ref.transactionSn.eq(query.getTransactionSn()))
                .and(ref.status.eq(query.getStatus()))
                .and(ref.currency.eq(query.getCurrency()));
        return MybatisQueryHelper.<FundsFrozenOrder, FundsFrozenOrderDTO>query(wrapper)
                .counter(fundsFrozenOrderMapper::selectCountByQuery)
                .resultQueryFunc(fundsFrozenOrderMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private FundsFrozenOrderDTO toDTO(FundsFrozenOrder entity) {
        return FundsFrozenOrderConverter.INSTANCE.convertToFundsFrozenOrderDTO(entity);
    }
}
