-- ----------------------------
-- 资金账户表
-- ----------------------------
DROP TABLE IF EXISTS `t_funding_account`;
CREATE TABLE `t_funding_account`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '资金账户号，全局唯一',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `owner_id`               VARCHAR(30)  NOT NULL COMMENT '账户归属主体 ID',
    `owner_type`             VARCHAR(50)  NOT NULL COMMENT '账户归属主体类型',
    `account_type`           VARCHAR(50)  NOT NULL COMMENT '资金账户类型',
    `is_platform`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否平台账户',
    `account_role_code`      VARCHAR(50)           DEFAULT NULL COMMENT '平台账户角色',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `ledger_profile_code`    VARCHAR(50)  NOT NULL COMMENT 'ledger profile 编码',
    `ledger_profile_version` INT(11)      NOT NULL DEFAULT 1 COMMENT 'profile 版本',
    `status`                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`            VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`      TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    `version`                INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_funding_account_sn` (`sn`),
    UNIQUE KEY `uk_funding_account_platform_role` (`tenant_id`, `currency`, `account_role_code`),
    KEY `idx_funding_account_owner` (`owner_type`, `owner_id`),
    KEY `idx_funding_account_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '资金账户表';

-- ----------------------------
-- 信用账户表
-- ----------------------------
DROP TABLE IF EXISTS `t_credit_account`;
CREATE TABLE `t_credit_account`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '信用账户号，全局唯一',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `owner_id`               VARCHAR(30)  NOT NULL COMMENT '归属主体 ID',
    `owner_type`             VARCHAR(50)  NOT NULL COMMENT '归属主体类型',
    `account_type`           VARCHAR(50)  NOT NULL COMMENT '信用账户类型',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `period_type`            VARCHAR(20)  NOT NULL DEFAULT 'LIFETIME' COMMENT '账本周期类型',
    `period_id`              VARCHAR(30)  NOT NULL DEFAULT 'LIFETIME' COMMENT '账本周期 ID',
    `ledger_profile_code`    VARCHAR(50)  NOT NULL COMMENT 'ledger profile 编码',
    `ledger_profile_version` INT(11)      NOT NULL DEFAULT 1 COMMENT 'profile 版本',
    `status`                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`            VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`      TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    `version`                INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_account_sn` (`sn`),
    KEY `idx_credit_account_owner` (`owner_type`, `owner_id`),
    KEY `idx_credit_account_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '信用账户表';

