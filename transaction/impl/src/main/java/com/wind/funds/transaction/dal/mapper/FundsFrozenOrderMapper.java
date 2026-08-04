package com.wind.funds.transaction.dal.mapper;

import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金冻结单 Mapper。
 *
 * <p>职责：提供业务冻结单记录的基础持久化能力。</p>
 *
 * <p>边界：Mapper 只承载数据访问，不表达账本冻结事实、不执行冻结/解冻资金控制、不推进冻结单生命周期。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Mapper
public interface FundsFrozenOrderMapper extends BaseMapper<FundsFrozenOrder> {

    @Select("""
            <script>
            SELECT COALESCE(MAX(id), 0)
            FROM t_funds_frozen_order
            WHERE tenant_id = #{tenantId}
              <if test="sourceSn != null and sourceSn != ''">AND sn = #{sourceSn}</if>
              <if test="ownerType != null and ownerType != ''">AND subject_type = #{ownerType}</if>
              <if test="ownerId != null and ownerId != ''">AND subject_id = #{ownerId}</if>
              <if test="startTime != null">AND gmt_create &gt;= #{startTime}</if>
              <if test="endTime != null">AND gmt_create &lt; #{endTime}</if>
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
            SELECT *
            FROM t_funds_frozen_order
            WHERE tenant_id = #{tenantId}
              AND id &gt; #{lastId} AND id &lt;= #{upperBoundId}
              <if test="sourceSn != null and sourceSn != ''">AND sn = #{sourceSn}</if>
              <if test="ownerType != null and ownerType != ''">AND subject_type = #{ownerType}</if>
              <if test="ownerId != null and ownerId != ''">AND subject_id = #{ownerId}</if>
              <if test="startTime != null">AND gmt_create &gt;= #{startTime}</if>
              <if test="endTime != null">AND gmt_create &lt; #{endTime}</if>
            ORDER BY id ASC
            LIMIT #{maxBatchSize}
            </script>
            """)
    List<FundsFrozenOrder> scanProjectionFacts(@Param("tenantId") Long tenantId,
                                               @Param("sourceSn") String sourceSn,
                                               @Param("ownerType") String ownerType,
                                               @Param("ownerId") String ownerId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("lastId") long lastId,
                                               @Param("upperBoundId") long upperBoundId,
                                               @Param("maxBatchSize") int maxBatchSize);
}
