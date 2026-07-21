package com.wind.funds.wallet.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.wallet.dal.entities.SpendControlMovement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SpendControlMovement mapper.
 *
 * @author Codex
 * @date 2026-06-20
 */
@Mapper
public interface SpendControlMovementMapper extends BaseMapper<SpendControlMovement> {

    /**
     * Reads the idempotency winner with a shared current-read lock on MySQL.
     * H2 ignores the MySQL executable comment and uses its READ_COMMITTED test semantics.
     */
    @Select("""
            SELECT *
            FROM t_spend_control_movement
            WHERE tenant_id = #{tenantId}
              AND movement_sn = #{movementSn}
            /*! LOCK IN SHARE MODE */
            """)
    SpendControlMovement selectByMovementSnWithSharedLock(@Param("tenantId") Long tenantId,
                                                          @Param("movementSn") String movementSn);
}
