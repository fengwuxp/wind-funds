package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清算候选 Mapper。
 */
@Mapper
public interface ClearingCandidateMapper extends BaseMapper<ClearingCandidate> {

    @Select("""
            SELECT * FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingCandidate selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingCandidate selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND candidate_digest = #{candidateDigest}
            """)
    ClearingCandidate selectByDigest(@Param("tenantId") Long tenantId,
                                     @Param("candidateDigest") String candidateDigest);

    @Select("""
            SELECT * FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId}
              AND active_splittable_detail_sn = #{splittableDetailSn}
            FOR UPDATE
            """)
    ClearingCandidate selectByActiveDetailForUpdate(@Param("tenantId") Long tenantId,
                                                    @Param("splittableDetailSn") String splittableDetailSn);

    @Select("""
            <script>
            SELECT * FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId}
              AND sn IN
              <foreach collection="sns" item="sn" open="(" separator="," close=")">
                #{sn}
              </foreach>
            ORDER BY sn
            </script>
            """)
    List<ClearingCandidate> selectBySns(@Param("tenantId") Long tenantId,
                                        @Param("sns") List<String> sns);

    @Update("""
            UPDATE t_clearing_candidate
            SET status = 'LOCKED', locked_clearing_batch_sn = #{clearingBatchSn},
                updated_by = #{updatedBy}, status_changed_time = #{statusChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn} AND status = 'READY'
            """)
    int lockReadyCandidate(@Param("tenantId") Long tenantId,
                           @Param("candidateSn") String candidateSn,
                           @Param("clearingBatchSn") String clearingBatchSn,
                           @Param("updatedBy") String updatedBy,
                           @Param("statusChangedTime") LocalDateTime statusChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET status = 'READY', locked_clearing_batch_sn = NULL, block_reason = NULL,
                updated_by = #{updatedBy}, status_changed_time = #{statusChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND status = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int releaseLockedCandidate(@Param("tenantId") Long tenantId,
                               @Param("candidateSn") String candidateSn,
                               @Param("clearingBatchSn") String clearingBatchSn,
                               @Param("updatedBy") String updatedBy,
                               @Param("statusChangedTime") LocalDateTime statusChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET status = 'CLEARED', locked_clearing_batch_sn = NULL,
                updated_by = #{updatedBy}, status_changed_time = #{statusChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND status = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int markLockedCandidateCleared(@Param("tenantId") Long tenantId,
                                   @Param("candidateSn") String candidateSn,
                                   @Param("clearingBatchSn") String clearingBatchSn,
                                   @Param("updatedBy") String updatedBy,
                                   @Param("statusChangedTime") LocalDateTime statusChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET status = 'BLOCKED', locked_clearing_batch_sn = NULL, block_reason = #{blockReason},
                updated_by = #{updatedBy}, status_changed_time = #{statusChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND status = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int blockLockedCandidate(@Param("tenantId") Long tenantId,
                             @Param("candidateSn") String candidateSn,
                             @Param("clearingBatchSn") String clearingBatchSn,
                             @Param("blockReason") String blockReason,
                             @Param("updatedBy") String updatedBy,
                             @Param("statusChangedTime") LocalDateTime statusChangedTime);
}
