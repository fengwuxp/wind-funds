-- wind-funds governance forward migration: append-only projection recovery assets.

CREATE TABLE `t_projection_replay_task`
(
    `id`                       BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                       VARCHAR(64)  NOT NULL COMMENT '重放任务号',
    `tenant_id`                BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `request_sn`               VARCHAR(64)  NOT NULL COMMENT '请求幂等流水',
    `request_digest`           VARCHAR(128) NOT NULL COMMENT '请求摘要',
    `view_domain`              VARCHAR(50)  NOT NULL COMMENT '视图域',
    `replay_mode`              VARCHAR(50)  NOT NULL COMMENT '重放模式',
    `source_sn`                VARCHAR(64)           DEFAULT NULL COMMENT '单笔来源流水',
    `owner_type`               VARCHAR(50)           DEFAULT NULL COMMENT '主体类型',
    `owner_id`                 VARCHAR(64)           DEFAULT NULL COMMENT '主体 ID',
    `range_start_time`         DATETIME              DEFAULT NULL COMMENT '范围开始时间',
    `range_end_time`           DATETIME              DEFAULT NULL COMMENT '范围结束时间',
    `batch_type`               VARCHAR(50)           DEFAULT NULL COMMENT '批次类型',
    `batch_sn`                 VARCHAR(64)           DEFAULT NULL COMMENT '批次流水',
    `checkpoint_type`          VARCHAR(50)  NOT NULL COMMENT '检查点类型',
    `checkpoint_value`         VARCHAR(128) NOT NULL COMMENT '稳定扫描游标',
    `state`                    VARCHAR(30)  NOT NULL COMMENT '任务状态',
    `success_count`            BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '成功数量',
    `failed_count`             BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '失败数量',
    `skipped_count`            BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '跳过数量',
    `difference_count`         BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '差异数量',
    `replay_reason`            VARCHAR(256) NOT NULL COMMENT '重放原因',
    `audit_ref`                VARCHAR(128) NOT NULL COMMENT '审计引用',
    `approval_ref`             VARCHAR(128)          DEFAULT NULL COMMENT '审批引用',
    `validated_shadow_task_sn` VARCHAR(64)           DEFAULT NULL COMMENT '已验证影子任务号',
    `operator_id`              VARCHAR(128) NOT NULL COMMENT '稳定操作者身份',
    `version`                  INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_projection_replay_task_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_projection_replay_task_request` (`tenant_id`, `request_sn`),
    KEY `idx_projection_replay_task_backlog` (`tenant_id`, `state`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '投影重放任务表';

CREATE TABLE `t_projection_replay_difference`
(
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`      BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `task_sn`        VARCHAR(64)  NOT NULL COMMENT '重放任务号',
    `source_sn`      VARCHAR(64)  NOT NULL COMMENT '来源流水',
    `field_name`     VARCHAR(64)  NOT NULL COMMENT '差异字段',
    `expected_value` VARCHAR(128) NOT NULL COMMENT '期望值摘要',
    `actual_value`   VARCHAR(128) NOT NULL COMMENT '实际值摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_projection_replay_difference` (`tenant_id`, `task_sn`, `source_sn`, `field_name`),
    KEY `idx_projection_replay_difference_task` (`tenant_id`, `task_sn`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '投影重放差异证据表';

CREATE TABLE `t_funds_transaction_projection`
(
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`        BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `view_domain`      VARCHAR(50)  NOT NULL COMMENT '视图域',
    `projection_scope` VARCHAR(20)  NOT NULL COMMENT 'OFFICIAL/SHADOW',
    `scope_ref`        VARCHAR(64)  NOT NULL COMMENT '正式常量或影子任务号',
    `projection_sn`    VARCHAR(128) NOT NULL COMMENT '投影流水号',
    `owner_type`       VARCHAR(50)  NOT NULL COMMENT '主体类型',
    `owner_id`         VARCHAR(64)  NOT NULL COMMENT '主体 ID',
    `source_sn`        VARCHAR(64)  NOT NULL COMMENT '来源流水',
    `display_type`     VARCHAR(50)  NOT NULL COMMENT '展示类型',
    `display_status`   VARCHAR(50)  NOT NULL COMMENT '展示状态',
    `amount`           BIGINT(20)   NOT NULL COMMENT '金额',
    `currency`         VARCHAR(10)  NOT NULL COMMENT '币种',
    `occurred_time`    DATETIME     NOT NULL COMMENT '发生时间',
    `payload_json`     TEXT         NOT NULL COMMENT '解释载荷',
    `replay_task_sn`   VARCHAR(64)  NOT NULL COMMENT '最近重放任务号',
    `version`          INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_funds_transaction_projection` (`tenant_id`, `view_domain`, `projection_scope`, `scope_ref`, `projection_sn`),
    KEY `idx_funds_transaction_projection_owner` (`tenant_id`, `view_domain`, `owner_type`, `owner_id`, `occurred_time`),
    KEY `idx_funds_transaction_projection_source` (`tenant_id`, `source_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '资金交易只读投影表';
