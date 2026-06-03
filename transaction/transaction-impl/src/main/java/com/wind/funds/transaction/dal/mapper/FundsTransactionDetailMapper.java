package com.wind.funds.transaction.dal.mapper;

import com.wind.funds.transaction.dal.entities.FundsTransactionDetail;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金交易明细事实 Mapper。
 *
 * <p>职责：提供资金交易主体明细表的基础持久化能力。</p>
 *
 * <p>边界：Mapper 只承载数据访问，不推导交易效果、不归纳主交易金额、不写账本或余额投影。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsTransactionDetailMapper extends BaseMapper<FundsTransactionDetail> {
}
