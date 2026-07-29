package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 可清分明细 Mapper。
 */
@Mapper
public interface ClearingSplittableDetailMapper extends BaseMapper<ClearingSplittableDetail> {

    /**
     * 按来源账本分录查询唯一准入结果。
     */
    @Select("""
            SELECT *
            FROM t_clearing_splittable_detail
            WHERE tenant_id = #{tenantId}
              AND ledger_entry_sn = #{ledgerEntrySn}
            """)
    ClearingSplittableDetail selectByLedgerEntrySn(@Param("tenantId") Long tenantId,
                                                   @Param("ledgerEntrySn") String ledgerEntrySn);

    @Select("""
            <script>
            SELECT * FROM t_clearing_splittable_detail
            WHERE tenant_id = #{tenantId}
              AND sn IN
              <foreach collection="sns" item="sn" open="(" separator="," close=")">
                #{sn}
              </foreach>
            ORDER BY sn
            </script>
            """)
    List<ClearingSplittableDetail> selectBySns(@Param("tenantId") Long tenantId,
                                               @Param("sns") List<String> sns);
}
