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
-- 账户层级绑定表
-- ----------------------------
DROP TABLE IF EXISTS `t_account_hierarchy_binding`;
CREATE TABLE `t_account_hierarchy_binding`
(
    `id`                     BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64) NOT NULL COMMENT '绑定号，全局唯一',
    `tenant_id`              BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `account_id`             VARCHAR(64) NOT NULL COMMENT '子账户 ID',
    `account_type`           VARCHAR(50) NOT NULL COMMENT '子账户主体类型',
    `parent_account_id`      VARCHAR(64) NOT NULL COMMENT '父账户 ID',
    `parent_account_type`    VARCHAR(50) NOT NULL COMMENT '父账户主体类型',
    `root_account_id`        VARCHAR(64) NOT NULL COMMENT '根账户 ID',
    `root_account_type`      VARCHAR(50) NOT NULL COMMENT '根账户主体类型',
    `currency`               VARCHAR(10) NOT NULL COMMENT '币种',
    `status`                 VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `operator_id`            VARCHAR(64)          DEFAULT NULL COMMENT '操作者',
    `context_variables`      TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_hierarchy_binding_sn` (`sn`),
    KEY `idx_account_hierarchy_binding_account` (`tenant_id`, `account_type`, `account_id`),
    KEY `idx_account_hierarchy_binding_parent` (`tenant_id`, `parent_account_type`, `parent_account_id`),
    KEY `idx_account_hierarchy_binding_root` (`tenant_id`, `root_account_type`, `root_account_id`),
    KEY `idx_account_hierarchy_binding_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户层级绑定表';

