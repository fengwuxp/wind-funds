package com.wind.funds.governance.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投影重放字段差异证据。
 */
@Data
@Table(ProjectionReplayDifference.TABLE_NAME)
public class ProjectionReplayDifference implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_projection_replay_difference";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @Column(tenantId = true)
    private Long tenantId;

    private String taskSn;

    private String sourceSn;

    private String fieldName;

    private String expectedValue;

    private String actualValue;
}
