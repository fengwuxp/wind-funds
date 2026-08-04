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
            SELECT * FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND request_sn = #{requestSn}
            """)
    ProjectionReplayTask selectByRequest(@Param("tenantId") Long tenantId,
                                         @Param("requestSn") String requestSn);

    @Select("""
            SELECT * FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND sn = #{taskSn}
            """)
    ProjectionReplayTask selectBySn(@Param("tenantId") Long tenantId,
                                    @Param("taskSn") String taskSn);

    @Select("""
            SELECT * FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND sn = #{taskSn}
            FOR UPDATE
            """)
    ProjectionReplayTask selectBySnForUpdate(@Param("tenantId") Long tenantId,
                                             @Param("taskSn") String taskSn);

    @Select("""
            SELECT * FROM t_projection_replay_task
            WHERE tenant_id = #{tenantId} AND status IN ('CREATED', 'RUNNING')
            ORDER BY id ASC
            LIMIT #{maxSize}
            """)
    List<ProjectionReplayTask> selectBacklog(@Param("tenantId") Long tenantId,
                                            @Param("maxSize") int maxSize);
}
