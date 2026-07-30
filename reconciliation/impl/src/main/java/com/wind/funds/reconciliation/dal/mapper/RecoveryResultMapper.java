package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.RecoveryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecoveryResultMapper extends BaseMapper<RecoveryResult> {

    @Select("""
            SELECT * FROM t_recovery_result
            WHERE tenant_id = #{tenantId} AND idempotency_key = #{idempotencyKey}
            """)
    RecoveryResult selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                          @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT * FROM t_recovery_result
            WHERE tenant_id = #{tenantId} AND funds_transaction_sn = #{fundsTransactionSn}
            """)
    RecoveryResult selectByFundsTransactionSn(@Param("tenantId") Long tenantId,
                                              @Param("fundsTransactionSn") String fundsTransactionSn);

    @Select("""
            SELECT * FROM t_recovery_result
            WHERE tenant_id = #{tenantId} AND idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    RecoveryResult selectByIdempotencyKeyForUpdate(@Param("tenantId") Long tenantId,
                                                   @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT * FROM t_recovery_result
            WHERE tenant_id = #{tenantId} AND funds_transaction_sn = #{fundsTransactionSn}
            FOR UPDATE
            """)
    RecoveryResult selectByFundsTransactionSnForUpdate(@Param("tenantId") Long tenantId,
                                                       @Param("fundsTransactionSn") String fundsTransactionSn);
}
