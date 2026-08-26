package com.wind.funds.transaction.dal.mapper;

import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

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
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, transaction_mode, transaction_type, business_scene,
                   business_sn, external_source_code, external_funds_fact_sn, external_funds_effect_type,
                   external_funds_fact_digest, reference_transaction_sn, state, amount, currency,
                   authorized_amount, reversed_amount, completed_amount, refunded_amount, declined_amount,
                   fee_amount, route_snapshot, description, context_variables, version
            FROM t_funds_transaction
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            FOR UPDATE
            """)
    FundsTransaction selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, transaction_mode, transaction_type, business_scene,
                   business_sn, external_source_code, external_funds_fact_sn, external_funds_effect_type,
                   external_funds_fact_digest, reference_transaction_sn, state, amount, currency,
                   authorized_amount, reversed_amount, completed_amount, refunded_amount, declined_amount,
                   fee_amount, route_snapshot, description, context_variables, version
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

    @Select("""
            <script>
            SELECT COALESCE(MAX(t.id), 0)
            FROM t_funds_transaction t
            WHERE t.tenant_id = #{tenantId}
              <if test="sourceSn != null and sourceSn != ''">AND t.sn = #{sourceSn}</if>
              <if test="startTime != null">AND t.gmt_create &gt;= #{startTime}</if>
              <if test="endTime != null">AND t.gmt_create &lt; #{endTime}</if>
              <if test="ownerType != null and ownerType != '' and ownerId != null and ownerId != ''">
              AND EXISTS (
                  SELECT 1 FROM t_funds_transaction_detail d
                  WHERE d.tenant_id = t.tenant_id AND d.transaction_sn = t.sn
                    AND d.subject_type = #{ownerType} AND d.subject_id = #{ownerId}
              )
              </if>
            </script>
            """)
    long selectProjectionUpperBound(@Param("tenantId") Long tenantId,
                                    @Param("sourceSn") String sourceSn,
                                    @Param("ownerType") String ownerType,
                                    @Param("ownerId") String ownerId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT t.id, t.gmt_create, t.gmt_modified, t.sn, t.tenant_id, t.transaction_mode, t.transaction_type,
                   t.business_scene, t.business_sn, t.external_source_code, t.external_funds_fact_sn,
                   t.external_funds_effect_type, t.external_funds_fact_digest, t.reference_transaction_sn,
                   t.state, t.amount, t.currency, t.authorized_amount, t.reversed_amount, t.completed_amount,
                   t.refunded_amount, t.declined_amount, t.fee_amount, t.route_snapshot, t.description,
                   t.context_variables, t.version
            FROM t_funds_transaction t
            WHERE t.tenant_id = #{tenantId}
              AND t.id &gt; #{lastId} AND t.id &lt;= #{upperBoundId}
              <if test="sourceSn != null and sourceSn != ''">AND t.sn = #{sourceSn}</if>
              <if test="startTime != null">AND t.gmt_create &gt;= #{startTime}</if>
              <if test="endTime != null">AND t.gmt_create &lt; #{endTime}</if>
              <if test="ownerType != null and ownerType != '' and ownerId != null and ownerId != ''">
              AND EXISTS (
                  SELECT 1 FROM t_funds_transaction_detail d
                  WHERE d.tenant_id = t.tenant_id AND d.transaction_sn = t.sn
                    AND d.subject_type = #{ownerType} AND d.subject_id = #{ownerId}
              )
              </if>
            ORDER BY t.id ASC
            LIMIT #{maxBatchSize}
            </script>
            """)
    List<FundsTransaction> scanProjectionFacts(@Param("tenantId") Long tenantId,
                                               @Param("sourceSn") String sourceSn,
                                               @Param("ownerType") String ownerType,
                                               @Param("ownerId") String ownerId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("lastId") long lastId,
                                               @Param("upperBoundId") long upperBoundId,
                                               @Param("maxBatchSize") int maxBatchSize);
}
