package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.RecoveryOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecoveryOrderMapper extends BaseMapper<RecoveryOrder> {

    @Select("""
            SELECT * FROM t_recovery_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    RecoveryOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_recovery_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    RecoveryOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_recovery_order
            WHERE tenant_id = #{tenantId}
              AND source_type = #{sourceType}
              AND source_sn = #{sourceSn}
              AND responsible_subject_type = #{responsibleSubjectType}
              AND responsible_subject_id = #{responsibleSubjectId}
              AND currency = #{currency}
            """)
    RecoveryOrder selectBySource(@Param("tenantId") Long tenantId,
                                 @Param("sourceType") String sourceType,
                                 @Param("sourceSn") String sourceSn,
                                 @Param("responsibleSubjectType") String responsibleSubjectType,
                                 @Param("responsibleSubjectId") String responsibleSubjectId,
                                 @Param("currency") String currency);

    @Select("""
            SELECT * FROM t_recovery_order
            WHERE tenant_id = #{tenantId}
              AND source_type = #{sourceType}
              AND source_sn = #{sourceSn}
              AND responsible_subject_type = #{responsibleSubjectType}
              AND responsible_subject_id = #{responsibleSubjectId}
              AND currency = #{currency}
            FOR UPDATE
            """)
    RecoveryOrder selectBySourceForUpdate(@Param("tenantId") Long tenantId,
                                          @Param("sourceType") String sourceType,
                                          @Param("sourceSn") String sourceSn,
                                          @Param("responsibleSubjectType") String responsibleSubjectType,
                                          @Param("responsibleSubjectId") String responsibleSubjectId,
                                          @Param("currency") String currency);
}