-- ----------------------------
-- 预算组表
-- ----------------------------
DROP TABLE IF EXISTS `t_budget_group`;
CREATE TABLE `t_budget_group`
(
    `id`                     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `sn`                     VARCHAR(64)  NOT NULL COMMENT '预算组号，全局唯一',
    `tenant_id`              BIGINT(20)   NOT NULL COMMENT '租户 ID',
    `owner_id`               VARCHAR(30)  NOT NULL COMMENT '归属主体 ID',
    `owner_type`             VARCHAR(50)  NOT NULL COMMENT '归属主体类型',
    `budget_type`            VARCHAR(50)  NOT NULL COMMENT '预算类型',
    `currency`               VARCHAR(10)  NOT NULL COMMENT '币种',
    `period_type`            VARCHAR(20)  NOT NULL DEFAULT 'LIFETIME' COMMENT '周期类型',
    `period_id`              VARCHAR(30)  NOT NULL DEFAULT 'LIFETIME' COMMENT '周期 ID',
    `period_policy`          VARCHAR(50)           DEFAULT NULL COMMENT '周期策略',
    `status`                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`            VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    `context_variables`      TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    `version`                INT(11)      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_budget_group_sn` (`sn`),
    KEY `idx_budget_group_owner` (`owner_type`, `owner_id`),
    KEY `idx_budget_group_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '预算组表';

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
    `instrument_direction`   VARCHAR(50)  NOT NULL COMMENT '工具方向：RECEIVE/PAYMENT/BOTH',
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
    `status`              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `version`             INT(11)     NOT NULL DEFAULT 1 COMMENT '绑定版本',
    `valid_from`          DATETIME             DEFAULT NULL COMMENT '生效时间',
    `valid_to`            DATETIME             DEFAULT NULL COMMENT '失效时间',
    `description`         VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`   TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_binding_sn` (`sn`),
    UNIQUE KEY `uk_payment_instrument_binding_subject` (`instrument_sn`, `binding_role`, `subject_type`, `subject_id`, `currency`),
    KEY `idx_payment_instrument_binding_instrument` (`instrument_sn`),
    KEY `idx_payment_instrument_binding_subject` (`subject_type`, `subject_id`),
    KEY `idx_payment_instrument_binding_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具绑定表';

-- ----------------------------
-- 支付工具绑定并发保护表
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_instrument_binding_guard`;
CREATE TABLE `t_payment_instrument_binding_guard`
(
    `id`             BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`      BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `instrument_sn`  VARCHAR(64) NOT NULL COMMENT '工具号',
    `binding_role`   VARCHAR(50) NOT NULL COMMENT '绑定角色',
    `currency`       VARCHAR(10) NOT NULL COMMENT '币种',
    `guard_type`     VARCHAR(50) NOT NULL COMMENT '保护类型',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_binding_guard_scope`
        (`tenant_id`, `instrument_sn`, `binding_role`, `currency`, `guard_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '支付工具绑定并发保护表';

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
    `after_snapshot`      TEXT         NOT NULL COMMENT '变更后快照',
    `operator_id`         VARCHAR(64)  NOT NULL COMMENT '操作者',
    `change_reason`       VARCHAR(256) NOT NULL COMMENT '变更原因',
    `effective_at`        DATETIME              DEFAULT NULL COMMENT '生效时间',
    `request_sn`          VARCHAR(64)           DEFAULT NULL COMMENT '请求号',
    `context_variables`   TEXT                  DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_instrument_binding_history_sn` (`sn`),
    UNIQUE KEY `uk_payment_instrument_binding_history_version` (`binding_sn`, `version`),
    UNIQUE KEY `uk_payment_instrument_binding_history_request` (`tenant_id`, `request_sn`),
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
    `funding_account_id`  VARCHAR(64)          DEFAULT NULL COMMENT '兼容真实资金账户 ID，仅资金账户目标主体使用',
    `target_subject_type` VARCHAR(50) NOT NULL COMMENT '资金责任目标主体类型',
    `target_subject_id`   VARCHAR(64) NOT NULL COMMENT '资金责任目标主体 ID',
    `currency`            VARCHAR(10) NOT NULL COMMENT '币种',
    `relation_type`       VARCHAR(50) NOT NULL COMMENT '关系类型',
    `priority`            INT(11)     NOT NULL DEFAULT 0 COMMENT '路由优先级',
    `is_default`          TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否默认关系',
    `status`              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `valid_from`          DATETIME             DEFAULT NULL COMMENT '生效时间',
    `valid_to`            DATETIME             DEFAULT NULL COMMENT '失效时间',
    `description`         VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    `context_variables`   TEXT                 DEFAULT NULL COMMENT '扩展上下文',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_subject_funding_rel_sn` (`sn`),
    UNIQUE KEY `uk_spend_subject_funding_rel_subject` (`spend_subject_type`, `spend_subject_id`, `target_subject_type`, `target_subject_id`, `currency`, `relation_type`),
    KEY `idx_spend_subject_funding_rel_spend_subject` (`spend_subject_type`, `spend_subject_id`),
    KEY `idx_spend_subject_funding_rel_funding` (`funding_account_id`),
    KEY `idx_spend_subject_funding_rel_target` (`target_subject_type`, `target_subject_id`),
    KEY `idx_spend_subject_funding_rel_status` (`status`)
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
DROP TABLE IF EXISTS `t_spend_rule_assignment`;
CREATE TABLE `t_spend_rule_assignment`
(
    `id`              BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gmt_create`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `tenant_id`       BIGINT(20)  NOT NULL COMMENT '租户 ID',
    `assignment_sn`   VARCHAR(64) NOT NULL COMMENT '规则挂载流水号',
    `rule_id`         VARCHAR(64) NOT NULL COMMENT 'Spend Rule 标识',
    `rule_version`    VARCHAR(64) NOT NULL COMMENT 'Spend Rule 版本',
    `scope_type`      VARCHAR(50) NOT NULL COMMENT '挂载范围类型',
    `scope_id`        VARCHAR(64) NOT NULL COMMENT '挂载范围标识',
    `priority`        INT(11)     NOT NULL DEFAULT 0 COMMENT '挂载优先级',
    `conflict_policy` VARCHAR(50) NOT NULL COMMENT '挂载冲突策略',
    `effective_from`  DATETIME    NOT NULL COMMENT '生效开始时间',
    `effective_to`    DATETIME    NOT NULL COMMENT '生效结束时间',
    `status`          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `description`     VARCHAR(512)         DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spend_rule_assignment_sn` (`tenant_id`, `assignment_sn`),
    UNIQUE KEY `uk_spend_rule_assignment_scope` (`tenant_id`, `scope_type`, `scope_id`, `rule_id`, `rule_version`),
    KEY `idx_spend_rule_assignment_rule` (`tenant_id`, `rule_id`, `rule_version`, `status`),
    KEY `idx_spend_rule_assignment_scope` (`tenant_id`, `scope_type`, `scope_id`, `status`)
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
    `assignment_sn`   VARCHAR(64)          DEFAULT NULL COMMENT '规则挂载流水号',
    `scope_type`      VARCHAR(50) NOT NULL COMMENT '控制范围类型',
    `scope_id`        VARCHAR(64) NOT NULL COMMENT '控制范围标识',
    `instrument_sn`   VARCHAR(64)          DEFAULT NULL COMMENT '支付工具号',
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
    KEY `idx_spend_rule_decision_record_assignment` (`tenant_id`, `assignment_sn`)
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
    `budget_group_sn`       VARCHAR(64)          DEFAULT NULL COMMENT '预算组或控制范围标识',
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
    KEY `idx_spend_control_movement_budget` (`tenant_id`, `budget_group_sn`, `period_id`, `currency`),
    KEY `idx_spend_control_movement_rule` (`tenant_id`, `spend_rule_id`, `spend_rule_version`),
    KEY `idx_spend_control_movement_rolling_count` (`tenant_id`, `budget_group_sn`, `currency`, `spend_rule_id`, `spend_rule_version`, `target_subject_type`, `target_subject_id`, `gmt_create`),
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
    `reference_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '引用交易号',
    `status`                   VARCHAR(50) NOT NULL DEFAULT 'CREATED' COMMENT '交易状态',
    `amount`                   BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '原始交易金额',
    `currency`                 VARCHAR(10) NOT NULL COMMENT '币种',
    `authorized_amount`        BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计授权金额',
    `reversed_amount`          BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计撤销金额',
    `settled_amount`           BIGINT(20)  NOT NULL DEFAULT 0 COMMENT '累计结算/完成金额',
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
    KEY `idx_funds_transaction_tenant` (`tenant_id`),
    KEY `idx_funds_transaction_reference` (`reference_transaction_sn`),
    KEY `idx_funds_transaction_status` (`status`),
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
    `is_balanced`                     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否借贷平衡',
    `status`                          VARCHAR(20)   NOT NULL DEFAULT 'POSTED' COMMENT '账本交易状态',
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
    `is_balanced`                 TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否平衡',
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
    `subject_id`                      VARCHAR(64)   NOT NULL COMMENT '账务主体 ID',
    `subject_type`                    VARCHAR(50)   NOT NULL COMMENT '账务主体类型',
    `ledger_subject_code`             VARCHAR(50)   NOT NULL COMMENT '账本科目编码',
    `ledger_subject_category`         VARCHAR(50)   NOT NULL COMMENT '科目分类快照',
    `entry_side`                      VARCHAR(10)   NOT NULL COMMENT '借贷方向',
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
    `settlement_status`               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '结算状态',
    `settlement_period`               VARCHAR(30)            DEFAULT NULL COMMENT '结算周期',
    `settlement_completed_time`       DATETIME               DEFAULT NULL COMMENT '结算完成时间',
    `reconcile_status`                VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '对账状态',
    `reconciliation_batch`            VARCHAR(64)            DEFAULT NULL COMMENT '对账批次',
    `reconciliation_completed_time`   DATETIME               DEFAULT NULL COMMENT '对账完成时间',
    `reconcile_remark`                VARCHAR(512)           DEFAULT NULL COMMENT '对账备注',
    `description`                     VARCHAR(512)           DEFAULT NULL COMMENT '描述',
    `context_variables`               TEXT                   DEFAULT NULL COMMENT '扩展上下文',
    `sha256`                          VARCHAR(128)  NOT NULL COMMENT '防篡改摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_entry_sn` (`sn`),
    KEY `idx_ledger_entry_transaction` (`ledger_transaction_sn`),
    KEY `idx_ledger_entry_posting_plan` (`posting_plan_sn`),
    KEY `idx_ledger_entry_ledger` (`ledger_id`),
    KEY `idx_ledger_entry_subject` (`subject_type`, `subject_id`),
    KEY `idx_ledger_entry_funds_transaction` (`funds_transaction_sn`),
    KEY `idx_ledger_entry_business` (`business_scene`, `business_sn`),
    KEY `idx_ledger_entry_settlement` (`settlement_status`, `settlement_period`),
    KEY `idx_ledger_entry_reconcile` (`reconcile_status`, `reconciliation_batch`),
    KEY `idx_ledger_entry_transaction_time` (`transaction_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账户账本条目表';

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
    `source_record_sn`          VARCHAR(64) NOT NULL COMMENT '对账来源记录流水号',
    `source_quality`            VARCHAR(50) NOT NULL COMMENT '对账来源质量',
    `match_strength`            VARCHAR(50) NOT NULL COMMENT '对账匹配强度',
    `difference_type`           VARCHAR(50) NOT NULL COMMENT '对账差错类型',
    `severity`                  VARCHAR(50) NOT NULL COMMENT '对账差错严重等级',
    `status`                    VARCHAR(50) NOT NULL COMMENT '对账差错状态',
    `currency`                  VARCHAR(10) NOT NULL COMMENT '差异币种',
    `difference_amount`         BIGINT(20)  NOT NULL COMMENT '差异金额，最小货币单位',
    `responsible_party_ref`     VARCHAR(128) NOT NULL COMMENT '责任方引用',
    `blocking_scope`            VARCHAR(128) NOT NULL COMMENT '阻断范围',
    `blocking_object_type`      VARCHAR(50)          DEFAULT NULL COMMENT '阻断对象类型',
    `blocking_object_sn`        VARCHAR(64)          DEFAULT NULL COMMENT '阻断对象流水号',
    `rule_version`              VARCHAR(64) NOT NULL COMMENT '匹配或对账规则版本',
    `evidence_ref`              VARCHAR(128) NOT NULL COMMENT '来源证据引用',
    `action_type`               VARCHAR(50)          DEFAULT NULL COMMENT '差错处理动作类型',
    `adjustment_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '关联处理动作或调账单号',
    `adjustment_idempotency_key` VARCHAR(128)         DEFAULT NULL COMMENT '处理动作幂等键',
    `original_fact_ref`         VARCHAR(128)         DEFAULT NULL COMMENT '被处理的原始事实引用',
    `adjustment_transaction_sn` VARCHAR(64)          DEFAULT NULL COMMENT '关联资金交易流水号',
    `adjustment_approval_ref`   VARCHAR(128)         DEFAULT NULL COMMENT '调账审批引用',
    `adjustment_evidence_ref`   VARCHAR(128)         DEFAULT NULL COMMENT '调账证据引用',
    `adjustment_reason`         VARCHAR(512)         DEFAULT NULL COMMENT '处理原因',
    `last_rerun_sn`             VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑流水号',
    `last_rerun_batch_sn`       VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑批次流水号',
    `last_rerun_rule_version`   VARCHAR(64)          DEFAULT NULL COMMENT '最后一次重跑规则版本',
    `last_rerun_balanced`       TINYINT(1)           DEFAULT NULL COMMENT '最后一次重跑是否对平',
    `last_rerun_evidence_ref`   VARCHAR(128)         DEFAULT NULL COMMENT '最后一次重跑证据引用',
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
    KEY `idx_reconciliation_difference_batch` (`tenant_id`, `reconciliation_batch_sn`),
    KEY `idx_reconciliation_difference_source` (`tenant_id`, `source_record_sn`),
    KEY `idx_reconciliation_difference_status` (`tenant_id`, `status`),
    KEY `idx_reconciliation_difference_blocking_object` (`tenant_id`, `blocking_scope`, `blocking_object_type`, `blocking_object_sn`, `status`),
    KEY `idx_reconciliation_difference_adjustment` (`tenant_id`, `adjustment_sn`),
    KEY `idx_reconciliation_difference_rerun` (`tenant_id`, `last_rerun_sn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '对账差错表';
