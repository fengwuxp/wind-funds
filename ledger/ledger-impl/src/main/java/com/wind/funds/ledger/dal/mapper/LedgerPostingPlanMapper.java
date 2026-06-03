package com.wind.funds.ledger.dal.mapper;

import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账本记账计划 Mapper。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface LedgerPostingPlanMapper extends BaseMapper<LedgerPostingPlan> {
}
