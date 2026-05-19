package com.capte.funds.transaction.dal.mapper;

import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金交易主事实 Mapper。
 *
 * <p>职责：提供资金交易主表的基础持久化能力。</p>
 *
 * <p>边界：Mapper 只承载数据访问，不承载交易状态流转、路由解析、账本入账或余额投影逻辑。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsTransactionMapper extends BaseMapper<FundsTransaction> {
}
