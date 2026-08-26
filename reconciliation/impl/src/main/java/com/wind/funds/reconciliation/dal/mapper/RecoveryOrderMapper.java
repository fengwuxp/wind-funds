package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.RecoveryOrder;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecoveryOrderMapper extends BaseMapper<RecoveryOrder> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, source_type, source_sn, responsible_subject_type,
                   responsible_subject_id, expected_amount, recovered_amount, currency, state, source_digest,
                   order_digest, approval_ref, evidence_ref, last_funds_transaction_sn, created_by, recovered_time,
                   version FROM t_recovery_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    RecoveryOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, source_type, source_sn, responsible_subject_type,
                   responsible_subject_id, expected_amount, recovered_amount, currency, state, source_digest,
                   order_digest, approval_ref, evidence_ref, last_funds_transaction_sn, created_by, recovered_time,
                   version FROM t_recovery_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    RecoveryOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, source_type, source_sn, responsible_subject_type,
                   responsible_subject_id, expected_amount, recovered_amount, currency, state, source_digest,
                   order_digest, approval_ref, evidence_ref, last_funds_transaction_sn, created_by, recovered_time,
                   version FROM t_recovery_order
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
                                 @Param("currency") CurrencyIsoCode currency);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, source_type, source_sn, responsible_subject_type,
                   responsible_subject_id, expected_amount, recovered_amount, currency, state, source_digest,
                   order_digest, approval_ref, evidence_ref, last_funds_transaction_sn, created_by, recovered_time,
                   version FROM t_recovery_order
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
                                          @Param("currency") CurrencyIsoCode currency);
}
