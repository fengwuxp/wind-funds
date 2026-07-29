package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitResultSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 清分结果快照 Mapper。
 */
@Mapper
public interface ClearingSplitResultSnapshotMapper extends BaseMapper<ClearingSplitResultSnapshot> {

    @Select("""
            SELECT * FROM t_clearing_split_result_snapshot
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingSplitResultSnapshot selectBySn(@Param("tenantId") Long tenantId,
                                           @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_split_result_snapshot
            WHERE tenant_id = #{tenantId} AND split_batch_sn = #{splitBatchSn}
            ORDER BY splittable_detail_sn
            """)
    List<ClearingSplitResultSnapshot> selectByBatchSn(@Param("tenantId") Long tenantId,
                                                      @Param("splitBatchSn") String splitBatchSn);
}
