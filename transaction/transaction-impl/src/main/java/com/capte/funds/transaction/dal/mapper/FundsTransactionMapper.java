package com.capte.funds.transaction.dal.mapper;

import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * FundsTransaction mapper.
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsTransactionMapper extends BaseMapper<FundsTransaction> {
}
