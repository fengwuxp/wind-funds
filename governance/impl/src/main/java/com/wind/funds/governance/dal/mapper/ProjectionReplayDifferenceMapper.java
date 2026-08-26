package com.wind.funds.governance.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.governance.dal.entities.ProjectionReplayDifference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 投影重放差异 Mapper。
 */
@Mapper
public interface ProjectionReplayDifferenceMapper extends BaseMapper<ProjectionReplayDifference> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, tenant_id, task_sn, source_sn, field_name, expected_value,
                   actual_value FROM t_projection_replay_difference
            WHERE tenant_id = #{tenantId} AND task_sn = #{taskSn}
              AND source_sn = #{sourceSn} AND field_name = #{fieldName}
            """)
    ProjectionReplayDifference selectDifference(@Param("tenantId") Long tenantId,
                                                @Param("taskSn") String taskSn,
                                                @Param("sourceSn") String sourceSn,
                                                @Param("fieldName") String fieldName);
}
