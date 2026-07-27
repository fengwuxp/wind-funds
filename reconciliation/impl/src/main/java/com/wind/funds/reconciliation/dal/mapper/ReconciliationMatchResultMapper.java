package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对账匹配结果 Mapper。
 */
@Mapper
public interface ReconciliationMatchResultMapper extends BaseMapper<ReconciliationMatchResult> {

    @Select("""
            SELECT *
            FROM t_reconciliation_match_result
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationMatchResult selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    /**
     * 按运行结果和来源对身份查询匹配结果。
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_match_result
            WHERE tenant_id = #{tenantId}
              AND reconciliation_run_result_sn = #{reconciliationRunResultSn}
              AND match_identity_digest = #{matchIdentityDigest}
            """)
    ReconciliationMatchResult selectByRunResultAndIdentity(
            @Param("tenantId") Long tenantId,
            @Param("reconciliationRunResultSn") String reconciliationRunResultSn,
            @Param("matchIdentityDigest") String matchIdentityDigest);
}
