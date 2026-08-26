package com.wind.funds.governance.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.governance.dal.entities.ProjectionReplayTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 投影重放任务 Mapper。
 */
@Mapper
public interface ProjectionReplayTaskMapper extends BaseMapper<ProjectionReplayTask> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, request_sn, request_digest, view_domain,
                   replay_mode, source_sn, owner_type, owner_id, range_start_time, range_end_time, batch_type,
                   batch_sn, checkpoint_type, checkpoint_value, state, success_count, failed_count,
                   skipped_count, difference_count, replay_reason, audit_ref, approval_ref,
                   validated_shadow_task_sn, operator_id, version FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND request_sn = #{requestSn}
            """)
    ProjectionReplayTask selectByRequest(@Param("tenantId") Long tenantId,
                                         @Param("requestSn") String requestSn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, request_sn, request_digest, view_domain,
                   replay_mode, source_sn, owner_type, owner_id, range_start_time, range_end_time, batch_type,
                   batch_sn, checkpoint_type, checkpoint_value, state, success_count, failed_count,
                   skipped_count, difference_count, replay_reason, audit_ref, approval_ref,
                   validated_shadow_task_sn, operator_id, version FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND sn = #{taskSn}
            """)
    ProjectionReplayTask selectBySn(@Param("tenantId") Long tenantId,
                                    @Param("taskSn") String taskSn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, request_sn, request_digest, view_domain,
                   replay_mode, source_sn, owner_type, owner_id, range_start_time, range_end_time, batch_type,
                   batch_sn, checkpoint_type, checkpoint_value, state, success_count, failed_count,
                   skipped_count, difference_count, replay_reason, audit_ref, approval_ref,
                   validated_shadow_task_sn, operator_id, version FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND sn = #{taskSn}
            FOR UPDATE
            """)
    ProjectionReplayTask selectBySnForUpdate(@Param("tenantId") Long tenantId,
                                             @Param("taskSn") String taskSn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, request_sn, request_digest, view_domain,
                   replay_mode, source_sn, owner_type, owner_id, range_start_time, range_end_time, batch_type,
                   batch_sn, checkpoint_type, checkpoint_value, state, success_count, failed_count,
                   skipped_count, difference_count, replay_reason, audit_ref, approval_ref,
                   validated_shadow_task_sn, operator_id, version FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND state IN ('CREATED', 'RUNNING')
            ORDER BY id ASC
            LIMIT #{maxSize}
            """)
    List<ProjectionReplayTask> selectBacklog(@Param("tenantId") Long tenantId,
                                            @Param("maxSize") int maxSize);
}
