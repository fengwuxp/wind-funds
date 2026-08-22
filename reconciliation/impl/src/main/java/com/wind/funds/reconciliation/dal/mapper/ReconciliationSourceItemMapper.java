package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对账来源快照成员 Mapper。
 */
@Mapper
public interface ReconciliationSourceItemMapper extends BaseMapper<ReconciliationSourceItem> {

    @Select("""
            SELECT *
            FROM t_reconciliation_source_item
            WHERE tenant_id = #{tenantId}
              AND source_snapshot_sn = #{sourceSnapshotSn}
            ORDER BY source_fact_owner_namespace, source_fact_identity_value
            """)
    List<ReconciliationSourceItem> selectBySnapshot(@Param("tenantId") Long tenantId,
                                                    @Param("sourceSnapshotSn") String sourceSnapshotSn);
}
