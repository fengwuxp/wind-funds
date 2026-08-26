package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingBatchDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 清算批次明细 Mapper。
 */
@Mapper
public interface ClearingBatchDetailMapper extends BaseMapper<ClearingBatchDetail> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, clearing_batch_sn, candidate_sn, split_batch_sn,
                   splittable_detail_sn, funds_transaction_detail_sn, ledger_entry_sn, amount, currency, created_by
            FROM t_clearing_batch_detail
            WHERE tenant_id = #{tenantId} AND clearing_batch_sn = #{clearingBatchSn}
            ORDER BY candidate_sn
            """)
    List<ClearingBatchDetail> selectByBatchSn(@Param("tenantId") Long tenantId,
                                              @Param("clearingBatchSn") String clearingBatchSn);

    @Delete("""
            DELETE FROM t_clearing_batch_detail
            WHERE tenant_id = #{tenantId} AND clearing_batch_sn = #{clearingBatchSn}
            """)
    int deleteByBatchSn(@Param("tenantId") Long tenantId,
                        @Param("clearingBatchSn") String clearingBatchSn);
}
