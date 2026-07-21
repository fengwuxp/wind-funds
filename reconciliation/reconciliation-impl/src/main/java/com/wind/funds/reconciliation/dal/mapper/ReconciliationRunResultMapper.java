package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对账运行结果 Mapper。
 */
@Mapper
public interface ReconciliationRunResultMapper extends BaseMapper<ReconciliationRunResult> {

    /**
     * 按流水号查询运行结果。
     *
     * @param tenantId 租户 ID
     * @param sn 运行结果流水号
     * @return 运行结果；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_run_result
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationRunResult selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    /**
     * 按不可变业务键查询运行结果。
     *
     * @param tenantId 租户 ID
     * @param reconciliationBatchSn 对账批次流水号
     * @param gateObjectType 准入对象类型
     * @param gateObjectSn 准入对象流水号
     * @return 运行结果；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_run_result
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
              AND gate_object_type = #{gateObjectType}
              AND gate_object_sn = #{gateObjectSn}
            """)
    ReconciliationRunResult selectByBusinessKey(@Param("tenantId") Long tenantId,
                                                @Param("reconciliationBatchSn") String reconciliationBatchSn,
                                                @Param("gateObjectType") String gateObjectType,
                                                @Param("gateObjectSn") String gateObjectSn);

    /**
     * 唯一键竞争后按不可变业务键读取已提交的胜出结果。
     *
     * @param tenantId 租户 ID
     * @param reconciliationBatchSn 对账批次流水号
     * @param gateObjectType 准入对象类型
     * @param gateObjectSn 准入对象流水号
     * @return 运行结果；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_run_result
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
              AND gate_object_type = #{gateObjectType}
              AND gate_object_sn = #{gateObjectSn}
            FOR UPDATE
            """)
    ReconciliationRunResult selectByBusinessKeyForUpdate(@Param("tenantId") Long tenantId,
                                                         @Param("reconciliationBatchSn") String reconciliationBatchSn,
                                                         @Param("gateObjectType") String gateObjectType,
                                                         @Param("gateObjectSn") String gateObjectSn);
}
