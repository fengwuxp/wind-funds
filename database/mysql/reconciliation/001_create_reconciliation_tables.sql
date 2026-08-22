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
    `gate_evidence_ref`               VARCHAR(64)  NOT NULL COMMENT '清分准入消费的 Stage Gate evidence 流水号',
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
        (`tenant_id`, `subject_type`, `subject_id`, `currency`, `business_line`, `split_period`, `status`),
    KEY `idx_clearing_split_batch_status_age` (`tenant_id`, `status`, `gmt_modified`)
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
    `gate_evidence_ref`            VARCHAR(64) NOT NULL COMMENT '清分确认消费的 Stage Gate evidence 流水号',
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
    `gate_evidence_ref`               VARCHAR(64)  NOT NULL COMMENT '清算候选消费的 Stage Gate evidence 流水号',
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
    KEY `idx_clearing_candidate_status_available` (`tenant_id`, `status`, `clearing_available_time`),
    KEY `idx_clearing_candidate_status_changed` (`tenant_id`, `status`, `status_changed_time`),
    KEY `idx_clearing_candidate_locked_batch` (`tenant_id`, `locked_clearing_batch_sn`, `status`)
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
    KEY `idx_clearing_batch_status_age` (`tenant_id`, `status`, `gmt_modified`),
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
    `id`                         BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                         VARCHAR(64)  NOT NULL COMMENT '对账批次流水号',
    `tenant_id`                  BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `scope_owner_namespace`      VARCHAR(64)  NOT NULL COMMENT '对账 scope 身份 Owner namespace',
    `scope_identity_value`       VARCHAR(128) NOT NULL COMMENT '对账 scope 稳定身份',
    `pair_owner_namespace`       VARCHAR(64)  NOT NULL COMMENT '对账 pair 身份 Owner namespace',
    `pair_identity_value`        VARCHAR(128) NOT NULL COMMENT '对账 pair 稳定身份',
    `currency`                   VARCHAR(10)  NOT NULL COMMENT '对账币种',
    `rule_namespace`             VARCHAR(64)  NOT NULL COMMENT '比较规则 namespace',
    `rule_identity`              VARCHAR(128) NOT NULL COMMENT '比较规则稳定身份',
    `rule_version`               VARCHAR(64)  NOT NULL COMMENT '比较规则版本',
    `window_start`               DATETIME     NOT NULL COMMENT '对账窗口开始时间，含',
    `window_end`                 DATETIME     NOT NULL COMMENT '对账窗口结束时间，不含',
    `time_semantics`             VARCHAR(64)  NOT NULL COMMENT '窗口时间语义',
    `timezone_id`                VARCHAR(64)  NOT NULL COMMENT '对账窗口时区 ID',
    `previous_batch_sn`          VARCHAR(64)           DEFAULT NULL COMMENT '重跑引用的上一批次流水号',
    `status`                     VARCHAR(50)  NOT NULL COMMENT 'CREATED/DATA_COLLECTING/DATA_READY/COMPLETED/ABORTED',
    `run_result_sn`              VARCHAR(64)           DEFAULT NULL COMMENT '完成态运行结果流水号',
    `aborted_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '终止操作人',
    `aborted_time`               DATETIME              DEFAULT NULL COMMENT '终止时间',
    `abort_reason`               VARCHAR(512)          DEFAULT NULL COMMENT '终止原因',
    `replacement_reason`         VARCHAR(512)          DEFAULT NULL COMMENT '替代上一已完成批次的原因',
    `replacement_evidence_ref`   VARCHAR(256)          DEFAULT NULL COMMENT '证明上一批次证据失效的安全引用',
    `batch_digest`               VARCHAR(64)  NOT NULL COMMENT '对账范围、规则与血缘事实 SHA-256',
    `created_by`                 VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_batch_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_batch_digest` (`tenant_id`, `batch_digest`),
    UNIQUE KEY `uk_reconciliation_batch_previous` (`tenant_id`, `previous_batch_sn`),
    KEY `idx_reconciliation_batch_scope_pair`
        (`tenant_id`, `scope_owner_namespace`, `scope_identity_value`, `pair_owner_namespace`, `pair_identity_value`, `status`),
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
    `scope_owner_namespace`    VARCHAR(64)  NOT NULL COMMENT '对账 scope 身份 Owner namespace',
    `scope_identity_value`     VARCHAR(128) NOT NULL COMMENT '对账 scope 稳定身份',
    `pair_owner_namespace`     VARCHAR(64)  NOT NULL COMMENT '对账 pair 身份 Owner namespace',
    `pair_identity_value`      VARCHAR(128) NOT NULL COMMENT '对账 pair 稳定身份',
    `current_batch_sn`         VARCHAR(64)  NOT NULL COMMENT '当前批次血缘头流水号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_batch_lineage_pair`
        (`tenant_id`, `scope_owner_namespace`, `scope_identity_value`, `pair_owner_namespace`, `pair_identity_value`),
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
    `source_namespace`         VARCHAR(64)  NOT NULL COMMENT '归一来源逻辑 namespace，不绑定载体',
    `snapshot_owner_namespace` VARCHAR(64)  NOT NULL COMMENT '来源快照身份 Owner namespace',
    `snapshot_identity_value`  VARCHAR(128) NOT NULL COMMENT '来源快照稳定身份',
    `snapshot_version`         VARCHAR(64)  NOT NULL COMMENT '来源快照版本',
    `coverage_complete`        TINYINT(1)   NOT NULL COMMENT 'coverage 是否完整',
    `coverage_watermark`       VARCHAR(128)          DEFAULT NULL COMMENT 'coverage watermark',
    `coverage_member_count`    INT(11)      NOT NULL COMMENT 'coverage 成员数',
    `source_digest`            VARCHAR(64)  NOT NULL COMMENT '来源成员集合 SHA-256',
    `semantic_digest`          VARCHAR(64)  NOT NULL COMMENT '来源快照语义 SHA-256',
    `evidence_bundle_digest`   VARCHAR(64)  NOT NULL COMMENT '证据引用集合 SHA-256',
    `evidence_refs`            MEDIUMTEXT   NOT NULL COMMENT '稳定证据引用 JSON',
    `created_by`               VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_source_snapshot_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_source_snapshot_role` (`tenant_id`, `reconciliation_batch_sn`, `source_role`),
    UNIQUE KEY `uk_reconciliation_source_snapshot_identity`
        (`tenant_id`, `snapshot_owner_namespace`, `snapshot_identity_value`, `snapshot_version`),
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
    `source_fact_owner_namespace` VARCHAR(64)  NOT NULL COMMENT '来源事实身份 Owner namespace',
    `source_fact_identity_value`  VARCHAR(128) NOT NULL COMMENT '来源事实稳定身份',
    `comparison_owner_namespace`  VARCHAR(64)  NOT NULL COMMENT 'comparison identity Owner namespace',
    `comparison_identity_value`   VARCHAR(128) NOT NULL COMMENT 'comparison identity value',
    `amount`                      BIGINT(20)   NOT NULL COMMENT '归一金额，最小货币单位',
    `currency`                    VARCHAR(10)  NOT NULL COMMENT '归一币种',
    `rule_namespace`              VARCHAR(64)  NOT NULL COMMENT '比较规则 namespace',
    `rule_identity`               VARCHAR(128) NOT NULL COMMENT '比较规则稳定身份',
    `rule_version`                VARCHAR(64)  NOT NULL COMMENT '比较规则版本',
    `comparison_status_code`      VARCHAR(64)  NOT NULL COMMENT '规则域内归一比较状态',
    `comparison_proven`           TINYINT(1)   NOT NULL COMMENT '比较状态是否已证明',
    `claim_kind`                  VARCHAR(64)  NOT NULL COMMENT '归一 claim kind',
    `economic_component`          VARCHAR(64)  NOT NULL COMMENT '归一经济组成',
    `direction`                   VARCHAR(64)  NOT NULL COMMENT '归一资金方向',
    `normalization_version`       VARCHAR(64)  NOT NULL COMMENT '归一规则版本',
    `semantic_digest`             VARCHAR(64)  NOT NULL COMMENT '归一事实语义 SHA-256',
    `evidence_bundle_digest`      VARCHAR(64)  NOT NULL COMMENT '证据引用集合 SHA-256',
    `evidence_refs`               MEDIUMTEXT   NOT NULL COMMENT '稳定证据引用 JSON',
    `created_by`          VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_source_item_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_source_item_fact`
        (`tenant_id`, `source_snapshot_sn`, `source_fact_owner_namespace`, `source_fact_identity_value`),
    KEY `idx_reconciliation_source_item_comparison`
        (`tenant_id`, `source_snapshot_sn`, `comparison_owner_namespace`, `comparison_identity_value`),
    KEY `idx_reconciliation_source_item_semantic_digest` (`tenant_id`, `source_snapshot_sn`, `semantic_digest`)
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
    `scope_owner_namespace`    VARCHAR(64)  NOT NULL COMMENT '对账 scope 身份 Owner namespace',
    `scope_identity_value`     VARCHAR(128) NOT NULL COMMENT '对账 scope 稳定身份',
    `pair_owner_namespace`     VARCHAR(64)  NOT NULL COMMENT '对账 pair 身份 Owner namespace',
    `pair_identity_value`      VARCHAR(128) NOT NULL COMMENT '对账 pair 稳定身份',
    `currency`                 VARCHAR(10)  NOT NULL COMMENT '对账币种',
    `status`                   VARCHAR(50)  NOT NULL COMMENT 'BALANCED/DIFFERENCE_FOUND',
    `rule_namespace`           VARCHAR(64)  NOT NULL COMMENT '比较规则 namespace',
    `rule_identity`            VARCHAR(128) NOT NULL COMMENT '比较规则稳定身份',
    `rule_version`             VARCHAR(64)  NOT NULL COMMENT '匹配或对账规则版本',
    `reference_snapshot_sn`    VARCHAR(64)  NOT NULL COMMENT '基准侧来源快照流水号',
    `comparison_snapshot_sn`   VARCHAR(64)  NOT NULL COMMENT '核对侧来源快照流水号',
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
    KEY `idx_reconciliation_run_result_scope_pair`
        (`tenant_id`, `scope_owner_namespace`, `scope_identity_value`, `pair_owner_namespace`, `pair_identity_value`, `status`),
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
    `reference_fact_owner_namespace` VARCHAR(64)           DEFAULT NULL COMMENT '基准侧事实 Owner namespace',
    `reference_fact_identity_value`  VARCHAR(128)          DEFAULT NULL COMMENT '基准侧事实稳定身份',
    `comparison_fact_owner_namespace` VARCHAR(64)          DEFAULT NULL COMMENT '核对侧事实 Owner namespace',
    `comparison_fact_identity_value` VARCHAR(128)          DEFAULT NULL COMMENT '核对侧事实稳定身份',
    `comparison_owner_namespace`     VARCHAR(64)  NOT NULL COMMENT 'comparison identity Owner namespace',
    `comparison_identity_value`      VARCHAR(128) NOT NULL COMMENT 'comparison identity value',
    `result_kind`                    VARCHAR(50)  NOT NULL COMMENT 'strict-exact 有限结果类型',
    `absolute_difference_currency`   VARCHAR(10)           DEFAULT NULL COMMENT '同币金额差异币种',
    `absolute_difference_amount`     BIGINT(20)            DEFAULT NULL COMMENT '同币绝对金额差异',
    `larger_side`                    VARCHAR(50)           DEFAULT NULL COMMENT 'REFERENCE/COMPARISON',
    `evidence_refs`                  MEDIUMTEXT   NOT NULL COMMENT '匹配结论证据引用 JSON',
    `match_identity_digest`         VARCHAR(64)  NOT NULL COMMENT '基准侧与核对侧来源对身份 SHA-256',
    `result_digest`                 VARCHAR(64)  NOT NULL COMMENT '匹配结果 SHA-256',
    `created_by`                    VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_match_result_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_match_result_identity` (`tenant_id`, `reconciliation_run_result_sn`, `match_identity_digest`),
    KEY `idx_reconciliation_match_result_digest` (`tenant_id`, `reconciliation_run_result_sn`, `result_digest`),
    KEY `idx_reconciliation_match_result_comparison`
        (`tenant_id`, `reconciliation_run_result_sn`, `comparison_owner_namespace`, `comparison_identity_value`),
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
    `scope_owner_namespace`     VARCHAR(64) NOT NULL COMMENT '对账 scope 身份 Owner namespace',
    `scope_identity_value`      VARCHAR(128) NOT NULL COMMENT '对账 scope 稳定身份',
    `pair_owner_namespace`      VARCHAR(64) NOT NULL COMMENT '对账 pair 身份 Owner namespace',
    `pair_identity_value`       VARCHAR(128) NOT NULL COMMENT '对账 pair 稳定身份',
    `difference_type`           VARCHAR(50) NOT NULL COMMENT '对账差错类型',
    `severity`                  VARCHAR(50) NOT NULL COMMENT '对账差错严重等级',
    `status`                    VARCHAR(50) NOT NULL COMMENT '对账差错状态',
    `currency`                  VARCHAR(10)          DEFAULT NULL COMMENT '差异币种，金额差异时必填',
    `difference_amount`         BIGINT(20)           DEFAULT NULL COMMENT '差异金额，最小货币单位；非金额差异可空',
    `responsible_party_ref`     VARCHAR(128) NOT NULL COMMENT '责任方引用',
    `rule_namespace`            VARCHAR(64) NOT NULL COMMENT '比较规则 namespace',
    `rule_identity`             VARCHAR(128) NOT NULL COMMENT '比较规则稳定身份',
    `rule_version`              VARCHAR(64) NOT NULL COMMENT '匹配或对账规则版本',
    `current_lineage_ref`       VARCHAR(128) NOT NULL COMMENT 'current scope+pair lineage 稳定引用',
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
    KEY `idx_reconciliation_difference_scope_pair`
        (`tenant_id`, `scope_owner_namespace`, `scope_identity_value`, `pair_owner_namespace`, `pair_identity_value`, `status`),
    KEY `idx_reconciliation_difference_lineage` (`tenant_id`, `current_lineage_ref`, `status`),
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

-- immutable Gate Requirement header
CREATE TABLE `t_reconciliation_gate_requirement`
(
    `id`                                      BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `tenant_id`                               BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `stage_kind`                              VARCHAR(50)  NOT NULL COMMENT 'exact Stage action kind',
    `stage_identity_owner_namespace`          VARCHAR(64)  NOT NULL COMMENT 'Stage identity Owner namespace',
    `stage_identity_value`                    VARCHAR(128) NOT NULL COMMENT 'Stage stable identity',
    `requirement_identity_owner_namespace`    VARCHAR(64)  NOT NULL COMMENT 'Requirement identity Owner namespace',
    `requirement_identity_value`              VARCHAR(128) NOT NULL COMMENT 'Requirement stable identity',
    `requirement_version`                     VARCHAR(64)  NOT NULL COMMENT 'Requirement version',
    `semantic_digest`                         VARCHAR(64)  NOT NULL COMMENT 'Requirement semantic SHA-256',
    `evidence_refs`                           MEDIUMTEXT   NOT NULL COMMENT 'Requirement evidence refs JSON',
    `evidence_bundle_digest`                  VARCHAR(64)  NOT NULL COMMENT 'Evidence bundle SHA-256',
    `previous_requirement_identity_owner_namespace` VARCHAR(64)  DEFAULT NULL COMMENT 'Previous Requirement identity Owner namespace',
    `previous_requirement_identity_value`     VARCHAR(128)         DEFAULT NULL COMMENT 'Previous Requirement stable identity',
    `previous_requirement_version`            VARCHAR(64)          DEFAULT NULL COMMENT 'Previous Requirement version',
    `previous_semantic_digest`                VARCHAR(64)          DEFAULT NULL COMMENT 'Previous semantic SHA-256',
    `previous_evidence_bundle_digest`         VARCHAR(64)          DEFAULT NULL COMMENT 'Previous evidence bundle SHA-256',
    `created_by`                              VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_gate_requirement_identity`
        (`tenant_id`, `requirement_identity_owner_namespace`, `requirement_identity_value`),
    UNIQUE KEY `uk_reconciliation_gate_requirement_stage_version`
        (`tenant_id`, `stage_kind`, `stage_identity_owner_namespace`, `stage_identity_value`, `requirement_version`),
    KEY `idx_reconciliation_gate_requirement_previous`
        (`tenant_id`, `previous_requirement_identity_owner_namespace`, `previous_requirement_identity_value`, `previous_requirement_version`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '不可变 Gate Requirement header';

-- mandatory scope+pair rows of one immutable Requirement
CREATE TABLE `t_reconciliation_gate_requirement_pair`
(
    `id`                                   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `tenant_id`                            BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `requirement_identity_owner_namespace` VARCHAR(64)  NOT NULL COMMENT 'Requirement identity Owner namespace',
    `requirement_identity_value`           VARCHAR(128) NOT NULL COMMENT 'Requirement stable identity',
    `scope_owner_namespace`                VARCHAR(64)  NOT NULL COMMENT 'Required scope Owner namespace',
    `scope_identity_value`                 VARCHAR(128) NOT NULL COMMENT 'Required scope stable identity',
    `pair_owner_namespace`                 VARCHAR(64)  NOT NULL COMMENT 'Required pair Owner namespace',
    `pair_identity_value`                  VARCHAR(128) NOT NULL COMMENT 'Required pair stable identity',
    `rule_namespace`                       VARCHAR(64)  NOT NULL COMMENT 'Comparison rule namespace',
    `rule_identity`                        VARCHAR(128) NOT NULL COMMENT 'Comparison rule stable identity',
    `rule_version`                         VARCHAR(64)  NOT NULL COMMENT 'Comparison rule version',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_gate_requirement_pair`
        (`tenant_id`, `requirement_identity_owner_namespace`, `requirement_identity_value`,
         `scope_owner_namespace`, `scope_identity_value`, `pair_owner_namespace`, `pair_identity_value`),
    KEY `idx_reconciliation_gate_requirement_pair_requirement`
        (`tenant_id`, `requirement_identity_owner_namespace`, `requirement_identity_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'Gate Requirement mandatory scope and pair';

-- one current Requirement pointer per exact Stage action
CREATE TABLE `t_reconciliation_gate_requirement_head`
(
    `id`                                           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`                                    BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `stage_kind`                                   VARCHAR(50)  NOT NULL COMMENT 'exact Stage action kind',
    `stage_identity_owner_namespace`               VARCHAR(64)  NOT NULL COMMENT 'Stage identity Owner namespace',
    `stage_identity_value`                         VARCHAR(128) NOT NULL COMMENT 'Stage stable identity',
    `current_requirement_identity_owner_namespace` VARCHAR(64)  NOT NULL COMMENT 'Current Requirement identity Owner namespace',
    `current_requirement_identity_value`           VARCHAR(128) NOT NULL COMMENT 'Current Requirement stable identity',
    `current_requirement_version`                  VARCHAR(64)  NOT NULL COMMENT 'Current Requirement version',
    `current_semantic_digest`                      VARCHAR(64)  NOT NULL COMMENT 'Current Requirement semantic SHA-256',
    `current_evidence_bundle_digest`               VARCHAR(64)  NOT NULL COMMENT 'Current evidence bundle SHA-256',
    `version`                                      INT(11)      NOT NULL DEFAULT 0 COMMENT 'CAS version',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_gate_requirement_head_stage`
        (`tenant_id`, `stage_kind`, `stage_identity_owner_namespace`, `stage_identity_value`),
    KEY `idx_reconciliation_gate_requirement_head_current`
        (`tenant_id`, `current_requirement_identity_owner_namespace`, `current_requirement_identity_value`, `current_requirement_version`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'exact Stage current Gate Requirement head';

-- consumed Gate evidence written with one successful exact Stage action
CREATE TABLE `t_reconciliation_stage_gate_evidence`
(
    `id`                                   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                                   VARCHAR(64)  NOT NULL COMMENT 'Stage Gate evidence 流水号',
    `tenant_id`                            BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `stage_kind`                           VARCHAR(50)  NOT NULL COMMENT 'exact Stage action kind',
    `stage_identity_owner_namespace`       VARCHAR(64)  NOT NULL COMMENT 'Stage identity Owner namespace',
    `stage_identity_value`                 VARCHAR(128) NOT NULL COMMENT 'Stage stable identity',
    `requirement_identity_owner_namespace` VARCHAR(64)  NOT NULL COMMENT 'Consumed Requirement identity Owner namespace',
    `requirement_identity_value`           VARCHAR(128) NOT NULL COMMENT 'Consumed Requirement stable identity',
    `requirement_version`                  VARCHAR(64)  NOT NULL COMMENT 'Consumed Requirement version',
    `requirement_semantic_digest`          VARCHAR(64)  NOT NULL COMMENT 'Consumed Requirement semantic SHA-256',
    `requirement_evidence_bundle_digest`   VARCHAR(64)  NOT NULL COMMENT 'Consumed Requirement evidence bundle SHA-256',
    `consumed_pair_evidence`               MEDIUMTEXT   NOT NULL COMMENT 'Sorted required pair run/lineage/result evidence JSON',
    `decision_digest`                      VARCHAR(64)  NOT NULL COMMENT 'Gate decision SHA-256',
    `evidence_refs`                        MEDIUMTEXT   NOT NULL COMMENT 'Consumed Gate evidence refs JSON',
    `created_by`                           VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconciliation_stage_gate_evidence_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_reconciliation_stage_gate_evidence_stage`
        (`tenant_id`, `stage_kind`, `stage_identity_owner_namespace`, `stage_identity_value`),
    KEY `idx_reconciliation_stage_gate_evidence_requirement`
        (`tenant_id`, `requirement_identity_owner_namespace`, `requirement_identity_value`, `requirement_version`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'Stage 成功事务消费的 Gate evidence';


-- 结算单表：只表达内部结算锁定，不表达外部出款状态
CREATE TABLE `t_settlement_order`
(
    `id`                          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                          VARCHAR(64)  NOT NULL COMMENT '结算单流水号',
    `tenant_id`                   BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `settlement_subject_type`     VARCHAR(50)  NOT NULL COMMENT '结算主体类型',
    `settlement_subject_id`       VARCHAR(64)  NOT NULL COMMENT '结算主体 ID',
    `currency`                    VARCHAR(10)  NOT NULL COMMENT '币种',
    `settlement_period`           VARCHAR(30)  NOT NULL COMMENT '结算周期',
    `settlement_mode`             VARCHAR(30)  NOT NULL COMMENT '结算模式；当前只支持 INTERMEDIARY_ACCOUNT',
    `settlement_destination`      VARCHAR(50)  NOT NULL COMMENT '结算去向；INTERNAL_ACCOUNT/EXTERNAL_ENDPOINT',
    `trigger_mode`                VARCHAR(30)  NOT NULL COMMENT '触发方式；当前只支持 HOST_COMMAND',
    `timezone`                    VARCHAR(50)  NOT NULL COMMENT '策略时区',
    `cutoff`                      VARCHAR(30)  NOT NULL COMMENT 'cutoff 规则快照',
    `total_amount`                BIGINT(20)   NOT NULL COMMENT '金额项绝对值汇总，最小货币单位',
    `add_amount`                  BIGINT(20)   NOT NULL COMMENT '加项金额，最小货币单位',
    `deduct_amount`               BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '扣减金额，当前固定 0',
    `reserve_amount`              BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '准备金金额，当前固定 0',
    `net_amount`                  BIGINT(20)   NOT NULL COMMENT '净结算金额，最小货币单位',
    `status`                      VARCHAR(50)  NOT NULL COMMENT 'DRAFT/REVIEWING/APPROVED/LOCKED/FAILED/CANCELLED/RELEASED',
    `settlement_approval_ref`     VARCHAR(128)          DEFAULT NULL COMMENT '结算审批引用',
    `lock_funds_transaction_sn`   VARCHAR(64)           DEFAULT NULL COMMENT '锁定资金交易流水号',
    `release_funds_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '释放资金交易流水号',
    `release_freeze_order_sn`     VARCHAR(64)           DEFAULT NULL COMMENT '释放后受保护冻结单流水号',
    `release_disposition`         VARCHAR(30)           DEFAULT NULL COMMENT '释放处置；当前仅 FROZEN',
    `release_digest`              VARCHAR(64)           DEFAULT NULL COMMENT '释放请求幂等 SHA-256',
    `release_gate_evidence_ref`       VARCHAR(64)        DEFAULT NULL COMMENT '释放消费的 Stage Gate evidence 流水号',
    `release_current_lineage_batch_sn` VARCHAR(64)      DEFAULT NULL COMMENT '释放时当前对账血缘批次流水号',
    `release_source_closure_digest` VARCHAR(64)         DEFAULT NULL COMMENT '来源关闭事实 SHA-256',
    `release_authority_decision_digest` VARCHAR(64)     DEFAULT NULL COMMENT '释放授权决策 SHA-256',
    `release_authority_evidence_refs` TEXT              DEFAULT NULL COMMENT '释放授权证据引用 JSON',
    `release_approval_ref`        VARCHAR(128)          DEFAULT NULL COMMENT '释放审批引用',
    `release_reason`              VARCHAR(512)          DEFAULT NULL COMMENT '释放原因',
    `released_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '释放操作人',
    `released_time`               DATETIME              DEFAULT NULL COMMENT '释放时间',
    `rule_code`                   VARCHAR(64)  NOT NULL COMMENT '结算策略编码',
    `rule_version`                VARCHAR(64)  NOT NULL COMMENT '结算策略版本',
    `policy_approval_ref`         VARCHAR(128)          DEFAULT NULL COMMENT '策略审批引用',
    `amount_digest`               VARCHAR(64)  NOT NULL COMMENT '不可变金额项 SHA-256',
    `source_digest`               VARCHAR(64)  NOT NULL COMMENT '排序后来源集合 SHA-256',
    `policy_snapshot_digest`      VARCHAR(64)  NOT NULL COMMENT '策略快照 SHA-256',
    `order_digest`                VARCHAR(64)  NOT NULL COMMENT '创建请求幂等摘要',
    `created_by`                  VARCHAR(64)  NOT NULL COMMENT '创建人',
    `submitted_by`                VARCHAR(64)           DEFAULT NULL COMMENT '提交人',
    `submitted_time`              DATETIME              DEFAULT NULL COMMENT '提交时间',
    `approved_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '审批记录人',
    `approved_time`               DATETIME              DEFAULT NULL COMMENT '审批时间',
    `locked_by`                   VARCHAR(64)           DEFAULT NULL COMMENT '锁定人',
    `locked_time`                 DATETIME              DEFAULT NULL COMMENT '锁定时间',
    `returned_by`                 VARCHAR(64)           DEFAULT NULL COMMENT '退回人',
    `returned_time`               DATETIME              DEFAULT NULL COMMENT '退回时间',
    `return_reason`               VARCHAR(512)          DEFAULT NULL COMMENT '退回原因',
    `cancelled_by`                VARCHAR(64)           DEFAULT NULL COMMENT '取消人',
    `cancelled_time`              DATETIME              DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`               VARCHAR(512)          DEFAULT NULL COMMENT '取消原因',
    `lock_gate_evidence_ref`       VARCHAR(64)           DEFAULT NULL COMMENT '锁定消费的 Stage Gate evidence 流水号',
    `active_order_digest`         VARCHAR(64)           DEFAULT NULL COMMENT '活动创建幂等占用；取消或明确失败后置 NULL',
    `failed_by`                   VARCHAR(64)           DEFAULT NULL COMMENT '明确失败记录人',
    `failed_time`                 DATETIME              DEFAULT NULL COMMENT '明确失败时间',
    `failure_reason`              VARCHAR(512)          DEFAULT NULL COMMENT '明确失败原因',
    `version`                     INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_order_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_settlement_order_active_digest` (`tenant_id`, `active_order_digest`),
    KEY `idx_settlement_order_subject` (`tenant_id`, `settlement_subject_type`, `settlement_subject_id`, `settlement_period`),
    KEY `idx_settlement_order_status` (`tenant_id`, `status`, `gmt_modified`),
    KEY `idx_settlement_order_lock_transaction` (`tenant_id`, `lock_funds_transaction_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '结算单表';

-- 结算金额项表：活动来源占用由 nullable claim 唯一键保护
CREATE TABLE `t_settlement_order_item`
(
    `id`                    BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                    VARCHAR(64)  NOT NULL COMMENT '结算金额项流水号',
    `tenant_id`             BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `settlement_order_sn`   VARCHAR(64)  NOT NULL COMMENT '结算单流水号',
    `item_type`             VARCHAR(30)  NOT NULL COMMENT '金额项类型；当前固定 PRINCIPAL',
    `direction`             VARCHAR(20)  NOT NULL COMMENT '金额方向；当前固定 ADD',
    `source_type`           VARCHAR(30)  NOT NULL COMMENT '来源类型；当前固定 CLEARING_BATCH',
    `source_sn`             VARCHAR(64)  NOT NULL COMMENT '已确认清算批次流水号',
    `amount`                BIGINT(20)   NOT NULL COMMENT '金额，最小货币单位',
    `currency`              VARCHAR(10)  NOT NULL COMMENT '币种',
    `source_amount_digest`  VARCHAR(64)  NOT NULL COMMENT '创建时清算批次金额摘要',
    `active_source_claim`   TINYINT(1)            DEFAULT NULL COMMENT '活动来源占用标记；活动时为 1，取消时置 NULL',
    `created_by`            VARCHAR(64)  NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_order_item_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_settlement_order_item_source` (`tenant_id`, `settlement_order_sn`, `source_type`, `source_sn`),
    UNIQUE KEY `uk_settlement_item_active_source` (`tenant_id`, `source_type`, `source_sn`, `active_source_claim`),
    KEY `idx_settlement_order_item_order` (`tenant_id`, `settlement_order_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '结算金额项表';

-- 出款单表：表达外部出款状态和终态资金事实，不承载通道执行
CREATE TABLE `t_payout_order`
(
    `id`                             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                             VARCHAR(64)  NOT NULL COMMENT '出款单流水号',
    `tenant_id`                      BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `settlement_order_sn`            VARCHAR(64)  NOT NULL COMMENT '锁定结算单流水号',
    `settlement_subject_type`        VARCHAR(50)  NOT NULL COMMENT '结算资金主体类型',
    `settlement_subject_id`          VARCHAR(64)  NOT NULL COMMENT '结算资金主体 ID',
    `amount`                         BIGINT(20)   NOT NULL COMMENT '出款金额，最小货币单位',
    `currency`                       VARCHAR(10)  NOT NULL COMMENT '币种',
    `status`                         VARCHAR(30)  NOT NULL COMMENT 'CREATED/SUBMITTED/ACCEPTED/PROCESSING/SUCCEEDED/FAILED/RETURNED/MISMATCHED/CANCELLED',
    `payout_account_ref`             VARCHAR(128)          DEFAULT NULL COMMENT '宿主出款账户引用',
    `payee_endpoint_ref`             VARCHAR(128)          DEFAULT NULL COMMENT '宿主收款端点引用',
    `channel_ref`                    VARCHAR(128)          DEFAULT NULL COMMENT '宿主通道引用',
    `approval_ref`                   VARCHAR(128)          DEFAULT NULL COMMENT '出款审批引用',
    `external_rule_evidence_digest`  VARCHAR(64)           DEFAULT NULL COMMENT '外部规则核验证据 SHA-256',
    `payout_gate_evidence_ref`       VARCHAR(64)           DEFAULT NULL COMMENT '提交消费的 Stage Gate evidence 流水号',
    `admission_decision_digest`      VARCHAR(64)           DEFAULT NULL COMMENT '宿主权威准入决策 SHA-256',
    `admission_evidence_refs`        TEXT                  DEFAULT NULL COMMENT '宿主权威准入证据引用 JSON',
    `submit_digest`                  VARCHAR(64)           DEFAULT NULL COMMENT '提交请求幂等摘要',
    `external_reference`             VARCHAR(128)          DEFAULT NULL COMMENT '外部出款 reference',
    `completion_funds_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '成功资金交易流水号',
    `rollback_funds_transaction_sn`  VARCHAR(64)           DEFAULT NULL COMMENT '失败回退资金交易流水号',
    `last_receipt_digest`            VARCHAR(64)           DEFAULT NULL COMMENT '最近规范化回单 SHA-256',
    `failure_code`                   VARCHAR(64)           DEFAULT NULL COMMENT '脱敏失败码',
    `failure_reason`                 VARCHAR(512)          DEFAULT NULL COMMENT '脱敏失败原因或冲突说明',
    `created_by`                     VARCHAR(64)  NOT NULL COMMENT '创建人',
    `submitted_by`                   VARCHAR(64)           DEFAULT NULL COMMENT '提交人',
    `submitted_time`                 DATETIME              DEFAULT NULL COMMENT '提交时间',
    `completed_time`                 DATETIME              DEFAULT NULL COMMENT '终态或冲突确认时间',
    `cancelled_by`                   VARCHAR(64)           DEFAULT NULL COMMENT '取消操作人',
    `cancelled_time`                 DATETIME              DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`                  VARCHAR(512)          DEFAULT NULL COMMENT '取消原因',
    `version`                        INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payout_order_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_payout_order_settlement` (`tenant_id`, `settlement_order_sn`),
    UNIQUE KEY `uk_payout_order_external` (`tenant_id`, `channel_ref`, `external_reference`),
    KEY `idx_payout_order_status` (`tenant_id`, `status`, `gmt_modified`),
    KEY `idx_payout_order_completion` (`tenant_id`, `completion_funds_transaction_sn`),
    KEY `idx_payout_order_rollback` (`tenant_id`, `rollback_funds_transaction_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '出款单事实表';

-- 出款回单事实表：保留多次规范化回单并约束来源幂等
CREATE TABLE `t_payout_receipt`
(
    `id`                         BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                         VARCHAR(64)  NOT NULL COMMENT '回单流水号',
    `tenant_id`                  BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `payout_order_sn`            VARCHAR(64)  NOT NULL COMMENT '出款单流水号',
    `channel_ref`                VARCHAR(128) NOT NULL COMMENT '宿主通道引用',
    `external_receipt_ref`       VARCHAR(128) NOT NULL COMMENT '外部回单唯一引用',
    `external_reference`         VARCHAR(128) NOT NULL COMMENT '外部出款 reference',
    `status`                     VARCHAR(30)  NOT NULL COMMENT '归一化回单状态',
    `amount`                     BIGINT(20)   NOT NULL COMMENT '回单金额，最小货币单位',
    `currency`                   VARCHAR(10)  NOT NULL COMMENT '回单币种',
    `source_receipt_digest`      VARCHAR(64)  NOT NULL COMMENT '来源回单 SHA-256',
    `normalized_receipt_digest`  VARCHAR(64)  NOT NULL COMMENT '规范化回单 SHA-256',
    `evidence_ref`               VARCHAR(256) NOT NULL COMMENT '受控回单证据引用',
    `external_occurred_at`       DATETIME     NOT NULL COMMENT '外部发生时间',
    `received_by`                VARCHAR(64)  NOT NULL COMMENT '记录人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payout_receipt_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_payout_receipt_source` (`tenant_id`, `channel_ref`, `external_receipt_ref`),
    KEY `idx_payout_receipt_order` (`tenant_id`, `payout_order_sn`, `id`),
    KEY `idx_payout_receipt_external` (`tenant_id`, `channel_ref`, `external_reference`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '出款回单事实表';

-- 追偿单表：只记录已确认责任和累计追偿事实，不承载追偿策略或资金执行
CREATE TABLE `t_recovery_order`
(
    `id`                        BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                        VARCHAR(64)  NOT NULL COMMENT '追偿单流水号',
    `tenant_id`                 BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `source_type`               VARCHAR(50)  NOT NULL COMMENT '已确认责任来源类型',
    `source_sn`                 VARCHAR(64)  NOT NULL COMMENT '已确认责任来源流水号',
    `responsible_subject_type`  VARCHAR(50)  NOT NULL COMMENT '责任资金主体类型',
    `responsible_subject_id`    VARCHAR(64)  NOT NULL COMMENT '责任资金主体 ID',
    `expected_amount`           BIGINT(20)   NOT NULL COMMENT '应追金额，最小货币单位',
    `recovered_amount`          BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '已追金额，最小货币单位',
    `currency`                  VARCHAR(10)  NOT NULL COMMENT '币种',
    `status`                    VARCHAR(30)  NOT NULL COMMENT 'CREATED/PARTIALLY_RECOVERED/RECOVERED',
    `source_digest`             VARCHAR(64)  NOT NULL COMMENT '已确认责任来源 SHA-256',
    `order_digest`              VARCHAR(64)  NOT NULL COMMENT '追偿单创建事实 SHA-256',
    `approval_ref`              VARCHAR(128) NOT NULL COMMENT '责任确认审批引用',
    `evidence_ref`              VARCHAR(256) NOT NULL COMMENT '责任确认受控证据引用',
    `last_funds_transaction_sn` VARCHAR(64)           DEFAULT NULL COMMENT '最近登记的已完成资金交易流水号',
    `created_by`                VARCHAR(64)  NOT NULL COMMENT '创建人',
    `recovered_time`            DATETIME              DEFAULT NULL COMMENT '全额追偿完成时间',
    `version`                   INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recovery_order_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_recovery_order_source` (`tenant_id`, `source_type`, `source_sn`, `responsible_subject_type`, `responsible_subject_id`, `currency`),
    KEY `idx_recovery_order_subject_status` (`tenant_id`, `responsible_subject_type`, `responsible_subject_id`, `status`, `gmt_modified`),
    KEY `idx_recovery_order_last_transaction` (`tenant_id`, `last_funds_transaction_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '追偿责任事实表';

-- 追偿结果表：只追加引用宿主已完成的资金交易，不创建资金事实
CREATE TABLE `t_recovery_result`
(
    `id`                   BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sn`                   VARCHAR(64)  NOT NULL COMMENT '追偿结果流水号',
    `tenant_id`            BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `recovery_order_sn`    VARCHAR(64)  NOT NULL COMMENT '追偿单流水号',
    `funds_transaction_sn` VARCHAR(64)  NOT NULL COMMENT '已完成资金交易流水号',
    `amount`               BIGINT(20)   NOT NULL COMMENT '本次已追金额，最小货币单位',
    `currency`             VARCHAR(10)  NOT NULL COMMENT '币种',
    `idempotency_key`      VARCHAR(128) NOT NULL COMMENT '结果登记幂等键',
    `result_digest`        VARCHAR(64)  NOT NULL COMMENT '结果登记请求 SHA-256',
    `approval_ref`         VARCHAR(128) NOT NULL COMMENT '结果确认审批引用',
    `evidence_ref`         VARCHAR(256) NOT NULL COMMENT '结果确认受控证据引用',
    `recorded_by`          VARCHAR(64)  NOT NULL COMMENT '登记人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recovery_result_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_recovery_result_transaction` (`tenant_id`, `funds_transaction_sn`),
    UNIQUE KEY `uk_recovery_result_idempotency` (`tenant_id`, `idempotency_key`),
    KEY `idx_recovery_result_order` (`tenant_id`, `recovery_order_sn`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '追偿资金结果事实表';
