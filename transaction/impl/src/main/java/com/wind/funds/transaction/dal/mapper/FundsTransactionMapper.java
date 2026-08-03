package com.wind.funds.transaction.dal.mapper;

import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资金交易主事实 Mapper。
 *
 * <p>职责：提供资金交易主表的基础持久化能力。</p>
 *
 * <p>边界：Mapper 只承载数据访问，不承载交易状态流转、路由解析、账本入账或余额投影逻辑。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsTransactionMapper extends BaseMapper<FundsTransaction> {

    @Select("""
            SELECT *
            FROM t_funds_transaction
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            FOR UPDATE
            """)
    FundsTransaction selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT *
            FROM t_funds_transaction
            WHERE tenant_id = #{tenantId}
              AND external_source_code = #{externalSourceCode}
              AND external_funds_fact_sn = #{externalFundsFactSn}
              AND external_funds_effect_type = #{externalFundsEffectType}
            FOR UPDATE
            """)
    FundsTransaction selectByExternalFundsFactForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("externalSourceCode") String externalSourceCode,
            @Param("externalFundsFactSn") String externalFundsFactSn,
            @Param("externalFundsEffectType") FundsEffectType externalFundsEffectType);
}
