-- wind-funds 对账表 MySQL 基线。
-- 目标：MySQL 8.0+ / InnoDB。
-- 由宿主应用的迁移工具按版本执行；已有同名表时必须失败并先处理结构漂移。

-- 可清分明细准入事实表
-- ----------------------------
CREATE TABLE `t_clearing_splittable_detail`
(
    `id`                              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                              VARCHAR(64)  NOT NULL COMMENT '可清分明细流水号',
    `tenant_id`                       BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `funds_transaction_sn`            VARCHAR(64)  NOT NULL COMMENT '来源资金交易流水号',
    `funds_transaction_detail_sn`     VARCHAR(64)  NOT NULL COMMENT '来源资金交易明细流水号',
    `ledger_transaction_sn`           VARCHAR(64)  NOT NULL COMMENT '来源账本交易流水号',
    `posting_plan_sn`                 VARCHAR(64)  NOT NULL COMMENT '来源记账计划流水号',
    `ledger_entry_sn`                 VARCHAR(64)  NOT NULL COMMENT '来源账本分录流水号',
    `subject_type`                    VARCHAR(50)  NOT NULL COMMENT '账务主体类型',
    `subject_id`                      VARCHAR(64)  NOT NULL COMMENT '账务主体 ID',
    `currency`                        VARCHAR(10)  NOT NULL COMMENT '币种',
    `amount`                          BIGINT(20)   NOT NULL COMMENT '来源 CLEARING 入账金额，最小货币单位',
    `refund_amount`                   BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '清分前已退款金额',
    `business_line`                   VARCHAR(64)  NOT NULL COMMENT '上层清分策略确认的业务线',
    `split_period`                    VARCHAR(30)  NOT NULL COMMENT '已解析清分周期',
    `split_rule_code`                 VARCHAR(64)  NOT NULL COMMENT '清分规则编码',
    `split_rule_version`              VARCHAR(64)  NOT NULL COMMENT '清分规则版本',
    `status`                          VARCHAR(50)  NOT NULL COMMENT 'SPLIT_READY/EXCLUDED',
    `exclusion_reason`                VARCHAR(64)           DEFAULT NULL COMMENT '稳定排除原因',
    `reconciliation_decision_status` VARCHAR(50)  NOT NULL COMMENT '清分前对账门禁结论',
    `reconciliation_run_result_sn`    VARCHAR(64)  NOT NULL COMMENT '清分前对账运行结果流水号',
    `reconciliation_result_digest`    VARCHAR(64)           DEFAULT NULL COMMENT '清分前对账运行结果 SHA-256；结果缺失并阻断时为空',
    `reconciliation_evidence_refs`    MEDIUMTEXT   NOT NULL COMMENT '清分前对账证据引用 JSON',
    `route_snapshot_digest`           VARCHAR(64)           DEFAULT NULL COMMENT '来源 RouteSnapshot SHA-256；来源不完整被排除时为空',
    `source_digest`                   VARCHAR(64)  NOT NULL COMMENT '来源事实与规则快照 SHA-256',
    `created_by`                      VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_splittable_detail_sn` (`sn`),
    UNIQUE KEY `uk_clearing_splittable_detail_entry` (`tenant_id`, `ledger_entry_sn`),
    KEY `idx_clearing_splittable_detail_source` (`tenant_id`, `funds_transaction_sn`, `funds_transaction_detail_sn`),
    KEY `idx_clearing_splittable_detail_subject` (`tenant_id`, `subject_type`, `subject_id`, `split_period`),
    KEY `idx_clearing_splittable_detail_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '可清分明细准入事实表';

-- 清分批次表：一个批次只允许一个账务主体、币种、业务线、周期和规则版本
CREATE TABLE `t_clearing_split_batch`
(
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`               VARCHAR(64)  NOT NULL COMMENT '清分批次流水号',
    `tenant_id`        BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `subject_type`     VARCHAR(50)  NOT NULL COMMENT '唯一账务主体类型',
    `subject_id`       VARCHAR(64)  NOT NULL COMMENT '唯一账务主体 ID',
    `currency`         VARCHAR(10)  NOT NULL COMMENT '币种',
    `business_line`    VARCHAR(64)  NOT NULL COMMENT '业务线',
    `split_period`     VARCHAR(30)  NOT NULL COMMENT '清分周期',
    `split_rule_code`  VARCHAR(64)  NOT NULL COMMENT '清分规则编码',
    `split_rule_version` VARCHAR(64) NOT NULL COMMENT '清分规则版本',
    `detail_count`     INT(11)      NOT NULL COMMENT '成员明细数',
    `total_amount`     BIGINT(20)   NOT NULL COMMENT '批次金额汇总，最小货币单位',
    `member_digest`    VARCHAR(64)  NOT NULL COMMENT '成员来源摘要集合 SHA-256',
    `batch_digest`     VARCHAR(64)  NOT NULL COMMENT '批次边界和成员事实 SHA-256',
    `active_batch_digest` VARCHAR(64)        DEFAULT NULL COMMENT '有效批次幂等键；批次取消后置空',
    `status`           VARCHAR(50)  NOT NULL COMMENT 'DRAFT/REVIEWING/CONFIRMED/CANCELLED',
    `created_by`       VARCHAR(64)  NOT NULL COMMENT '创建人',
    `submitted_by`     VARCHAR(64)           DEFAULT NULL COMMENT '提交复核人',
    `submitted_time`   DATETIME              DEFAULT NULL COMMENT '提交复核时间',
    `confirmed_by`     VARCHAR(64)           DEFAULT NULL COMMENT '确认人',
    `confirmed_time`   DATETIME              DEFAULT NULL COMMENT '确认时间',
    `cancelled_by`     VARCHAR(64)           DEFAULT NULL COMMENT '取消人',
    `cancelled_time`   DATETIME              DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`    VARCHAR(512)          DEFAULT NULL COMMENT '取消原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_split_batch_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_split_batch_active_digest` (`tenant_id`, `active_batch_digest`),
    KEY `idx_clearing_split_batch_scope`
        (`tenant_id`, `subject_type`, `subject_id`, `currency`, `business_line`, `split_period`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '单账务主体清分批次表';

-- 清分批次成员关系表
CREATE TABLE `t_clearing_split_batch_detail`
(
    `id`                          BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                          VARCHAR(64) NOT NULL COMMENT '批次成员流水号',
    `tenant_id`                   BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `split_batch_sn`              VARCHAR(64) NOT NULL COMMENT '清分批次流水号',
    `splittable_detail_sn`        VARCHAR(64) NOT NULL COMMENT '可清分明细流水号',
    `active_splittable_detail_sn` VARCHAR(64)          DEFAULT NULL COMMENT '有效占用键；批次取消后置空',
    `created_by`                  VARCHAR(64) NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_split_batch_detail_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_split_batch_detail_member` (`tenant_id`, `split_batch_sn`, `splittable_detail_sn`),
    UNIQUE KEY `uk_clearing_split_batch_detail_active` (`tenant_id`, `active_splittable_detail_sn`),
    KEY `idx_clearing_split_batch_detail_batch` (`tenant_id`, `split_batch_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '清分批次成员关系表';

-- 不可变清分结果快照表
CREATE TABLE `t_clearing_split_result_snapshot`
(
    `id`                           BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                           VARCHAR(64) NOT NULL COMMENT '清分结果快照流水号',
    `tenant_id`                    BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `split_batch_sn`               VARCHAR(64) NOT NULL COMMENT '清分批次流水号',
    `splittable_detail_sn`         VARCHAR(64) NOT NULL COMMENT '可清分明细流水号',
    `subject_type`                 VARCHAR(50) NOT NULL COMMENT '账务主体类型',
    `subject_id`                   VARCHAR(64) NOT NULL COMMENT '账务主体 ID',
    `currency`                     VARCHAR(10) NOT NULL COMMENT '币种',
    `business_line`                VARCHAR(64) NOT NULL COMMENT '业务线',
    `split_period`                 VARCHAR(30) NOT NULL COMMENT '清分周期',
    `amount`                       BIGINT(20)  NOT NULL COMMENT '来源 CLEARING 入账金额，最小货币单位',
    `funds_transaction_sn`         VARCHAR(64) NOT NULL COMMENT '来源资金交易流水号',
    `funds_transaction_detail_sn`  VARCHAR(64) NOT NULL COMMENT '来源资金交易明细流水号',
    `ledger_transaction_sn`        VARCHAR(64) NOT NULL COMMENT '来源账本交易流水号',
    `posting_plan_sn`              VARCHAR(64) NOT NULL COMMENT '来源记账计划流水号',
    `ledger_entry_sn`              VARCHAR(64) NOT NULL COMMENT '来源账本分录流水号',
    `route_snapshot_digest`        VARCHAR(64) NOT NULL COMMENT '来源 RouteSnapshot SHA-256',
    `split_rule_code`              VARCHAR(64) NOT NULL COMMENT '清分规则编码',
    `split_rule_version`           VARCHAR(64) NOT NULL COMMENT '清分规则版本',
    `reconciliation_run_result_sn` VARCHAR(64) NOT NULL COMMENT '确认时消费的对账运行结果流水号',
    `reconciliation_result_digest` VARCHAR(64) NOT NULL COMMENT '确认时消费的对账运行结果 SHA-256',
    `reconciliation_evidence_refs` MEDIUMTEXT  NOT NULL COMMENT '确认时冻结的对账证据引用 JSON',
    `source_digest`                VARCHAR(64) NOT NULL COMMENT '可清分来源事实 SHA-256',
    `snapshot_digest`              VARCHAR(64) NOT NULL COMMENT '清分结果快照 SHA-256',
    `created_by`                   VARCHAR(64) NOT NULL COMMENT '确认人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_split_result_snapshot_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_split_result_snapshot_detail`
        (`tenant_id`, `split_batch_sn`, `splittable_detail_sn`),
    UNIQUE KEY `uk_clearing_split_result_snapshot_digest` (`tenant_id`, `snapshot_digest`),
    KEY `idx_clearing_split_result_snapshot_subject`
        (`tenant_id`, `subject_type`, `subject_id`, `currency`, `split_period`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '不可变清分结果快照表';

-- ----------------------------
-- 清算候选准入事实表
CREATE TABLE `t_clearing_candidate`
(
    `id`                              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `sn`                              VARCHAR(64)  NOT NULL COMMENT '清算候选流水号',
    `tenant_id`                       BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `split_result_sn`                 VARCHAR(64)  NOT NULL COMMENT '来源清分结果快照流水号',
    `split_batch_sn`                  VARCHAR(64)  NOT NULL COMMENT '来源清分批次流水号',
    `splittable_detail_sn`            VARCHAR(64)  NOT NULL COMMENT '来源可清分明细流水号',
    `subject_type`                    VARCHAR(50)  NOT NULL COMMENT '账务主体类型',
    `subject_id`                      VARCHAR(64)  NOT NULL COMMENT '账务主体 ID',
    `currency`                        VARCHAR(10)  NOT NULL COMMENT '币种',
    `business_line`                   VARCHAR(64)  NOT NULL COMMENT '业务线',
    `clearing_period`                 VARCHAR(30)  NOT NULL COMMENT '清算周期，不是账本周期',
    `amount`                          BIGINT(20)   NOT NULL COMMENT '当前候选可清算金额，最小货币单位',
    `funds_transaction_sn`            VARCHAR(64)  NOT NULL COMMENT '来源资金交易流水号',
    `funds_transaction_detail_sn`     VARCHAR(64)  NOT NULL COMMENT '来源资金交易明细流水号',
    `ledger_transaction_sn`           VARCHAR(64)  NOT NULL COMMENT '来源账本交易流水号',
    `posting_plan_sn`                 VARCHAR(64)  NOT NULL COMMENT '来源记账计划流水号',
    `ledger_entry_sn`                 VARCHAR(64)  NOT NULL COMMENT '来源账本分录流水号',
    `route_snapshot_digest`           VARCHAR(64)  NOT NULL COMMENT '来源 RouteSnapshot SHA-256',
    `clearing_available_time`         DATETIME     NOT NULL COMMENT '最早可进入清算批次的时间',
    `clearing_rule_code`              VARCHAR(64)  NOT NULL COMMENT '清算规则编码',
    `clearing_rule_version`           VARCHAR(64)  NOT NULL COMMENT '清算规则版本',
    `reconciliation_run_result_sn`    VARCHAR(64)  NOT NULL COMMENT '对账运行结果流水号',
    `reconciliation_result_digest`    VARCHAR(64)  NOT NULL COMMENT '对账结果 SHA-256',
    `reconciliation_evidence_refs`   MEDIUMTEXT   NOT NULL COMMENT '对账证据引用 JSON',
    `source_digest`                   VARCHAR(64)  NOT NULL COMMENT '来源事实摘要',
    `candidate_digest`                VARCHAR(64)  NOT NULL COMMENT '候选事实摘要',
    `active_splittable_detail_sn`     VARCHAR(64)           DEFAULT NULL COMMENT '有效候选占用键；排除后置空',
    `status`                          VARCHAR(50)  NOT NULL COMMENT 'WAITING_PERIOD/BLOCKED/READY/LOCKED/CLEARED/EXCLUDED',
    `block_reason`                    VARCHAR(128)          DEFAULT NULL COMMENT '当前阻断原因',
    `exclusion_reason`                VARCHAR(512)          DEFAULT NULL COMMENT '排除原因',
    `locked_clearing_batch_sn`        VARCHAR(64)           DEFAULT NULL COMMENT '锁定候选的清算批次流水号',
    `created_by`                      VARCHAR(64)  NOT NULL COMMENT '创建人',
    `updated_by`                      VARCHAR(64)           DEFAULT NULL COMMENT '最后状态操作人',
    `status_changed_time`             DATETIME              DEFAULT NULL COMMENT '状态变更时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_candidate_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_candidate_digest` (`tenant_id`, `candidate_digest`),
    UNIQUE KEY `uk_clearing_candidate_active_detail` (`tenant_id`, `active_splittable_detail_sn`),
    KEY `idx_clearing_candidate_source` (`tenant_id`, `split_result_sn`, `splittable_detail_sn`),
    KEY `idx_clearing_candidate_subject` (`tenant_id`, `subject_type`, `subject_id`, `currency`, `clearing_period`),
    KEY `idx_clearing_candidate_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '清算候选准入事实表';

-- ----------------------------
-- 清算批次表
CREATE TABLE `t_clearing_batch`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '清算批次流水号',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `subject_type`           VARCHAR(50)  NOT NULL COMMENT '账务主体类型',
    `subject_id`             VARCHAR(64)  NOT NULL COMMENT '账务主体 ID',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `business_line`          VARCHAR(64)  NOT NULL COMMENT '业务线',
    `clearing_period`        VARCHAR(30)  NOT NULL COMMENT '清算周期，不是账本周期',
    `clearing_rule_code`     VARCHAR(64)  NOT NULL COMMENT '清算规则编码',
    `clearing_rule_version`  VARCHAR(64)  NOT NULL COMMENT '清算规则版本',
    `candidate_count`        INT(11)      NOT NULL COMMENT '候选数量',
    `total_amount`           BIGINT(20)   NOT NULL COMMENT '清算总金额，最小货币单位',
    `amount_digest`          VARCHAR(64)  NOT NULL COMMENT '候选经济范围 SHA-256',
    `active_amount_digest`   VARCHAR(64)           DEFAULT NULL COMMENT 'REVIEWING 经济范围占用键',
    `funds_transaction_sn`   VARCHAR(64)           DEFAULT NULL COMMENT '成功或明确失败的资金交易流水号',
    `status`                 VARCHAR(50)  NOT NULL COMMENT 'DRAFT/REVIEWING/CONFIRMED/CANCELLED/FAILED',
    `created_by`             VARCHAR(64)  NOT NULL COMMENT '创建人',
    `submitted_by`           VARCHAR(64)           DEFAULT NULL COMMENT '提交人',
    `submitted_time`         DATETIME              DEFAULT NULL COMMENT '提交时间',
    `confirmed_by`           VARCHAR(64)           DEFAULT NULL COMMENT '确认人',
    `confirmed_time`         DATETIME              DEFAULT NULL COMMENT '确认时间',
    `returned_by`            VARCHAR(64)           DEFAULT NULL COMMENT '退回人',
    `returned_time`          DATETIME              DEFAULT NULL COMMENT '退回时间',
    `return_reason`          VARCHAR(512)          DEFAULT NULL COMMENT '退回原因',
    `cancelled_by`           VARCHAR(64)           DEFAULT NULL COMMENT '取消人',
    `cancelled_time`         DATETIME              DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`          VARCHAR(512)          DEFAULT NULL COMMENT '取消原因',
    `failed_by`              VARCHAR(64)           DEFAULT NULL COMMENT '失败记录人',
    `failed_time`            DATETIME              DEFAULT NULL COMMENT '明确失败时间',
    `failure_reason`         VARCHAR(512)          DEFAULT NULL COMMENT '明确失败原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_batch_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_batch_active_digest` (`tenant_id`, `active_amount_digest`),
    KEY `idx_clearing_batch_scope` (`tenant_id`, `subject_type`, `subject_id`, `currency`, `business_line`, `clearing_period`, `status`),
    KEY `idx_clearing_batch_funds_transaction` (`tenant_id`, `funds_transaction_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '清算批次表';

-- ----------------------------
-- 清算批次明细表
CREATE TABLE `t_clearing_batch_detail`
(
    `id`                          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `sn`                          VARCHAR(64)  NOT NULL COMMENT '清算批次明细流水号',
    `tenant_id`                   BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `clearing_batch_sn`           VARCHAR(64)  NOT NULL COMMENT '清算批次流水号',
    `candidate_sn`                VARCHAR(64)  NOT NULL COMMENT '清算候选流水号',
    `split_batch_sn`              VARCHAR(64)  NOT NULL COMMENT '来源清分批次流水号',
    `splittable_detail_sn`        VARCHAR(64)  NOT NULL COMMENT '来源可清分明细流水号',
    `funds_transaction_detail_sn` VARCHAR(64)  NOT NULL COMMENT '来源资金交易明细流水号',
    `ledger_entry_sn`             VARCHAR(64)  NOT NULL COMMENT '来源账本分录流水号',
    `amount`                      BIGINT(20)   NOT NULL COMMENT '清算金额，最小货币单位',
    `currency`                    VARCHAR(10)  NOT NULL COMMENT '币种',
    `created_by`                  VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clearing_batch_detail_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_clearing_batch_detail_candidate` (`tenant_id`, `clearing_batch_sn`, `candidate_sn`),
    KEY `idx_clearing_batch_detail_batch` (`tenant_id`, `clearing_batch_sn`),
    KEY `idx_clearing_batch_detail_entry` (`tenant_id`, `ledger_entry_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '清算批次明细表';

-- ----------------------------
-- 对账批次表
-- ----------------------------
CREATE TABLE `t_reconciliation_batch`
(
    `id`                   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                   VARCHAR(64)  NOT NULL COMMENT '对账批次流水号',
    `tenant_id`            BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `reconciliation_scope_ref` VARCHAR(128) NOT NULL COMMENT '本次对账作业范围的稳定业务引用',
    `gate_object_type`     VARCHAR(50)           DEFAULT NULL COMMENT '准入对象类型；纯对账时为空',
    `gate_object_sn`       VARCHAR(64)           DEFAULT NULL COMMENT '准入对象流水号；纯对账时为空',
    `rule_version`         VARCHAR(64)  NOT NULL COMMENT '匹配或对账规则版本',
    `window_start`         DATETIME     NOT NULL COMMENT '对账窗口开始时间，含',
    `window_end`           DATETIME     NOT NULL COMMENT '对账窗口结束时间，不含',
    `timezone_id`          VARCHAR(64)  NOT NULL COMMENT '对账窗口时区 ID',
    `previous_batch_sn`    VARCHAR(64)           DEFAULT NULL COMMENT '重跑引用的上一批次流水号',
    `status`               VARCHAR(50)  NOT NULL COMMENT 'CREATED/DATA_COLLECTING/DATA_READY/COMPLETED/ABORTED',
    `run_result_sn`        VARCHAR(64)           DEFAULT NULL COMMENT '完成态运行结果流水号',
    `aborted_by`           VARCHAR(64)           DEFAULT NULL COMMENT '终止操作人',
    `aborted_time`         DATETIME              DEFAULT NULL COMMENT '终止时间',
    `abort_reason`         VARCHAR(512)          DEFAULT NULL COMMENT '终止原因',
    `replacement_reason`   VARCHAR(512)          DEFAULT NULL COMMENT '替代上一已完成批次的原因',
    `replacement_evidence_ref` VARCHAR(256)      DEFAULT NULL COMMENT '证明上一已完成批次证据失效的安全引用',
    `batch_digest`         VARCHAR(64)  NOT NULL COMMENT '对账范围、重跑关系与替代事实 SHA-256',
    `created_by`           VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_batch_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_batch_digest` (`tenant_id`, `batch_digest`),
    UNIQUE KEY `uk_reconciliation_batch_previous` (`tenant_id`, `previous_batch_sn`),
    KEY `idx_reconciliation_batch_scope` (`tenant_id`, `reconciliation_scope_ref`, `status`),
    KEY `idx_reconciliation_batch_gate` (`tenant_id`, `gate_object_type`, `gate_object_sn`, `status`),
    KEY `idx_reconciliation_batch_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账批次表';

-- ----------------------------
-- Gate 对账批次血缘表
-- ----------------------------
CREATE TABLE `t_reconciliation_batch_lineage`
(
    `id`                       BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`                BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `reconciliation_scope_ref` VARCHAR(128) NOT NULL COMMENT '对账作业范围稳定业务引用',
    `gate_object_type`         VARCHAR(50)  NOT NULL COMMENT '准入对象类型',
    `gate_object_sn`           VARCHAR(64)  NOT NULL COMMENT '准入对象流水号',
    `current_batch_sn`         VARCHAR(64)  NOT NULL COMMENT '当前批次血缘头流水号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_batch_lineage_object`
        (`tenant_id`, `gate_object_type`, `gate_object_sn`),
    KEY `idx_reconciliation_batch_lineage_current` (`tenant_id`, `current_batch_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'Gate 对账批次当前血缘头';

-- ----------------------------
-- 对账来源快照表
-- ----------------------------
CREATE TABLE `t_reconciliation_source_snapshot`
(
    `id`                       BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                       VARCHAR(64)  NOT NULL COMMENT '来源快照流水号',
    `tenant_id`                BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `reconciliation_batch_sn`  VARCHAR(64)  NOT NULL COMMENT '对账批次流水号',
    `source_role`              VARCHAR(50)  NOT NULL COMMENT 'REFERENCE/COMPARISON',
    `source_type`              VARCHAR(50)  NOT NULL COMMENT '来源事实类型',
    `source_digest`            VARCHAR(64)  NOT NULL COMMENT '来源成员集合 SHA-256',
    `record_count`             INT(11)      NOT NULL COMMENT '来源成员数',
    `evidence_refs`            MEDIUMTEXT   NOT NULL COMMENT '来源文件或报表稳定证据引用 JSON',
    `created_by`               VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_source_snapshot_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_source_snapshot_role` (`tenant_id`, `reconciliation_batch_sn`, `source_role`),
    KEY `idx_reconciliation_source_snapshot_digest` (`tenant_id`, `source_digest`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账来源快照表';

-- ----------------------------
-- 对账来源成员表
-- ----------------------------
CREATE TABLE `t_reconciliation_source_item`
(
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                  VARCHAR(64)  NOT NULL COMMENT '来源成员流水号',
    `tenant_id`           BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `source_snapshot_sn`  VARCHAR(64)  NOT NULL COMMENT '来源快照流水号',
    `source_item_ref`     VARCHAR(128) NOT NULL COMMENT '不可变来源事实稳定引用',
    `content_digest`      VARCHAR(64)  NOT NULL COMMENT '规范化不可变来源事实内容 SHA-256',
    `created_by`          VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_source_item_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_source_item_ref` (`tenant_id`, `source_snapshot_sn`, `source_item_ref`),
    KEY `idx_reconciliation_source_content_digest` (`tenant_id`, `source_snapshot_sn`, `content_digest`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账来源成员表';

-- ----------------------------
-- 对账运行结果表
-- ----------------------------
CREATE TABLE `t_reconciliation_run_result`
(
    `id`                       BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                       VARCHAR(64)  NOT NULL COMMENT '对账运行结果流水号',
    `tenant_id`                BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `reconciliation_batch_sn`  VARCHAR(64)  NOT NULL COMMENT '对账批次流水号',
    `reconciliation_scope_ref` VARCHAR(128) NOT NULL COMMENT '本次对账作业范围的稳定业务引用',
    `gate_object_type`         VARCHAR(50)           DEFAULT NULL COMMENT '准入对象类型；纯对账时为空',
    `gate_object_sn`           VARCHAR(64)           DEFAULT NULL COMMENT '准入对象流水号；纯对账时为空',
    `status`                   VARCHAR(50)  NOT NULL COMMENT 'BALANCED/DIFFERENCE_FOUND',
    `rule_version`             VARCHAR(64)  NOT NULL COMMENT '匹配或对账规则版本',
    `reference_source_digest`  VARCHAR(64)  NOT NULL COMMENT '基准侧来源成员集合 SHA-256',
    `comparison_source_digest` VARCHAR(64)  NOT NULL COMMENT '核对侧来源成员集合 SHA-256',
    `source_digest`            VARCHAR(64)  NOT NULL COMMENT '基准侧与核对侧来源摘要的组合 SHA-256',
    `result_digest`            VARCHAR(64)  NOT NULL COMMENT '对账运行结果 SHA-256',
    `total_count`              INT(11)      NOT NULL COMMENT '参与运行的记录总数',
    `matched_count`            INT(11)      NOT NULL COMMENT '成功匹配记录数',
    `difference_count`         INT(11)      NOT NULL COMMENT '差错记录数',
    `evidence_refs`            MEDIUMTEXT   NOT NULL COMMENT '来源文件、报表或匹配报告证据引用 JSON',
    `created_by`               VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_run_result_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_run_result_business` (`tenant_id`, `reconciliation_batch_sn`),
    KEY `idx_reconciliation_run_result_scope` (`tenant_id`, `reconciliation_scope_ref`, `status`),
    KEY `idx_reconciliation_run_result_gate` (`tenant_id`, `gate_object_type`, `gate_object_sn`, `status`),
    KEY `idx_reconciliation_run_result_digest` (`tenant_id`, `result_digest`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账运行结果表';

-- ----------------------------
-- 对账匹配结果明细表
-- ----------------------------
CREATE TABLE `t_reconciliation_match_result`
(
    `id`                            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                            VARCHAR(64)  NOT NULL COMMENT '匹配结果流水号',
    `tenant_id`                     BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `reconciliation_run_result_sn`  VARCHAR(64)  NOT NULL COMMENT '对账运行结果流水号',
    `reconciliation_batch_sn`       VARCHAR(64)  NOT NULL COMMENT '对账批次流水号',
    `reference_source_ref`          VARCHAR(128)          DEFAULT NULL COMMENT '基准侧事实稳定引用',
    `comparison_source_ref`         VARCHAR(128)          DEFAULT NULL COMMENT '核对侧事实稳定引用',
    `source_quality`                VARCHAR(50)  NOT NULL COMMENT '来源质量',
    `match_strength`                VARCHAR(50)  NOT NULL COMMENT '匹配强度',
    `difference_type`               VARCHAR(50)           DEFAULT NULL COMMENT '差错类型',
    `severity`                      VARCHAR(50)           DEFAULT NULL COMMENT '差错严重等级',
    `currency`                      VARCHAR(10)           DEFAULT NULL COMMENT '差异币种',
    `difference_amount`             BIGINT(20)            DEFAULT NULL COMMENT '金额差异，最小货币单位；非金额差异可空',
    `evidence_ref`                  VARCHAR(256) NOT NULL COMMENT '匹配结论证据引用',
    `match_identity_digest`         VARCHAR(64)  NOT NULL COMMENT '基准侧与核对侧来源对身份 SHA-256',
    `match_digest`                  VARCHAR(64)  NOT NULL COMMENT '匹配结果 SHA-256',
    `created_by`                    VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_match_result_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_match_result_identity` (`tenant_id`, `reconciliation_run_result_sn`, `match_identity_digest`),
    KEY `idx_reconciliation_match_result_digest` (`tenant_id`, `reconciliation_run_result_sn`, `match_digest`),
    KEY `idx_reconciliation_match_result_batch` (`tenant_id`, `reconciliation_batch_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账匹配结果明细表';

-- ----------------------------
-- 对账差错表
-- ----------------------------
CREATE TABLE `t_reconciliation_difference`
(
    `id`                        BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `difference_sn`             VARCHAR(64) NOT NULL COMMENT '对账差错流水号',
    `tenant_id`                 BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `reconciliation_batch_sn`   VARCHAR(64) NOT NULL COMMENT '对账批次流水号',
    `reconciliation_match_result_sn` VARCHAR(64) NOT NULL COMMENT '对账逐笔匹配结果流水号',
    `source_quality`            VARCHAR(50) NOT NULL COMMENT '对账来源质量',
    `match_strength`            VARCHAR(50) NOT NULL COMMENT '对账匹配强度',
    `difference_type`           VARCHAR(50) NOT NULL COMMENT '对账差错类型',
    `severity`                  VARCHAR(50) NOT NULL COMMENT '对账差错严重等级',
    `status`                    VARCHAR(50) NOT NULL COMMENT '对账差错状态',
    `currency`                  VARCHAR(10)          DEFAULT NULL COMMENT '差异币种，金额差异时必填',
    `difference_amount`         BIGINT(20)           DEFAULT NULL COMMENT '差异金额，最小货币单位；非金额差异可空',
    `responsible_party_ref`     VARCHAR(128) NOT NULL COMMENT '责任方引用',
    `blocking_object_type`      VARCHAR(50)          NOT NULL COMMENT '阻断对象类型',
    `blocking_object_sn`        VARCHAR(64)          NOT NULL COMMENT '阻断对象流水号',
    `rule_version`              VARCHAR(64) NOT NULL COMMENT '匹配或对账规则版本',
    `evidence_ref`              VARCHAR(256) NOT NULL COMMENT '来源证据引用，与逐笔匹配证据宽度一致',
    `action_type`               VARCHAR(50)          DEFAULT NULL COMMENT '差错处理动作类型',
    `adjustment_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '关联处理动作或调账单号',
    `adjustment_idempotency_key` VARCHAR(128)         DEFAULT NULL COMMENT '处理动作幂等键',
    `original_fact_ref`         VARCHAR(128)         DEFAULT NULL COMMENT '被处理的原始事实引用',
    `adjustment_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '关联资金交易流水号',
    `adjustment_approval_ref`   VARCHAR(128)         DEFAULT NULL COMMENT '调账审批引用',
    `adjustment_evidence_ref`   VARCHAR(256)         DEFAULT NULL COMMENT '调账证据引用',
    `adjustment_reason`         VARCHAR(512)         DEFAULT NULL COMMENT '处理原因',
    `last_rerun_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑对账运行结果流水号',
    `last_rerun_batch_sn`       VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑批次流水号',
    `last_rerun_rule_version`   VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑规则版本',
    `last_rerun_balanced`       TINYINT(1)           DEFAULT NULL COMMENT '最后一次重跑是否对平',
    `last_rerun_evidence_ref`   VARCHAR(128)         DEFAULT NULL COMMENT '最后一次重跑运行结果证据引用',
    `last_rerun_result_digest`  VARCHAR(128)         DEFAULT NULL COMMENT '最后一次重跑结果摘要',
    `rerun_count`               INT(11)     NOT NULL DEFAULT 0 COMMENT '重跑次数',
    `created_by`                VARCHAR(64)          DEFAULT NULL COMMENT '创建人',
    `adjusted_by`               VARCHAR(64)          DEFAULT NULL COMMENT '处理人',
    `resolved_by`               VARCHAR(64)          DEFAULT NULL COMMENT '关闭人',
    `adjusted_time`             DATETIME             DEFAULT NULL COMMENT '处理时间',
    `resolved_time`             DATETIME             DEFAULT NULL COMMENT '关闭时间',
    `description`               VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `version`                   INT(11)     NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_difference_sn` (`tenant_id`, `difference_sn`),
    UNIQUE KEY `uk_reconciliation_difference_match_result` (`tenant_id`, `reconciliation_match_result_sn`),
    KEY `idx_reconciliation_difference_batch` (`tenant_id`, `reconciliation_batch_sn`),
    KEY `idx_reconciliation_difference_status` (`tenant_id`, `status`),
    KEY `idx_reconciliation_difference_blocking_object` (`tenant_id`, `blocking_object_type`, `blocking_object_sn`, `status`),
    KEY `idx_reconciliation_difference_adjustment` (`tenant_id`, `adjustment_sn`),
    KEY `idx_reconciliation_difference_rerun` (`tenant_id`, `last_rerun_sn`),
    KEY `idx_reconciliation_difference_rerun_batch` (`tenant_id`, `last_rerun_batch_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账差错表';

-- ----------------------------
-- 对账差错处理动作事实表
-- ----------------------------
CREATE TABLE `t_reconciliation_difference_action`
(
    `id`                        BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                        VARCHAR(64)  NOT NULL COMMENT '差错处理动作记录流水号',
    `tenant_id`                 BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `difference_sn`             VARCHAR(64)  NOT NULL COMMENT '对账差错流水号',
    `action_type`               VARCHAR(50)  NOT NULL COMMENT '差错处理动作类型',
    `adjustment_sn`             VARCHAR(64)  NOT NULL COMMENT '上层处理动作、调账、冲正、挂账、追偿或核销单号',
    `idempotency_key`           VARCHAR(128) NOT NULL COMMENT '处理动作幂等键',
    `original_fact_ref`         VARCHAR(128) NOT NULL COMMENT '被处理的原始事实引用',
    `adjustment_transaction_sn` VARCHAR(64)           DEFAULT NULL COMMENT '关联资金交易流水号',
    `approval_ref`              VARCHAR(128) NOT NULL COMMENT '审批引用',
    `evidence_ref`              VARCHAR(256) NOT NULL COMMENT '处理证据引用',
    `reason`                    VARCHAR(512) NOT NULL COMMENT '处理原因',
    `created_by`                VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_difference_action_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_difference_action_adjustment` (`tenant_id`, `adjustment_sn`),
    UNIQUE KEY `uk_reconciliation_difference_action_idempotency` (`tenant_id`, `idempotency_key`),
    KEY `idx_reconciliation_difference_action_difference` (`tenant_id`, `difference_sn`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '对账差错处理动作事实表';
