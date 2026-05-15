package com.capte.funds.wallet.dal.mapper;

import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 真实资金账户 Mapper。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundingAccountMapper extends BaseMapper<FundingAccount> {
}
