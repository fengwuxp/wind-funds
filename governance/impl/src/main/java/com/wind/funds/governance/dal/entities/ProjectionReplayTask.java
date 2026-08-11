package com.wind.funds.governance.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.governance.enums.ProjectionCheckpointType;
import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.funds.governance.enums.ProjectionReplayTaskState;
import com.wind.integration.core.model.TenantIsolationObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 持久交易投影重放任务。
 */
@Data
@Table(ProjectionReplayTask.TABLE_NAME)
public class ProjectionReplayTask implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_projection_replay_task";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    private String requestSn;

    private String requestDigest;

    private String viewDomain;

    private ProjectionReplayMode replayMode;

    private String sourceSn;

    private String ownerType;

    private String ownerId;

    private LocalDateTime rangeStartTime;

    private LocalDateTime rangeEndTime;

    private String batchType;

    private String batchSn;

    private ProjectionCheckpointType checkpointType;

    private String checkpointValue;

    @Column("status")

    private ProjectionReplayTaskState state;

    void setStatus(ProjectionReplayTaskState state) {
        this.state = state;
    }

    private Long successCount;

    private Long failedCount;

    private Long skippedCount;

    private Long differenceCount;

    private String replayReason;

    private String auditRef;

    private String approvalRef;

    private String validatedShadowTaskSn;

    private String operatorId;

    @Column(version = true)
    private Integer version;
}
