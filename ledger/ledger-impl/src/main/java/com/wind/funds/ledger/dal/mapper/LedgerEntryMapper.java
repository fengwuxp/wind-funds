package com.wind.funds.ledger.dal.mapper;

import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * 账本条目 Mapper
 *
 * @author wuxp
 * @since 2026-04-14
 **/
@Mapper
public interface LedgerEntryMapper extends BaseMapper<LedgerEntry> {
}