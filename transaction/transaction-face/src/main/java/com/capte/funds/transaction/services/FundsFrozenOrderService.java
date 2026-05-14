package com.capte.funds.transaction.services;

import com.capte.funds.transaction.model.dto.FundsFrozenOrderDTO;
import com.capte.funds.transaction.model.query.FundsFrozenOrderQuery;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

/**
 * 资金冻结订单服务。
 *
 * <p>职责：维护上层业务选择落库的冻结订单记录。</p>
 *
 * <p>边界：冻结订单记录不等于账本冻结事实；是否创建冻结订单由上层业务决定，账本冻结以 LedgerEntry 为准。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface FundsFrozenOrderService {

    /**
     * 创建资金冻结订单。
     *
     * <p>能力范围：创建业务冻结单记录，用于业务审核、展示或撤销引用。</p>
     *
     * @param request 创建请求
     * @return 冻结单主键
     */
    @NonNull Long createFundsFrozenOrder(@NonNull CreateFundsFrozenOrderRequest request);

    /**
     * 根据主键查询资金冻结订单。
     *
     * <p>能力范围：只读查询冻结单，不查询账本冻结余额。</p>
     *
     * @param id 主键
     * @return 资金冻结订单
     */
    @NonNull FundsFrozenOrderDTO getFundsFrozenOrderById(@NonNull Long id);

    /**
     * 分页查询资金冻结订单。
     *
     * <p>能力范围：只读分页查询业务冻结单记录。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 冻结单分页结果
     */
    @NonNull WindPagination<FundsFrozenOrderDTO> queryFundsFrozenOrders(
            @NonNull FundsFrozenOrderQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
