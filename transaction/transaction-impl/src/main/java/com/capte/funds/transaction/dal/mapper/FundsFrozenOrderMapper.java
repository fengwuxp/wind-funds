package com.capte.funds.transaction.dal.mapper;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金冻结单 Mapper。
 *
 * <p>职责：提供业务冻结单记录的基础持久化能力。</p>
 *
 * <p>边界：Mapper 只承载数据访问，不表达账本冻结事实、不执行冻结/解冻资金控制、不推进冻结单生命周期。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsFrozenOrderMapper extends BaseMapper<FundsFrozenOrder> {
}