-- ----------------------------
-- 账户层级关系表
-- ----------------------------
DROP TABLE IF EXISTS `t_account_hierarchy_relation`;
CREATE TABLE `t_account_hierarchy_relation`
(
    `id`                     BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64) NOT NULL COMMENT '关系号，全局唯一',
    `tenant_id`              BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `account_id`             VARCHAR(64) NOT NULL COMMENT '子账户 ID',
    `account_type`           VARCHAR(50) NOT NULL COMMENT '子账户主体类型',
    `parent_account_id`      VARCHAR(64) NOT NULL COMMENT '父账户 ID',
    `parent_account_type`    VARCHAR(50) NOT NULL COMMENT '父账户主体类型',
    `currency`               VARCHAR(10) NOT NULL COMMENT '币种',
    `operator_id`            VARCHAR(64)          DEFAULT NULL COMMENT '操作者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_hierarchy_relation_sn` (`sn`),
    UNIQUE KEY `uk_account_hierarchy_relation_account` (`tenant_id`, `account_type`, `account_id`),
    KEY `idx_account_hierarchy_relation_account` (`tenant_id`, `account_type`, `account_id`),
    KEY `idx_account_hierarchy_relation_parent` (`tenant_id`, `parent_account_type`, `parent_account_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户层级关系表';

-- ----------------------------
-- 支出控制范围表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_control_scope`;
CREATE TABLE `t_spend_control_scope`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '支出控制范围号，全局唯一',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `owner_id`               VARCHAR(30)  NOT NULL COMMENT '归属主体 ID',
    `owner_type`             VARCHAR(50)  NOT NULL COMMENT '归属主体类型',
    `scope_type`             VARCHAR(50)  NOT NULL COMMENT '控制范围类型',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `period_type`            VARCHAR(20)  NOT NULL DEFAULT 'LIFETIME' COMMENT '周期类型',
    `period_id`              VARCHAR(30)  NOT NULL DEFAULT 'LIFETIME' COMMENT '周期 ID',
    `period_policy`          VARCHAR(50)           DEFAULT NULL COMMENT '周期策略',
    `status`                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`            VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`      TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    `version`                INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_control_scope_sn` (`sn`),
    KEY `idx_spend_control_scope_owner` (`owner_type`, `owner_id`),
    KEY `idx_spend_control_scope_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支出控制范围表';

-- ----------------------------
-- 支付工具表
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_instrument`;
CREATE TABLE `t_payment_instrument`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '工具号，全局唯一',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `owner_id`               VARCHAR(30)  NOT NULL COMMENT '工具归属主体 ID',
    `owner_type`             VARCHAR(50)  NOT NULL COMMENT '工具归属主体类型',
    `instrument_type`        VARCHAR(50)  NOT NULL COMMENT '工具类型',
    `instrument_direction`   VARCHAR(50)  NOT NULL COMMENT '工具资金流向：INBOUND/OUTBOUND/BIDIRECTIONAL',
    `instrument_no`          VARCHAR(128) NOT NULL COMMENT '工具展示号、掩码号、别名号或稳定识别号',
    `channel_code`           VARCHAR(50)           DEFAULT NULL COMMENT '通道编码',
    `external_instrument_id` VARCHAR(128)          DEFAULT NULL COMMENT '外部工具 ID',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `status`                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `valid_from`             DATETIME              DEFAULT NULL COMMENT '生效时间',
    `valid_to`               DATETIME              DEFAULT NULL COMMENT '失效时间',
    `description`            VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`      TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_sn` (`sn`),
    UNIQUE KEY `uk_payment_instrument_external` (`tenant_id`, `channel_code`, `external_instrument_id`),
    KEY `idx_payment_instrument_owner` (`owner_type`, `owner_id`),
    KEY `idx_payment_instrument_status` (`status`),
    KEY `idx_payment_instrument_no` (`instrument_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具表';

-- ----------------------------
-- 支付工具绑定表
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_instrument_binding`;
CREATE TABLE `t_payment_instrument_binding`
(
    `id`                  BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                  VARCHAR(64) NOT NULL COMMENT '绑定号，全局唯一',
    `tenant_id`           BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `instrument_sn`       VARCHAR(64) NOT NULL COMMENT '工具号',
    `binding_role`        VARCHAR(50) NOT NULL COMMENT '绑定角色',
    `subject_id`          VARCHAR(64) NOT NULL COMMENT '内部主体 ID',
    `subject_type`        VARCHAR(50) NOT NULL COMMENT '内部主体类型',
    `currency`            VARCHAR(10) NOT NULL COMMENT '币种',
    `priority`            INT(11)     NOT NULL DEFAULT 0 COMMENT '路由优先级',
    `is_default`          TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否默认绑定',
    `status`              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '绑定生命周期状态',
    `version`             INT(11)     NOT NULL DEFAULT 1 COMMENT '绑定版本',
    `valid_from`          DATETIME             DEFAULT NULL COMMENT '生效时间',
    `valid_to`            DATETIME             DEFAULT NULL COMMENT '失效时间',
    `description`         VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`   TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_binding_sn` (`sn`),
    UNIQUE KEY `uk_payment_instrument_binding_subject` (`tenant_id`, `instrument_sn`, `binding_role`, `subject_type`, `subject_id`, `currency`),
    KEY `idx_payment_instrument_binding_instrument` (`instrument_sn`),
    KEY `idx_payment_instrument_binding_subject` (`subject_type`, `subject_id`),
    KEY `idx_payment_instrument_binding_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具绑定表';

-- ----------------------------
-- 支付工具绑定历史表
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_instrument_binding_history`;
CREATE TABLE `t_payment_instrument_binding_history`
(
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                  VARCHAR(64)  NOT NULL COMMENT '审计号，全局唯一',
    `tenant_id`           BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `binding_sn`          VARCHAR(64)  NOT NULL COMMENT '绑定号',
    `instrument_sn`       VARCHAR(64)  NOT NULL COMMENT '工具号',
    `change_type`         VARCHAR(50)  NOT NULL COMMENT '变更类型',
    `version`             INT(11)      NOT NULL COMMENT '绑定版本',
    `before_snapshot`     TEXT                  DEFAULT NULL COMMENT '变更前快照',
    `after_snapshot`      TEXT                  DEFAULT NULL COMMENT '变更后快照，解绑时为空',
    `operator_id`         VARCHAR(64)  NOT NULL COMMENT '操作者',
    `change_reason`       VARCHAR(256) NOT NULL COMMENT '变更原因',
    `effective_at`        DATETIME              DEFAULT NULL COMMENT '变更事实生效时间',
    `context_variables`   TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_binding_history_sn` (`sn`),
    UNIQUE KEY `uk_payment_instrument_binding_history_version` (`binding_sn`, `version`),
    KEY `idx_payment_instrument_binding_history_binding` (`binding_sn`),
    KEY `idx_payment_instrument_binding_history_instrument` (`instrument_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具绑定历史表';

-- ----------------------------
-- 支出主体资金关系表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_subject_funding_rel`;
CREATE TABLE `t_spend_subject_funding_rel`
(
    `id`                  BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                  VARCHAR(64) NOT NULL COMMENT '关系号，全局唯一',
    `tenant_id`           BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `spend_subject_id`    VARCHAR(64) NOT NULL COMMENT '支出控制主体 ID',
    `spend_subject_type`  VARCHAR(50) NOT NULL COMMENT '支出控制主体类型',
    `target_subject_type` VARCHAR(50) NOT NULL COMMENT '资金责任目标主体类型',
    `target_subject_id`   VARCHAR(64) NOT NULL COMMENT '资金责任目标主体 ID',
    `currency`            VARCHAR(10) NOT NULL COMMENT '币种',
    `relation_type`       VARCHAR(50) NOT NULL COMMENT '关系类型',
    `description`         VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_subject_funding_rel_sn` (`sn`),
    UNIQUE KEY `uk_spend_subject_funding_rel_scope` (`tenant_id`, `spend_subject_type`, `spend_subject_id`, `currency`, `relation_type`),
    KEY `idx_spend_subject_funding_rel_spend_subject` (`spend_subject_type`, `spend_subject_id`),
    KEY `idx_spend_subject_funding_rel_target` (`target_subject_type`, `target_subject_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支出主体资金关系表';

-- ----------------------------
-- Spend Rule 定义表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_rule_definition`;
CREATE TABLE `t_spend_rule_definition`
(
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`      BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `rule_id`        VARCHAR(64)  NOT NULL COMMENT 'Spend Rule 标识',
    `rule_name`      VARCHAR(128) NOT NULL COMMENT 'Spend Rule 名称',
    `rule_type`      VARCHAR(50)  NOT NULL COMMENT 'Spend Rule 类型',
    `rule_domain`    VARCHAR(50)  NOT NULL COMMENT 'Spend Rule 规则域',
    `status`         VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`    VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_rule_definition_rule` (`tenant_id`, `rule_id`),
    KEY `idx_spend_rule_definition_domain` (`tenant_id`, `rule_domain`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'Spend Rule 定义表';

-- ----------------------------
-- Spend Rule 版本表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_rule_version`;
CREATE TABLE `t_spend_rule_version`
(
    `id`                 BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`          BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `rule_id`            VARCHAR(64) NOT NULL COMMENT 'Spend Rule 标识',
    `rule_version`       VARCHAR(64) NOT NULL COMMENT 'Spend Rule 版本',
    `rule_spec`          TEXT        NOT NULL COMMENT '规则规格 JSON',
    `rule_digest`        VARCHAR(128) NOT NULL COMMENT '规则规格摘要',
    `status`             VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态',
    `operator_id`        VARCHAR(64) NOT NULL COMMENT '操作者',
    `audit_reference_sn` VARCHAR(128) NOT NULL COMMENT '审计引用',
    `description`        VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_rule_version_rule` (`tenant_id`, `rule_id`, `rule_version`),
    KEY `idx_spend_rule_version_status` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'Spend Rule 版本表';

-- ----------------------------
-- Spend Rule 挂载表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_rule_binding`;
CREATE TABLE `t_spend_rule_binding`
(
    `id`              BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`       BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `sn`              VARCHAR(64) NOT NULL COMMENT '规则挂载流水号',
    `rule_id`         VARCHAR(64) NOT NULL COMMENT 'Spend Rule 标识',
    `rule_version`    VARCHAR(64) NOT NULL COMMENT 'Spend Rule 版本',
    `scope_type`      VARCHAR(50) NOT NULL COMMENT '挂载范围类型',
    `scope_id`        VARCHAR(64) NOT NULL COMMENT '挂载范围标识',
    `priority`        INT(11)     NOT NULL DEFAULT 0 COMMENT '挂载优先级',
    `conflict_policy` VARCHAR(50) NOT NULL COMMENT '挂载冲突策略',
    `effective_from`  DATETIME    NOT NULL COMMENT '生效开始时间',
    `effective_to`    DATETIME    NOT NULL COMMENT '生效结束时间',
    `status`          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `audit_reference_sn` VARCHAR(128) NOT NULL COMMENT '审计引用',
    `description`     VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_rule_binding_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_spend_rule_binding_scope` (`tenant_id`, `scope_type`, `scope_id`, `rule_id`, `rule_version`, `audit_reference_sn`),
    KEY `idx_spend_rule_binding_rule` (`tenant_id`, `rule_id`, `rule_version`, `status`),
    KEY `idx_spend_rule_binding_scope` (`tenant_id`, `scope_type`, `scope_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'Spend Rule 挂载表';

-- ----------------------------
-- Spend Rule 决策记录表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_rule_decision_record`;
CREATE TABLE `t_spend_rule_decision_record`
(
    `id`              BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `tenant_id`       BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `decision_sn`     VARCHAR(64) NOT NULL COMMENT '规则决策流水号',
    `rule_id`         VARCHAR(64) NOT NULL COMMENT 'Spend Rule 标识',
    `rule_version`    VARCHAR(64) NOT NULL COMMENT 'Spend Rule 版本',
    `spend_rule_binding_sn` VARCHAR(64)    DEFAULT NULL COMMENT '规则挂载流水号',
    `scope_type`      VARCHAR(50) NOT NULL COMMENT '控制范围类型',
    `scope_id`        VARCHAR(64) NOT NULL COMMENT '控制范围标识',
    `instrument_sn`   VARCHAR(64)          DEFAULT NULL COMMENT '支付工具号',
    `instrument_binding_version` INT       DEFAULT NULL COMMENT '支付工具绑定版本',
    `control_scope_id` VARCHAR(64)          DEFAULT NULL COMMENT '预算控制范围标识',
    `period_id`       VARCHAR(64)           DEFAULT NULL COMMENT '预算控制周期标识',
    `target_subject_id` VARCHAR(64)         DEFAULT NULL COMMENT '规则评估目标资金主体 ID',
    `target_subject_type` VARCHAR(50)       DEFAULT NULL COMMENT '规则评估目标资金主体类型',
    `action`          VARCHAR(50) NOT NULL COMMENT '支付工具动作',
    `amount`          BIGINT(20)  NOT NULL COMMENT '交易金额',
    `currency`        VARCHAR(10) NOT NULL COMMENT '币种',
    `business_scene`  VARCHAR(50) NOT NULL COMMENT '业务场景',
    `business_sn`     VARCHAR(64) NOT NULL COMMENT '业务流水号',
    `decision_result` VARCHAR(50) NOT NULL COMMENT '规则决策结果',
    `reject_reason`   VARCHAR(512)         DEFAULT NULL COMMENT '拒绝原因',
    `decision_digest` VARCHAR(128) NOT NULL COMMENT '规则决策摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_rule_decision_record_sn` (`tenant_id`, `decision_sn`),
    KEY `idx_spend_rule_decision_record_business` (`tenant_id`, `business_scene`, `business_sn`),
    KEY `idx_spend_rule_decision_record_rule` (`tenant_id`, `rule_id`, `rule_version`),
    KEY `idx_spend_rule_decision_record_scope` (`tenant_id`, `scope_type`, `scope_id`),
    KEY `idx_spend_rule_decision_record_binding` (`tenant_id`, `spend_rule_binding_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'Spend Rule 决策记录表';

-- ----------------------------
-- 支出控制额度变动表
-- ----------------------------
DROP TABLE IF EXISTS `t_spend_control_movement`;
CREATE TABLE `t_spend_control_movement`
(
    `id`                    BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `movement_sn`           VARCHAR(64) NOT NULL COMMENT '控制额度变动流水号',
    `tenant_id`             BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `movement_type`         VARCHAR(50) NOT NULL COMMENT '控制额度变动类型',
    `business_scene`        VARCHAR(50) NOT NULL COMMENT '业务场景',
    `business_sn`           VARCHAR(64) NOT NULL COMMENT '业务号',
    `original_movement_sn`  VARCHAR(64)          DEFAULT NULL COMMENT '原支出控制额度变动流水号',
    `transaction_sn`        VARCHAR(64)          DEFAULT NULL COMMENT '资金交易流水号',
    `instrument_sn`         VARCHAR(64)          DEFAULT NULL COMMENT '支付工具号',
    `action`                VARCHAR(50)          DEFAULT NULL COMMENT '支付工具动作',
    `target_subject_id`     VARCHAR(64) NOT NULL COMMENT '目标资金主体 ID',
    `target_subject_type`   VARCHAR(50) NOT NULL COMMENT '目标资金主体类型',
    `amount`                BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '控制金额',
    `currency`              VARCHAR(10) NOT NULL COMMENT '币种',
    `spend_rule_id`         VARCHAR(64) NOT NULL COMMENT 'Spend Rule 标识',
    `spend_rule_version`    VARCHAR(64) NOT NULL COMMENT 'Spend Rule 版本',
    `spend_decision_sn`     VARCHAR(64)          DEFAULT NULL COMMENT 'Spend Rule 决策流水号',
    `spend_decision_result` VARCHAR(50)          DEFAULT NULL COMMENT 'Spend Rule 决策结果',
    `spend_decision_digest` VARCHAR(128)         DEFAULT NULL COMMENT 'Spend Rule 决策摘要',
    `control_scope_id`      VARCHAR(64)          DEFAULT NULL COMMENT '控制范围标识',
    `period_id`             VARCHAR(64)          DEFAULT NULL COMMENT '控制周期标识',
    `reject_reason`         VARCHAR(512)         DEFAULT NULL COMMENT '拒绝原因',
    `reason_code`           VARCHAR(64)          DEFAULT NULL COMMENT '调整原因码',
    `operator_id`           VARCHAR(64)          DEFAULT NULL COMMENT '操作者或系统来源',
    `audit_reference_sn`    VARCHAR(128)         DEFAULT NULL COMMENT '审批、凭证、规则发布或外部审计引用',
    `movement_digest`       VARCHAR(128) NOT NULL COMMENT '控制额度变动摘要',
    `description`           VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`     TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_control_movement_sn` (`tenant_id`, `movement_sn`),
    KEY `idx_spend_control_movement_business` (`tenant_id`, `business_scene`, `business_sn`),
    KEY `idx_spend_control_movement_original` (`tenant_id`, `original_movement_sn`),
    KEY `idx_spend_control_movement_transaction` (`tenant_id`, `transaction_sn`),
    KEY `idx_spend_control_movement_instrument` (`tenant_id`, `instrument_sn`, `action`),
    KEY `idx_spend_control_movement_target` (`tenant_id`, `target_subject_type`, `target_subject_id`),
    KEY `idx_spend_control_movement_scope` (`tenant_id`, `control_scope_id`, `period_id`, `currency`),
    KEY `idx_spend_control_movement_rule` (`tenant_id`, `spend_rule_id`, `spend_rule_version`),
    KEY `idx_spend_control_movement_rolling_count` (`tenant_id`, `control_scope_id`, `currency`, `spend_rule_id`, `spend_rule_version`, `target_subject_type`, `target_subject_id`, `gmt_create`),
    KEY `idx_spend_control_movement_created` (`gmt_create`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支出控制额度变动表';

-- ----------------------------
-- 资金交易表
-- ----------------------------
DROP TABLE IF EXISTS `t_funds_transaction`;
CREATE TABLE `t_funds_transaction`
(
    `id`                       BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                       VARCHAR(64) NOT NULL COMMENT '交易号，全局唯一',
    `tenant_id`                BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `transaction_mode`         VARCHAR(50) NOT NULL COMMENT 'DIRECT/AUTHORIZATION/BALANCE_CONTROL',
    `transaction_type`         VARCHAR(50) NOT NULL COMMENT '交易类型',
    `business_scene`           VARCHAR(50) NOT NULL COMMENT '业务场景',
    `business_sn`              VARCHAR(64) NOT NULL COMMENT '业务号',
    `external_source_code`     VARCHAR(128)         DEFAULT NULL COMMENT '外部资金事实来源命名空间',
    `external_funds_fact_sn`   VARCHAR(128)         DEFAULT NULL COMMENT '外部资金事实流水号',
    `external_funds_effect_type` VARCHAR(50)         DEFAULT NULL COMMENT '外部资金事实作用类型',
    `external_funds_fact_digest` VARCHAR(64)         DEFAULT NULL COMMENT '外部资金事实不可变载荷摘要',
    `reference_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '引用交易号',
    `status`                   VARCHAR(50) NOT NULL DEFAULT 'CREATED' COMMENT '交易状态',
    `amount`                   BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '原始交易金额',
    `currency`                 VARCHAR(10) NOT NULL COMMENT '币种',
    `authorized_amount`        BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计授权金额',
    `reversed_amount`          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计撤销金额',
    `completed_amount`           BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计完成金额',
    `refunded_amount`          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计退款金额',
    `declined_amount`          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计拒付/拒绝金额',
    `fee_amount`               BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计手续费金额',
    `route_snapshot`           TEXT                 DEFAULT NULL COMMENT 'RouteSnapshot JSON',
    `description`              VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`        TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    `version`                  INT(11)     NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_funds_transaction_sn` (`sn`),
    UNIQUE KEY `uk_funds_transaction_business` (`tenant_id`, `business_scene`, `business_sn`),
    UNIQUE KEY `uk_funds_transaction_external_fact` (`tenant_id`, `external_source_code`, `external_funds_fact_sn`, `external_funds_effect_type`),
    KEY `idx_funds_transaction_tenant` (`tenant_id`),
    KEY `idx_funds_transaction_reference` (`reference_transaction_sn`),
    KEY `idx_funds_transaction_status` (`status`),
    KEY `idx_funds_transaction_projection_scan` (`tenant_id`, `id`),
    KEY `idx_funds_transaction_gmt_create` (`gmt_create`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '资金交易表';

-- ----------------------------
-- 资金交易明细表
-- ----------------------------
DROP TABLE IF EXISTS `t_funds_transaction_detail`;
CREATE TABLE `t_funds_transaction_detail`
(
    `id`                              BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                              VARCHAR(64) NOT NULL COMMENT '明细号，全局唯一',
    `tenant_id`                       BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `transaction_sn`                  VARCHAR(64) NOT NULL COMMENT '业务交易号',
    `business_scene`                  VARCHAR(50) NOT NULL COMMENT '本次业务动作场景',
    `business_sn`                     VARCHAR(64) NOT NULL COMMENT '本次业务动作号',
    `transaction_type`                VARCHAR(50) NOT NULL COMMENT '交易类型',
    `event_type`                      VARCHAR(50) NOT NULL COMMENT '生命周期事件',
    `subject_id`                      VARCHAR(64) NOT NULL COMMENT '影响主体 ID',
    `subject_type`                    VARCHAR(50) NOT NULL COMMENT '影响主体类型',
    `participant_role`                VARCHAR(50) NOT NULL COMMENT '参与方角色',
    `request_hash`                    VARCHAR(64) NOT NULL COMMENT '请求参数一致性摘要',
    `funds_effect_type`               VARCHAR(50) NOT NULL COMMENT '资金业务效果',
    `ledger_transaction_sn`           VARCHAR(64)          DEFAULT NULL COMMENT '账本交易号',
    `reference_detail_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '引用明细号',
    `reference_ledger_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '引用账本交易号',
    `amount`                          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '本次动作金额',
    `currency`                        VARCHAR(10) NOT NULL COMMENT '币种',
    `status`                          VARCHAR(50) NOT NULL DEFAULT 'CREATED' COMMENT '明细状态',
    `error_code`                      VARCHAR(64)          DEFAULT NULL COMMENT '错误码',
    `error_message`                   VARCHAR(512)         DEFAULT NULL COMMENT '错误消息',
    `description`                     VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`               TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_funds_transaction_detail_sn` (`sn`),
    UNIQUE KEY `uk_funds_transaction_detail_event_subject` (`tenant_id`, `transaction_sn`, `business_scene`, `business_sn`, `event_type`, `subject_type`, `subject_id`, `participant_role`, `funds_effect_type`),
    KEY `idx_funds_transaction_detail_business_event` (`tenant_id`, `business_scene`, `business_sn`, `transaction_type`, `event_type`),
    KEY `idx_funds_transaction_detail_transaction` (`transaction_sn`),
    KEY `idx_funds_transaction_detail_subject` (`subject_type`, `subject_id`, `participant_role`),
    KEY `idx_funds_transaction_detail_ledger` (`ledger_transaction_sn`),
    KEY `idx_funds_transaction_detail_reference` (`reference_detail_sn`),
    KEY `idx_funds_transaction_detail_status` (`status`),
    KEY `idx_funds_transaction_detail_business` (`business_scene`, `business_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '资金交易明细表';

-- ----------------------------
-- 资金冻结订单表
-- ----------------------------
DROP TABLE IF EXISTS `t_funds_frozen_order`;
CREATE TABLE `t_funds_frozen_order`
(
    `id`                           BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                 DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                           VARCHAR(64) NOT NULL COMMENT '冻结单号，全局唯一',
    `tenant_id`                    BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `subject_id`                   VARCHAR(64) NOT NULL COMMENT '被冻结主体 ID',
    `subject_type`                 VARCHAR(50) NOT NULL COMMENT '被冻结主体类型',
    `freeze_type`                  VARCHAR(50) NOT NULL COMMENT '冻结类型',
    `business_scene`               VARCHAR(50) NOT NULL COMMENT '业务场景',
    `business_sn`                  VARCHAR(64) NOT NULL COMMENT '外部业务号',
    `transaction_sn`               VARCHAR(64)          DEFAULT NULL COMMENT '关联资金交易号',
    `freeze_detail_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '冻结动作明细号',
    `freeze_ledger_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '冻结账本交易号',
    `amount`                       BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '原冻结金额',
    `released_amount`              BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '已释放金额',
    `currency`                     VARCHAR(10) NOT NULL COMMENT '币种',
    `status`                       VARCHAR(50) NOT NULL DEFAULT 'CREATED' COMMENT '冻结单状态',
    `expire_time`                  DATETIME             DEFAULT NULL COMMENT '过期时间',
    `release_time`                 DATETIME             DEFAULT NULL COMMENT '完全释放时间',
    `description`                  VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`            TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    `version`                      INT(11)     NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_funds_frozen_order_sn` (`sn`),
    UNIQUE KEY `uk_funds_frozen_order_business` (`tenant_id`, `business_scene`, `business_sn`, `freeze_type`),
    KEY `idx_funds_frozen_order_subject` (`subject_type`, `subject_id`),
    KEY `idx_funds_frozen_order_transaction` (`transaction_sn`),
    KEY `idx_funds_frozen_order_status` (`status`),
    KEY `idx_funds_frozen_order_expire` (`expire_time`, `status`),
    KEY `idx_funds_frozen_order_projection_scan` (`tenant_id`, `id`),
    KEY `idx_funds_frozen_order_business` (`business_scene`, `business_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '资金冻结订单表';

-- ----------------------------
-- 账户账本表
-- ----------------------------
DROP TABLE IF EXISTS `t_ledger`;
CREATE TABLE `t_ledger`
(
    `id`                     BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`              BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `subject_id`             VARCHAR(64) NOT NULL COMMENT '账务主体 ID',
    `subject_type`           VARCHAR(50) NOT NULL COMMENT '账务主体类型',
    `ledger_profile_code`    VARCHAR(50) NOT NULL COMMENT 'profile 编码快照',
    `ledger_profile_version` INT(11)     NOT NULL DEFAULT 1 COMMENT 'profile 版本快照',
    `ledger_subject_code`    VARCHAR(50) NOT NULL COMMENT '账本科目编码',
    `ledger_subject_category` VARCHAR(50) NOT NULL COMMENT '科目分类快照',
    `normal_balance_side`    VARCHAR(10) NOT NULL COMMENT '正常余额方向',
    `is_allow_negative`      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否允许负余额',
    `currency`               VARCHAR(10) NOT NULL COMMENT '币种',
    `period_type`            VARCHAR(20) NOT NULL DEFAULT 'LIFETIME' COMMENT '周期类型',
    `period_id`              VARCHAR(30) NOT NULL DEFAULT 'LIFETIME' COMMENT '周期 ID',
    `settlement_policy`      VARCHAR(50) NOT NULL DEFAULT 'RT' COMMENT '结算策略',
    `cut_off_time`           TIME        NOT NULL DEFAULT '00:00:00' COMMENT '日切时间',
    `debit_amount`           BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '借方累计金额',
    `credit_amount`          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '贷方累计金额',
    `status`                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `version`                INT(11)     NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_subject_bucket` (`tenant_id`, `subject_type`, `subject_id`, `ledger_subject_code`, `currency`, `period_type`, `period_id`),
    KEY `idx_ledger_subject` (`subject_type`, `subject_id`),
    KEY `idx_ledger_profile` (`ledger_profile_code`, `ledger_profile_version`),
    KEY `idx_ledger_period` (`period_type`, `period_id`),
    KEY `idx_ledger_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户账本表';

-- ----------------------------
-- 账户账本交易表
-- ----------------------------
DROP TABLE IF EXISTS `t_ledger_transaction`;
CREATE TABLE `t_ledger_transaction`
(
    `id`                              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                              VARCHAR(64)   NOT NULL COMMENT '账本交易号，全局唯一',
    `tenant_id`                       BIGINT(20)    NOT NULL COMMENT '租户 ID',
    `funds_transaction_sn`            VARCHAR(64)            DEFAULT NULL COMMENT '业务交易号',
    `reference_ledger_transaction_sn` VARCHAR(64)            DEFAULT NULL COMMENT '引用账本交易号',
    `instruction_type`                VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '指令类型',
    `event_type`                      VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '事件类型',
    `transaction_type`                VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '交易类型',
    `business_scene`                  VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '业务场景',
    `business_sn`                     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '业务号',
    `amount`                          BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '本次交易金额',
    `currency`                        VARCHAR(10)   NOT NULL DEFAULT '' COMMENT '本次交易币种',
    `original_amount`                 BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '原始金额',
    `original_currency`               VARCHAR(10)   NOT NULL DEFAULT '' COMMENT '原始币种',
    `exchange_rate`                   DECIMAL(18,8) NOT NULL DEFAULT 1.00000000 COMMENT '汇率',
    `debit_amount`                    BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '本交易借方合计',
    `credit_amount`                   BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '本交易贷方合计',
    `transaction_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易发生时间',
    `description`                     VARCHAR(512)           DEFAULT NULL COMMENT '描述',
    `context_variables`               TEXT                   DEFAULT NULL COMMENT '上下文变量',
    `sha256`                          VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '防篡改摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_transaction_sn` (`sn`),
    KEY `idx_ledger_transaction_funds_transaction` (`funds_transaction_sn`),
    KEY `idx_ledger_transaction_reference` (`reference_ledger_transaction_sn`),
    KEY `idx_ledger_transaction_business` (`business_scene`, `business_sn`),
    KEY `idx_ledger_transaction_event` (`event_type`),
    KEY `idx_ledger_transaction_time` (`transaction_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户账本交易表';

-- ----------------------------
-- 账户账本记账计划表
-- ----------------------------
DROP TABLE IF EXISTS `t_ledger_posting_plan`;
CREATE TABLE `t_ledger_posting_plan`
(
    `id`                          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                          VARCHAR(64)  NOT NULL COMMENT '记账计划号，全局唯一',
    `tenant_id`                   BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `ledger_transaction_sn`       VARCHAR(64)  NOT NULL COMMENT '账本交易号',
    `funds_transaction_sn`        VARCHAR(64)           DEFAULT NULL COMMENT '业务交易号',
    `route_leg_id`                VARCHAR(64)           DEFAULT NULL COMMENT '来源 route leg',
    `intent`                      VARCHAR(50)  NOT NULL COMMENT '记账意图',
    `posting_scope`               VARCHAR(50)  NOT NULL COMMENT '记账范围',
    `balance_effect_type`         VARCHAR(50)  NOT NULL COMMENT '余额影响语义',
    `phase_code`                  VARCHAR(50)  NOT NULL COMMENT '记账阶段',
    `amount`                      BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '计划金额',
    `currency`                    VARCHAR(10)  NOT NULL COMMENT '币种',
    `debit_amount`                BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '借方合计',
    `credit_amount`               BIGINT(20)   NOT NULL DEFAULT 0 COMMENT '贷方合计',
    `description`                 VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`           TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    `sha256`                      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '防篡改摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_posting_plan_sn` (`sn`),
    KEY `idx_ledger_posting_plan_transaction` (`ledger_transaction_sn`),
    KEY `idx_ledger_posting_plan_funds_transaction` (`funds_transaction_sn`),
    KEY `idx_ledger_posting_plan_route_leg` (`route_leg_id`),
    KEY `idx_ledger_posting_plan_phase` (`phase_code`),
    KEY `idx_ledger_posting_plan_intent` (`intent`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户账本记账计划表';

-- ----------------------------
-- 账户账本条目表
-- ----------------------------
DROP TABLE IF EXISTS `t_ledger_entry`;
CREATE TABLE `t_ledger_entry`
(
    `id`                              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`                      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`                    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                              VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '分录号，全局唯一',
    `tenant_id`                       BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '租户 ID',
    `ledger_transaction_sn`           VARCHAR(64)   NOT NULL COMMENT '账本交易号',
    `posting_plan_sn`                 VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '记账计划号',
    `funds_transaction_sn`            VARCHAR(64)            DEFAULT NULL COMMENT '业务交易号',
    `ledger_id`                       BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '对应 t_ledger.id',
    `period_type`                     VARCHAR(20)   NOT NULL DEFAULT 'LIFETIME' COMMENT '账本周期类型快照',
    `period_id`                       VARCHAR(30)   NOT NULL DEFAULT 'LIFETIME' COMMENT '账本周期 ID 快照',
    `subject_id`                      VARCHAR(64)   NOT NULL COMMENT '账务主体 ID',
    `subject_type`                    VARCHAR(50)   NOT NULL COMMENT '账务主体类型',
    `ledger_subject_code`             VARCHAR(50)   NOT NULL COMMENT '账本科目编码',
    `ledger_subject_category`         VARCHAR(50)   NOT NULL COMMENT '科目分类快照',
    `entry_side`                      VARCHAR(10)   NOT NULL COMMENT '借贷方向',
    `posting_role`                    VARCHAR(50)   NOT NULL COMMENT '多级账户记账角色',
    `balance_constraint_type`         VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '本分录余额约束',
    `intent`                          VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '记账意图',
    `posting_scope`                   VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '记账范围',
    `balance_effect_type`             VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '余额影响语义',
    `phase_code`                      VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '记账阶段',
    `business_scene`                  VARCHAR(50)   NOT NULL COMMENT '业务场景',
    `business_sn`                     VARCHAR(64)            DEFAULT NULL COMMENT '业务号',
    `amount`                          BIGINT(20)    NOT NULL COMMENT '分录金额',
    `currency`                        VARCHAR(10)   NOT NULL COMMENT '币种',
    `original_amount`                 BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '原始金额',
    `original_currency`               VARCHAR(10)   NOT NULL DEFAULT '' COMMENT '原始币种',
    `exchange_rate`                   DECIMAL(18,8) NOT NULL DEFAULT 1.00000000 COMMENT '汇率',
    `transaction_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易发生时间',
    `description`                     VARCHAR(512)           DEFAULT NULL COMMENT '描述',
    `context_variables`               TEXT                   DEFAULT NULL COMMENT '扩展上下文',
    `sha256`                          VARCHAR(128)  NOT NULL COMMENT '防篡改摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_entry_sn` (`sn`),
    KEY `idx_ledger_entry_transaction` (`ledger_transaction_sn`),
    KEY `idx_ledger_entry_posting_plan` (`posting_plan_sn`),
    KEY `idx_ledger_entry_ledger` (`ledger_id`),
    KEY `idx_ledger_entry_period` (`period_type`, `period_id`),
    KEY `idx_ledger_entry_subject` (`subject_type`, `subject_id`),
    KEY `idx_ledger_entry_funds_transaction` (`funds_transaction_sn`),
    KEY `idx_ledger_entry_business` (`business_scene`, `business_sn`),
    KEY `idx_ledger_entry_transaction_time` (`transaction_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户账本条目表';

-- ----------------------------
-- 可清分明细准入事实表
-- ----------------------------
DROP TABLE IF EXISTS `t_clearing_splittable_detail`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '可清分明细准入事实表';

-- 清分批次表：一个批次只允许一个账务主体、币种、业务线、周期和规则版本
DROP TABLE IF EXISTS `t_clearing_split_batch`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '单账务主体清分批次表';

-- 清分批次成员关系表
DROP TABLE IF EXISTS `t_clearing_split_batch_detail`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '清分批次成员关系表';

-- 不可变清分结果快照表
DROP TABLE IF EXISTS `t_clearing_split_result_snapshot`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '不可变清分结果快照表';

-- ----------------------------
-- 清算候选准入事实表
DROP TABLE IF EXISTS `t_clearing_candidate`;
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
    KEY `idx_clearing_candidate_status_available` (`tenant_id`, `status`, `clearing_available_time`),
    KEY `idx_clearing_candidate_status_changed` (`tenant_id`, `status`, `status_changed_time`),
    KEY `idx_clearing_candidate_locked_batch` (`tenant_id`, `locked_clearing_batch_sn`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '清算候选准入事实表';

-- 清算批次表
DROP TABLE IF EXISTS `t_clearing_batch`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '清算批次表';

-- 清算批次明细表
DROP TABLE IF EXISTS `t_clearing_batch_detail`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '清算批次明细表';

-- 对账批次表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_batch`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账批次表';

-- ----------------------------
-- Gate 对账批次血缘表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_batch_lineage`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = 'Gate 对账批次当前血缘头';

-- ----------------------------
-- 对账来源快照表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_source_snapshot`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账来源快照表';

-- ----------------------------
-- 对账来源成员表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_source_item`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账来源成员表';

-- ----------------------------
-- 对账运行结果表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_run_result`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账运行结果表';

-- ----------------------------
-- 对账匹配结果明细表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_match_result`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账匹配结果明细表';

-- ----------------------------
-- 对账差错表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_difference`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账差错表';

-- ----------------------------
-- 对账差错处理动作事实表
-- ----------------------------
DROP TABLE IF EXISTS `t_reconciliation_difference_action`;
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账差错处理动作事实表';

DROP TABLE IF EXISTS `t_settlement_order_item`;
DROP TABLE IF EXISTS `t_settlement_order`;
DROP TABLE IF EXISTS `t_payout_receipt`;
DROP TABLE IF EXISTS `t_payout_order`;
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
    `status`                      VARCHAR(50)  NOT NULL COMMENT 'DRAFT/REVIEWING/APPROVED/LOCKED/FAILED/CANCELLED',
    `settlement_approval_ref`     VARCHAR(128)          DEFAULT NULL COMMENT '结算审批引用',
    `lock_funds_transaction_sn`   VARCHAR(64)           DEFAULT NULL COMMENT '锁定资金交易流水号',
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
    `reconciliation_run_result_sn` VARCHAR(64)           DEFAULT NULL COMMENT '锁定时消费的对账运行结果流水号',
    `reconciliation_result_digest` VARCHAR(64)           DEFAULT NULL COMMENT '锁定时对账结果 SHA-256',
    `reconciliation_evidence_digest` VARCHAR(64)         DEFAULT NULL COMMENT '锁定时对账证据引用 SHA-256',
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
  DEFAULT CHARSET = utf8mb4 COMMENT = '结算单表';

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '结算金额项表';

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
    `status`                         VARCHAR(30)  NOT NULL COMMENT 'CREATED/SUBMITTED/ACCEPTED/PROCESSING/SUCCEEDED/FAILED/RETURNED/MISMATCHED',
    `payout_account_ref`             VARCHAR(128)          DEFAULT NULL COMMENT '宿主出款账户引用',
    `payee_endpoint_ref`             VARCHAR(128)          DEFAULT NULL COMMENT '宿主收款端点引用',
    `channel_ref`                    VARCHAR(128)          DEFAULT NULL COMMENT '宿主通道引用',
    `approval_ref`                   VARCHAR(128)          DEFAULT NULL COMMENT '出款审批引用',
    `external_rule_evidence_digest`  VARCHAR(64)           DEFAULT NULL COMMENT '外部规则核验证据 SHA-256',
    `reconciliation_run_result_sn`   VARCHAR(64)           DEFAULT NULL COMMENT '提交消费的对账运行结果流水号',
    `reconciliation_result_digest`   VARCHAR(64)           DEFAULT NULL COMMENT '提交消费的对账结果 SHA-256',
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
    `version`                        INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payout_order_sn` (`tenant_id`, `sn`),
    UNIQUE KEY `uk_payout_order_settlement` (`tenant_id`, `settlement_order_sn`),
    UNIQUE KEY `uk_payout_order_external` (`tenant_id`, `channel_ref`, `external_reference`),
    KEY `idx_payout_order_status` (`tenant_id`, `status`, `gmt_modified`),
    KEY `idx_payout_order_completion` (`tenant_id`, `completion_funds_transaction_sn`),
    KEY `idx_payout_order_rollback` (`tenant_id`, `rollback_funds_transaction_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '出款单事实表';

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '出款回单事实表';

DROP TABLE IF EXISTS `t_recovery_result`;
DROP TABLE IF EXISTS `t_recovery_order`;

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '追偿责任事实表';

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '追偿资金结果事实表';

DROP TABLE IF EXISTS `t_projection_replay_difference`;
DROP TABLE IF EXISTS `t_funds_transaction_projection`;
DROP TABLE IF EXISTS `t_projection_replay_task`;

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
    `status`                   VARCHAR(30)  NOT NULL COMMENT '任务状态',
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
    KEY `idx_projection_replay_task_backlog` (`tenant_id`, `status`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '投影重放任务表';

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '投影重放差异证据表';

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
  DEFAULT CHARSET = utf8mb4 COMMENT = '资金交易只读投影表';
