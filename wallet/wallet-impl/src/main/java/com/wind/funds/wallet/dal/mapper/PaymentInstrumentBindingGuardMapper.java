package com.wind.funds.wallet.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.wallet.dal.entities.PaymentInstrumentBindingGuard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付工具绑定并发保护 Mapper。
 *
 * @author Codex
 * @date 2026-06-25
 */
@Mapper
public interface PaymentInstrumentBindingGuardMapper extends BaseMapper<PaymentInstrumentBindingGuard> {

    @Select("""
            SELECT id
            FROM t_payment_instrument_binding_guard
            WHERE tenant_id = #{tenantId}
              AND instrument_sn = #{instrumentSn}
              AND binding_role = #{bindingRole}
              AND currency = #{currency}
              AND guard_type = #{guardType}
            FOR UPDATE
            """)
    Long selectGuardIdForUpdate(@Param("tenantId") Long tenantId,
                                @Param("instrumentSn") String instrumentSn,
                                @Param("bindingRole") String bindingRole,
                                @Param("currency") String currency,
                                @Param("guardType") String guardType);
}
