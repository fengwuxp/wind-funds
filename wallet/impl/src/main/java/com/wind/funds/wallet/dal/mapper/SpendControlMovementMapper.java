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
 * SpendControlMovement 持久化 Mapper。
 *
 */
@Mapper
public interface SpendControlMovementMapper extends BaseMapper<SpendControlMovement> {

    /**
     * 在 MySQL 上通过共享当前读锁读取幂等竞争胜出记录。
     * H2 会忽略 MySQL 可执行注释，并使用自身的 READ_COMMITTED 测试语义。
     */
    @Select("""
            SELECT id, gmt_create, gmt_modified, movement_sn, tenant_id, movement_type, business_scene, business_sn,
                   original_movement_sn, transaction_sn, instrument_sn, action, target_subject_id,
                   target_subject_type, amount, currency, spend_rule_id, spend_rule_version, spend_decision_sn,
                   spend_decision_result, spend_decision_digest, control_scope_id, period_id, reject_reason,
                   reason_code, operator_id, audit_reference_sn, movement_digest, description, context_variables
            FROM t_spend_control_movement
            WHERE tenant_id = #{tenantId}
              AND movement_sn = #{movementSn}
            /*! LOCK IN SHARE MODE */
            """)
    SpendControlMovement selectByMovementSnWithSharedLock(@Param("tenantId") Long tenantId,
                                                          @Param("movementSn") String movementSn);

    /**
     * 在目标账户行已加锁时读取当前预算投影事实。
     */
    @Select("""
            SELECT id, gmt_create, gmt_modified, movement_sn, tenant_id, movement_type, business_scene, business_sn,
                   original_movement_sn, transaction_sn, instrument_sn, action, target_subject_id,
                   target_subject_type, amount, currency, spend_rule_id, spend_rule_version, spend_decision_sn,
                   spend_decision_result, spend_decision_digest, control_scope_id, period_id, reject_reason,
                   reason_code, operator_id, audit_reference_sn, movement_digest, description, context_variables
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
