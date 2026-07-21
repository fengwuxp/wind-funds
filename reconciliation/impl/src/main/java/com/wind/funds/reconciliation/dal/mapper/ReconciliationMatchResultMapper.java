package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账匹配结果 Mapper。
 */
@Mapper
public interface ReconciliationMatchResultMapper extends BaseMapper<ReconciliationMatchResult> {
}
