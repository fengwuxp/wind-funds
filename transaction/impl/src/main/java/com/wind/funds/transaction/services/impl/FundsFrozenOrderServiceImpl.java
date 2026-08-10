package com.wind.funds.transaction.services.impl;

import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.mapstruct.FundsFrozenOrderConverter;
import com.wind.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.wind.funds.transaction.model.query.FundsFrozenOrderQuery;
import com.wind.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.funds.transaction.services.FundsFrozenOrderService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.transaction.support.FundsInstructionContextValidator;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
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
        validateCreateRequest(request);
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

    private void validateCreateRequest(CreateFundsFrozenOrderRequest request) {
        AssertUtils.hasText(request.getSn(), "冻结单号不能为空");
        AssertUtils.notNull(request.getTenantId(), "冻结单租户不能为空");
        AssertUtils.hasText(request.getSubjectId(), "冻结主体不能为空");
        AssertUtils.notNull(request.getSubjectType(), "冻结主体类型不能为空");
        AssertUtils.hasText(request.getFreezeType(), "冻结类型不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "冻结业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "冻结业务号不能为空");
        AssertUtils.notNull(request.getAmount(), "冻结金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "冻结金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "冻结币种不能为空");
        assertNoSensitiveContextVariables(request.getContextVariables());
    }

    private void assertNoSensitiveContextVariables(String contextVariables) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(contextVariables),
                "contextVariables must not contain sensitive funds frozen order fields");
        FundsInstructionContextValidator.rejectInstructionContextVariables(contextVariables, "fundsFrozenOrder");
    }
}
