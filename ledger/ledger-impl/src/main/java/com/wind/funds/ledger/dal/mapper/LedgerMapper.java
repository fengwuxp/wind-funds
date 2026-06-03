package com.wind.funds.ledger.dal.mapper;

import com.wind.funds.ledger.dal.entities.Ledger;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* 账户账本 Mapper
*
* @author wuxp
* @since 2026-04-14
**/
@Mapper
public interface LedgerMapper extends BaseMapper<Ledger> {
}