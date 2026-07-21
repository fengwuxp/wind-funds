package com.wind.funds.wallet.dal.mapper;

import com.wind.funds.wallet.dal.entities.CreditAccount;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 信用账户 Mapper。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface CreditAccountMapper extends BaseMapper<CreditAccount> {

    @Update("""
            UPDATE t_credit_account
            SET version = version + 1
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND version = #{expectedVersion}
            """)
    int incrementVersionIfMatch(@Param("tenantId") Long tenantId,
                                @Param("sn") String sn,
                                @Param("expectedVersion") Integer expectedVersion);

    @Select("""
            SELECT version
            FROM t_credit_account
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            FOR UPDATE
            """)
    Integer selectVersionBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
