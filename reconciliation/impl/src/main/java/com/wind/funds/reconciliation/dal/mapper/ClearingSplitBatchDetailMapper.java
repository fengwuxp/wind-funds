package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitBatchDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 清分批次成员 Mapper。
 */
@Mapper
public interface ClearingSplitBatchDetailMapper extends BaseMapper<ClearingSplitBatchDetail> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_batch_sn, splittable_detail_sn,
                   active_splittable_detail_sn, created_by FROM t_clearing_split_batch_detail
            WHERE tenant_id = #{tenantId} AND split_batch_sn = #{splitBatchSn}
            ORDER BY splittable_detail_sn
            """)
    List<ClearingSplitBatchDetail> selectByBatchSn(@Param("tenantId") Long tenantId,
                                                   @Param("splitBatchSn") String splitBatchSn);

    @Select("""
            <script>
            SELECT COUNT(*) FROM t_clearing_split_batch_detail
            WHERE tenant_id = #{tenantId}
              AND active_splittable_detail_sn IN
              <foreach collection="splittableDetailSns" item="sn" open="(" separator="," close=")">
                  #{sn}
              </foreach>
            </script>
            """)
    int countActiveMemberships(@Param("tenantId") Long tenantId,
                               @Param("splittableDetailSns") List<String> splittableDetailSns);

    @Update("""
            UPDATE t_clearing_split_batch_detail
            SET active_splittable_detail_sn = NULL
            WHERE tenant_id = #{tenantId} AND split_batch_sn = #{splitBatchSn}
            """)
    int releaseActiveMembership(@Param("tenantId") Long tenantId,
                                @Param("splitBatchSn") String splitBatchSn);
}
