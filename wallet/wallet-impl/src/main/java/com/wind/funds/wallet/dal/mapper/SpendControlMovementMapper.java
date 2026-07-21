package com.wind.funds.wallet.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.dal.entities.SpendControlMovement;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * Reads the current budget projection facts while the target account row is locked.
     */
    @Select("""
            SELECT *
            FROM t_spend_control_movement
            WHERE tenant_id = #{tenantId}
              AND currency = #{currency}
              AND spend_rule_id = #{spendRuleId}
              AND spend_rule_version = #{spendRuleVersion}
              AND control_scope_id = #{controlScopeId}
              AND period_id = #{periodId}
              AND target_subject_id = #{targetSubjectId}
              AND target_subject_type = #{targetSubjectType}
            ORDER BY id ASC
            LIMIT #{limit}
            /*! LOCK IN SHARE MODE */
            """)
    List<SpendControlMovement> selectBudgetProjectionMovementsWithSharedLock(
            @Param("tenantId") Long tenantId,
            @Param("currency") CurrencyIsoCode currency,
            @Param("spendRuleId") String spendRuleId,
            @Param("spendRuleVersion") String spendRuleVersion,
            @Param("controlScopeId") String controlScopeId,
            @Param("periodId") String periodId,
            @Param("targetSubjectId") String targetSubjectId,
            @Param("targetSubjectType") FundsSubjectType targetSubjectType,
            @Param("limit") int limit);
}
