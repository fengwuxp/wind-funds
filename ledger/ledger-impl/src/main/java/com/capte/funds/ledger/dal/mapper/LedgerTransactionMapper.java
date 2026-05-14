package com.capte.funds.ledger.dal.mapper;

import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* 账户账本交易 Mapper
*
* @author wuxp
* @since 2026-04-14
**/
@Mapper
public interface LedgerTransactionMapper extends BaseMapper<LedgerTransaction> {
}