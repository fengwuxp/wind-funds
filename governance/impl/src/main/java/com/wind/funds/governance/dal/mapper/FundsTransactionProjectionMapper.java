package com.wind.funds.governance.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.governance.dal.entities.FundsTransactionProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 交易只读投影 Mapper。
 */
@Mapper
public interface FundsTransactionProjectionMapper extends BaseMapper<FundsTransactionProjection> {

    @Select("""
            SELECT * FROM t_funds_transaction_projection
            WHERE tenant_id = #{tenantId} AND view_domain = #{viewDomain}
              AND projection_scope = #{projectionScope} AND scope_ref = #{scopeRef}
              AND projection_sn = #{projectionSn}
            """)
    FundsTransactionProjection selectProjection(@Param("tenantId") Long tenantId,
                                                @Param("viewDomain") String viewDomain,
                                                @Param("projectionScope") String projectionScope,
                                                @Param("scopeRef") String scopeRef,
                                                @Param("projectionSn") String projectionSn);
}
