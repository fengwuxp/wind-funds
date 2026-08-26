-- wind-funds state 命名一次性升级脚本（MySQL 8.0.4+）。
-- 仅用于已有宿主数据库；fresh install 使用各模块 001_create_* canonical DDL。
-- 执行前必须停止旧应用实例、完成备份，并在同一会话保存以下预检和分布输出。
-- 必须使用未开启 --force 的 mysql client；任一 guard 失败都会以非零退出码终止后续 DDL。

SET SESSION group_concat_max_len = 65535;

SET @state_naming_columns = JSON_ARRAY(
        JSON_OBJECT('table', 't_funding_account', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_credit_account', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_spend_control_scope', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_payment_instrument', 'source', 'instrument_direction', 'target', 'flow_direction'),
        JSON_OBJECT('table', 't_payment_instrument', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_payment_instrument_binding', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_spend_rule_definition', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_spend_rule_version', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_spend_rule_binding', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_funds_transaction', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_funds_transaction_detail', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_funds_frozen_order', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_ledger', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_clearing_splittable_detail', 'source', 'status', 'target', 'admission_result'),
        JSON_OBJECT('table', 't_clearing_splittable_detail', 'source', 'reconciliation_decision_status', 'target', 'reconciliation_decision_result'),
        JSON_OBJECT('table', 't_clearing_split_batch', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_clearing_candidate', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_clearing_candidate', 'source', 'status_changed_time', 'target', 'state_changed_time'),
        JSON_OBJECT('table', 't_clearing_batch', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_reconciliation_batch', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_reconciliation_run_result', 'source', 'status', 'target', 'outcome'),
        JSON_OBJECT('table', 't_reconciliation_difference', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_settlement_order', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_payout_order', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_payout_receipt', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_recovery_order', 'source', 'status', 'target', 'state'),
        JSON_OBJECT('table', 't_projection_replay_task', 'source', 'status', 'target', 'state'));

SET @state_naming_indexes = JSON_ARRAY(
        JSON_OBJECT('table', 't_funding_account', 'source', 'idx_funding_account_status', 'target', 'idx_funding_account_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_credit_account', 'source', 'idx_credit_account_status', 'target', 'idx_credit_account_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_spend_control_scope', 'source', 'idx_spend_control_scope_status', 'target', 'idx_spend_control_scope_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_payment_instrument', 'source', 'idx_payment_instrument_status', 'target', 'idx_payment_instrument_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_payment_instrument_binding', 'source', 'idx_payment_instrument_binding_status', 'target', 'idx_payment_instrument_binding_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_spend_rule_version', 'source', 'idx_spend_rule_version_status', 'target', 'idx_spend_rule_version_state', 'sourceColumns', 'tenant_id,status', 'targetColumns', 'tenant_id,state'),
        JSON_OBJECT('table', 't_funds_transaction', 'source', 'idx_funds_transaction_status', 'target', 'idx_funds_transaction_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_funds_transaction_detail', 'source', 'idx_funds_transaction_detail_status', 'target', 'idx_funds_transaction_detail_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_funds_frozen_order', 'source', 'idx_funds_frozen_order_status', 'target', 'idx_funds_frozen_order_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_ledger', 'source', 'idx_ledger_status', 'target', 'idx_ledger_state', 'sourceColumns', 'status', 'targetColumns', 'state'),
        JSON_OBJECT('table', 't_clearing_splittable_detail', 'source', 'idx_clearing_splittable_detail_status', 'target', 'idx_clearing_splittable_detail_admission_result', 'sourceColumns', 'tenant_id,status', 'targetColumns', 'tenant_id,admission_result'),
        JSON_OBJECT('table', 't_clearing_split_batch', 'source', 'idx_clearing_split_batch_status_age', 'target', 'idx_clearing_split_batch_state_age', 'sourceColumns', 'tenant_id,status,gmt_modified', 'targetColumns', 'tenant_id,state,gmt_modified'),
        JSON_OBJECT('table', 't_clearing_candidate', 'source', 'idx_clearing_candidate_status_available', 'target', 'idx_clearing_candidate_state_available', 'sourceColumns', 'tenant_id,status,clearing_available_time', 'targetColumns', 'tenant_id,state,clearing_available_time'),
        JSON_OBJECT('table', 't_clearing_candidate', 'source', 'idx_clearing_candidate_status_changed', 'target', 'idx_clearing_candidate_state_changed', 'sourceColumns', 'tenant_id,status,status_changed_time', 'targetColumns', 'tenant_id,state,state_changed_time'),
        JSON_OBJECT('table', 't_clearing_batch', 'source', 'idx_clearing_batch_status_age', 'target', 'idx_clearing_batch_state_age', 'sourceColumns', 'tenant_id,status,gmt_modified', 'targetColumns', 'tenant_id,state,gmt_modified'),
        JSON_OBJECT('table', 't_reconciliation_batch', 'source', 'idx_reconciliation_batch_status', 'target', 'idx_reconciliation_batch_state', 'sourceColumns', 'tenant_id,status', 'targetColumns', 'tenant_id,state'),
        JSON_OBJECT('table', 't_reconciliation_difference', 'source', 'idx_reconciliation_difference_status', 'target', 'idx_reconciliation_difference_state', 'sourceColumns', 'tenant_id,status', 'targetColumns', 'tenant_id,state'),
        JSON_OBJECT('table', 't_settlement_order', 'source', 'idx_settlement_order_status', 'target', 'idx_settlement_order_state', 'sourceColumns', 'tenant_id,status,gmt_modified', 'targetColumns', 'tenant_id,state,gmt_modified'),
        JSON_OBJECT('table', 't_payout_order', 'source', 'idx_payout_order_status', 'target', 'idx_payout_order_state', 'sourceColumns', 'tenant_id,status,gmt_modified', 'targetColumns', 'tenant_id,state,gmt_modified'),
        JSON_OBJECT('table', 't_recovery_order', 'source', 'idx_recovery_order_subject_status', 'target', 'idx_recovery_order_subject_state', 'sourceColumns', 'tenant_id,responsible_subject_type,responsible_subject_id,status,gmt_modified', 'targetColumns', 'tenant_id,responsible_subject_type,responsible_subject_id,state,gmt_modified'));

SET @state_naming_tables = JSON_ARRAY(
        't_funding_account', 't_credit_account', 't_spend_control_scope', 't_payment_instrument',
        't_payment_instrument_binding', 't_spend_rule_definition', 't_spend_rule_version',
        't_spend_rule_binding', 't_funds_transaction', 't_funds_transaction_detail',
        't_funds_frozen_order', 't_ledger', 't_clearing_splittable_detail', 't_clearing_split_batch',
        't_clearing_candidate', 't_clearing_batch', 't_reconciliation_batch',
        't_reconciliation_run_result', 't_reconciliation_difference', 't_settlement_order',
        't_payout_order', 't_payout_receipt', 't_recovery_order', 't_projection_replay_task');

WITH expected_columns (table_name, source_column, target_column) AS (
    SELECT 't_funding_account', 'status', 'state'
    UNION ALL SELECT 't_credit_account', 'status', 'state'
    UNION ALL SELECT 't_spend_control_scope', 'status', 'state'
    UNION ALL SELECT 't_payment_instrument', 'instrument_direction', 'flow_direction'
    UNION ALL SELECT 't_payment_instrument', 'status', 'state'
    UNION ALL SELECT 't_payment_instrument_binding', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_definition', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_version', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_binding', 'status', 'state'
    UNION ALL SELECT 't_funds_transaction', 'status', 'state'
    UNION ALL SELECT 't_funds_transaction_detail', 'status', 'state'
    UNION ALL SELECT 't_funds_frozen_order', 'status', 'state'
    UNION ALL SELECT 't_ledger', 'status', 'state'
    UNION ALL SELECT 't_clearing_splittable_detail', 'status', 'admission_result'
    UNION ALL SELECT 't_clearing_splittable_detail', 'reconciliation_decision_status', 'reconciliation_decision_result'
    UNION ALL SELECT 't_clearing_split_batch', 'status', 'state'
    UNION ALL SELECT 't_clearing_candidate', 'status', 'state'
    UNION ALL SELECT 't_clearing_candidate', 'status_changed_time', 'state_changed_time'
    UNION ALL SELECT 't_clearing_batch', 'status', 'state'
    UNION ALL SELECT 't_reconciliation_batch', 'status', 'state'
    UNION ALL SELECT 't_reconciliation_run_result', 'status', 'outcome'
    UNION ALL SELECT 't_reconciliation_difference', 'status', 'state'
    UNION ALL SELECT 't_settlement_order', 'status', 'state'
    UNION ALL SELECT 't_payout_order', 'status', 'state'
    UNION ALL SELECT 't_payout_receipt', 'status', 'state'
    UNION ALL SELECT 't_recovery_order', 'status', 'state'
    UNION ALL SELECT 't_projection_replay_task', 'status', 'state'
)
SELECT e.table_name,
       e.source_column,
       e.target_column,
       MAX(c.column_name = e.source_column) AS source_exists,
       MAX(c.column_name = e.target_column) AS target_exists
FROM expected_columns e
LEFT JOIN information_schema.columns c
       ON c.table_schema = DATABASE()
      AND c.table_name = e.table_name
      AND c.column_name IN (e.source_column, e.target_column)
GROUP BY e.table_name, e.source_column, e.target_column
ORDER BY e.table_name, e.source_column;

-- 预期：27 行全部 source_exists=1、target_exists=0。
WITH expected_columns AS (
    SELECT table_name, source_column, target_column
    FROM JSON_TABLE(@state_naming_columns, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_column VARCHAR(64) PATH '$.source',
        target_column VARCHAR(64) PATH '$.target')) AS mapping
)
SELECT COUNT(*) INTO @column_precheck_failures
FROM expected_columns e
WHERE (SELECT COUNT(*) FROM information_schema.columns c
       WHERE c.table_schema = DATABASE() AND c.table_name = e.table_name
         AND c.column_name = e.source_column) <> 1
   OR (SELECT COUNT(*) FROM information_schema.columns c
       WHERE c.table_schema = DATABASE() AND c.table_name = e.table_name
         AND c.column_name = e.target_column) <> 0;

WITH expected_indexes AS (
    SELECT table_name, source_index, target_index, source_columns, target_columns
    FROM JSON_TABLE(@state_naming_indexes, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_index VARCHAR(128) PATH '$.source',
        target_index VARCHAR(128) PATH '$.target',
        source_columns VARCHAR(512) PATH '$.sourceColumns',
        target_columns VARCHAR(512) PATH '$.targetColumns')) AS mapping
), actual_indexes AS (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS index_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name, index_name
)
SELECT e.table_name, e.source_index, e.target_index,
       source.index_columns AS source_columns,
       target.index_columns AS target_columns,
       e.source_columns AS expected_source_columns,
       e.target_columns AS expected_target_columns
FROM expected_indexes e
LEFT JOIN actual_indexes source
       ON source.table_name = e.table_name AND source.index_name = e.source_index
LEFT JOIN actual_indexes target
       ON target.table_name = e.table_name AND target.index_name = e.target_index
ORDER BY e.table_name, e.source_index;

WITH expected_indexes AS (
    SELECT table_name, source_index, target_index, source_columns
    FROM JSON_TABLE(@state_naming_indexes, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_index VARCHAR(128) PATH '$.source',
        target_index VARCHAR(128) PATH '$.target',
        source_columns VARCHAR(512) PATH '$.sourceColumns')) AS mapping
), actual_indexes AS (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS index_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name, index_name
)
SELECT COUNT(*) INTO @index_precheck_failures
FROM expected_indexes e
LEFT JOIN actual_indexes source
       ON source.table_name = e.table_name AND source.index_name = e.source_index
LEFT JOIN actual_indexes target
       ON target.table_name = e.table_name AND target.index_name = e.target_index
WHERE source.index_columns IS NULL
   OR source.index_columns <> e.source_columns
   OR target.index_columns IS NOT NULL;

SET @state_naming_precheck_failures = @column_precheck_failures + @index_precheck_failures;
SET @state_naming_guard_sql = IF(
        @state_naming_precheck_failures = 0,
        'SELECT ''STATE_NAMING_FORWARD_PRECHECK_PASS'' AS migration_guard',
        'SELECT * FROM information_schema.__wind_funds_state_naming_forward_precheck_failed__');
PREPARE state_naming_guard FROM @state_naming_guard_sql;
EXECUTE state_naming_guard;
DEALLOCATE PREPARE state_naming_guard;

SELECT GROUP_CONCAT(
        CONCAT('SELECT ''', table_name, ''' AS table_name, COUNT(*) AS row_count FROM `', table_name, '`')
        ORDER BY table_name SEPARATOR ' UNION ALL ')
INTO @state_naming_row_count_sql
FROM JSON_TABLE(@state_naming_tables, '$[*]' COLUMNS (table_name VARCHAR(64) PATH '$')) AS tables_to_count;
PREPARE state_naming_row_counts FROM @state_naming_row_count_sql;
EXECUTE state_naming_row_counts;
DEALLOCATE PREPARE state_naming_row_counts;

-- 保存下列枚举分布输出，执行后与脚本末尾同结构输出逐行比较。
SELECT 't_funding_account' AS table_name, 'status' AS column_name, status AS value_code, COUNT(*) AS row_count FROM t_funding_account GROUP BY status
UNION ALL SELECT 't_credit_account', 'status', status, COUNT(*) FROM t_credit_account GROUP BY status
UNION ALL SELECT 't_spend_control_scope', 'status', status, COUNT(*) FROM t_spend_control_scope GROUP BY status
UNION ALL SELECT 't_payment_instrument', 'instrument_direction', instrument_direction, COUNT(*) FROM t_payment_instrument GROUP BY instrument_direction
UNION ALL SELECT 't_payment_instrument', 'status', status, COUNT(*) FROM t_payment_instrument GROUP BY status
UNION ALL SELECT 't_payment_instrument_binding', 'status', status, COUNT(*) FROM t_payment_instrument_binding GROUP BY status
UNION ALL SELECT 't_spend_rule_definition', 'status', status, COUNT(*) FROM t_spend_rule_definition GROUP BY status
UNION ALL SELECT 't_spend_rule_version', 'status', status, COUNT(*) FROM t_spend_rule_version GROUP BY status
UNION ALL SELECT 't_spend_rule_binding', 'status', status, COUNT(*) FROM t_spend_rule_binding GROUP BY status
UNION ALL SELECT 't_funds_transaction', 'status', status, COUNT(*) FROM t_funds_transaction GROUP BY status
UNION ALL SELECT 't_funds_transaction_detail', 'status', status, COUNT(*) FROM t_funds_transaction_detail GROUP BY status
UNION ALL SELECT 't_funds_frozen_order', 'status', status, COUNT(*) FROM t_funds_frozen_order GROUP BY status
UNION ALL SELECT 't_ledger', 'status', status, COUNT(*) FROM t_ledger GROUP BY status
UNION ALL SELECT 't_clearing_splittable_detail', 'status', status, COUNT(*) FROM t_clearing_splittable_detail GROUP BY status
UNION ALL SELECT 't_clearing_splittable_detail', 'reconciliation_decision_status', reconciliation_decision_status, COUNT(*) FROM t_clearing_splittable_detail GROUP BY reconciliation_decision_status
UNION ALL SELECT 't_clearing_split_batch', 'status', status, COUNT(*) FROM t_clearing_split_batch GROUP BY status
UNION ALL SELECT 't_clearing_candidate', 'status', status, COUNT(*) FROM t_clearing_candidate GROUP BY status
UNION ALL SELECT 't_clearing_batch', 'status', status, COUNT(*) FROM t_clearing_batch GROUP BY status
UNION ALL SELECT 't_reconciliation_batch', 'status', status, COUNT(*) FROM t_reconciliation_batch GROUP BY status
UNION ALL SELECT 't_reconciliation_run_result', 'status', status, COUNT(*) FROM t_reconciliation_run_result GROUP BY status
UNION ALL SELECT 't_reconciliation_difference', 'status', status, COUNT(*) FROM t_reconciliation_difference GROUP BY status
UNION ALL SELECT 't_settlement_order', 'status', status, COUNT(*) FROM t_settlement_order GROUP BY status
UNION ALL SELECT 't_payout_order', 'status', status, COUNT(*) FROM t_payout_order GROUP BY status
UNION ALL SELECT 't_payout_receipt', 'status', status, COUNT(*) FROM t_payout_receipt GROUP BY status
UNION ALL SELECT 't_recovery_order', 'status', status, COUNT(*) FROM t_recovery_order GROUP BY status
UNION ALL SELECT 't_projection_replay_task', 'status', status, COUNT(*) FROM t_projection_replay_task GROUP BY status
ORDER BY table_name, column_name, value_code;

ALTER TABLE `t_funding_account` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_credit_account` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_control_scope` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payment_instrument` RENAME COLUMN `instrument_direction` TO `flow_direction`;
ALTER TABLE `t_payment_instrument` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payment_instrument_binding` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_definition` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_version` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_binding` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_transaction` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_transaction_detail` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_frozen_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_ledger` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_clearing_splittable_detail` RENAME COLUMN `status` TO `admission_result`;
ALTER TABLE `t_clearing_splittable_detail` RENAME COLUMN `reconciliation_decision_status` TO `reconciliation_decision_result`;
ALTER TABLE `t_clearing_split_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_clearing_candidate` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_clearing_candidate` RENAME COLUMN `status_changed_time` TO `state_changed_time`;
ALTER TABLE `t_clearing_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_reconciliation_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_reconciliation_run_result` RENAME COLUMN `status` TO `outcome`;
ALTER TABLE `t_reconciliation_difference` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_settlement_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payout_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payout_receipt` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_recovery_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_projection_replay_task` RENAME COLUMN `status` TO `state`;

ALTER TABLE `t_funding_account` RENAME INDEX `idx_funding_account_status` TO `idx_funding_account_state`;
ALTER TABLE `t_credit_account` RENAME INDEX `idx_credit_account_status` TO `idx_credit_account_state`;
ALTER TABLE `t_spend_control_scope` RENAME INDEX `idx_spend_control_scope_status` TO `idx_spend_control_scope_state`;
ALTER TABLE `t_payment_instrument` RENAME INDEX `idx_payment_instrument_status` TO `idx_payment_instrument_state`;
ALTER TABLE `t_payment_instrument_binding` RENAME INDEX `idx_payment_instrument_binding_status` TO `idx_payment_instrument_binding_state`;
ALTER TABLE `t_spend_rule_version` RENAME INDEX `idx_spend_rule_version_status` TO `idx_spend_rule_version_state`;
ALTER TABLE `t_funds_transaction` RENAME INDEX `idx_funds_transaction_status` TO `idx_funds_transaction_state`;
ALTER TABLE `t_funds_transaction_detail` RENAME INDEX `idx_funds_transaction_detail_status` TO `idx_funds_transaction_detail_state`;
ALTER TABLE `t_funds_frozen_order` RENAME INDEX `idx_funds_frozen_order_status` TO `idx_funds_frozen_order_state`;
ALTER TABLE `t_ledger` RENAME INDEX `idx_ledger_status` TO `idx_ledger_state`;
ALTER TABLE `t_clearing_splittable_detail` RENAME INDEX `idx_clearing_splittable_detail_status` TO `idx_clearing_splittable_detail_admission_result`;
ALTER TABLE `t_clearing_split_batch` RENAME INDEX `idx_clearing_split_batch_status_age` TO `idx_clearing_split_batch_state_age`;
ALTER TABLE `t_clearing_candidate` RENAME INDEX `idx_clearing_candidate_status_available` TO `idx_clearing_candidate_state_available`;
ALTER TABLE `t_clearing_candidate` RENAME INDEX `idx_clearing_candidate_status_changed` TO `idx_clearing_candidate_state_changed`;
ALTER TABLE `t_clearing_batch` RENAME INDEX `idx_clearing_batch_status_age` TO `idx_clearing_batch_state_age`;
ALTER TABLE `t_reconciliation_batch` RENAME INDEX `idx_reconciliation_batch_status` TO `idx_reconciliation_batch_state`;
ALTER TABLE `t_reconciliation_difference` RENAME INDEX `idx_reconciliation_difference_status` TO `idx_reconciliation_difference_state`;
ALTER TABLE `t_settlement_order` RENAME INDEX `idx_settlement_order_status` TO `idx_settlement_order_state`;
ALTER TABLE `t_payout_order` RENAME INDEX `idx_payout_order_status` TO `idx_payout_order_state`;
ALTER TABLE `t_recovery_order` RENAME INDEX `idx_recovery_order_subject_status` TO `idx_recovery_order_subject_state`;

WITH expected_columns (table_name, source_column, target_column) AS (
    SELECT 't_funding_account', 'status', 'state'
    UNION ALL SELECT 't_credit_account', 'status', 'state'
    UNION ALL SELECT 't_spend_control_scope', 'status', 'state'
    UNION ALL SELECT 't_payment_instrument', 'instrument_direction', 'flow_direction'
    UNION ALL SELECT 't_payment_instrument', 'status', 'state'
    UNION ALL SELECT 't_payment_instrument_binding', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_definition', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_version', 'status', 'state'
    UNION ALL SELECT 't_spend_rule_binding', 'status', 'state'
    UNION ALL SELECT 't_funds_transaction', 'status', 'state'
    UNION ALL SELECT 't_funds_transaction_detail', 'status', 'state'
    UNION ALL SELECT 't_funds_frozen_order', 'status', 'state'
    UNION ALL SELECT 't_ledger', 'status', 'state'
    UNION ALL SELECT 't_clearing_splittable_detail', 'status', 'admission_result'
    UNION ALL SELECT 't_clearing_splittable_detail', 'reconciliation_decision_status', 'reconciliation_decision_result'
    UNION ALL SELECT 't_clearing_split_batch', 'status', 'state'
    UNION ALL SELECT 't_clearing_candidate', 'status', 'state'
    UNION ALL SELECT 't_clearing_candidate', 'status_changed_time', 'state_changed_time'
    UNION ALL SELECT 't_clearing_batch', 'status', 'state'
    UNION ALL SELECT 't_reconciliation_batch', 'status', 'state'
    UNION ALL SELECT 't_reconciliation_run_result', 'status', 'outcome'
    UNION ALL SELECT 't_reconciliation_difference', 'status', 'state'
    UNION ALL SELECT 't_settlement_order', 'status', 'state'
    UNION ALL SELECT 't_payout_order', 'status', 'state'
    UNION ALL SELECT 't_payout_receipt', 'status', 'state'
    UNION ALL SELECT 't_recovery_order', 'status', 'state'
    UNION ALL SELECT 't_projection_replay_task', 'status', 'state'
)
SELECT e.table_name,
       e.source_column,
       e.target_column,
       MAX(c.column_name = e.source_column) AS source_exists,
       MAX(c.column_name = e.target_column) AS target_exists
FROM expected_columns e
LEFT JOIN information_schema.columns c
       ON c.table_schema = DATABASE()
      AND c.table_name = e.table_name
      AND c.column_name IN (e.source_column, e.target_column)
GROUP BY e.table_name, e.source_column, e.target_column
ORDER BY e.table_name, e.source_column;

-- 预期：27 行全部 source_exists=0、target_exists=1。
WITH expected_columns AS (
    SELECT table_name, source_column, target_column
    FROM JSON_TABLE(@state_naming_columns, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_column VARCHAR(64) PATH '$.source',
        target_column VARCHAR(64) PATH '$.target')) AS mapping
)
SELECT COUNT(*) INTO @column_postcheck_failures
FROM expected_columns e
WHERE (SELECT COUNT(*) FROM information_schema.columns c
       WHERE c.table_schema = DATABASE() AND c.table_name = e.table_name
         AND c.column_name = e.source_column) <> 0
   OR (SELECT COUNT(*) FROM information_schema.columns c
       WHERE c.table_schema = DATABASE() AND c.table_name = e.table_name
         AND c.column_name = e.target_column) <> 1;

WITH expected_indexes AS (
    SELECT table_name, source_index, target_index, source_columns, target_columns
    FROM JSON_TABLE(@state_naming_indexes, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_index VARCHAR(128) PATH '$.source',
        target_index VARCHAR(128) PATH '$.target',
        source_columns VARCHAR(512) PATH '$.sourceColumns',
        target_columns VARCHAR(512) PATH '$.targetColumns')) AS mapping
), actual_indexes AS (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS index_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name, index_name
)
SELECT e.table_name, e.source_index, e.target_index,
       source.index_columns AS source_columns,
       target.index_columns AS target_columns,
       e.target_columns AS expected_target_columns
FROM expected_indexes e
LEFT JOIN actual_indexes source
       ON source.table_name = e.table_name AND source.index_name = e.source_index
LEFT JOIN actual_indexes target
       ON target.table_name = e.table_name AND target.index_name = e.target_index
ORDER BY e.table_name, e.target_index;

WITH expected_indexes AS (
    SELECT table_name, source_index, target_index, target_columns
    FROM JSON_TABLE(@state_naming_indexes, '$[*]' COLUMNS (
        table_name VARCHAR(64) PATH '$.table',
        source_index VARCHAR(128) PATH '$.source',
        target_index VARCHAR(128) PATH '$.target',
        target_columns VARCHAR(512) PATH '$.targetColumns')) AS mapping
), actual_indexes AS (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS index_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name, index_name
)
SELECT COUNT(*) INTO @index_postcheck_failures
FROM expected_indexes e
LEFT JOIN actual_indexes source
       ON source.table_name = e.table_name AND source.index_name = e.source_index
LEFT JOIN actual_indexes target
       ON target.table_name = e.table_name AND target.index_name = e.target_index
WHERE source.index_columns IS NOT NULL
   OR target.index_columns IS NULL
   OR target.index_columns <> e.target_columns;

SELECT GROUP_CONCAT(
        CONCAT('SELECT ''', table_name, ''' AS table_name, COUNT(*) AS row_count FROM `', table_name, '`')
        ORDER BY table_name SEPARATOR ' UNION ALL ')
INTO @state_naming_row_count_sql
FROM JSON_TABLE(@state_naming_tables, '$[*]' COLUMNS (table_name VARCHAR(64) PATH '$')) AS tables_to_count;
PREPARE state_naming_row_counts FROM @state_naming_row_count_sql;
EXECUTE state_naming_row_counts;
DEALLOCATE PREPARE state_naming_row_counts;

SET @state_naming_postcheck_failures = @column_postcheck_failures + @index_postcheck_failures;
SET @state_naming_guard_sql = IF(
        @state_naming_postcheck_failures = 0,
        'SELECT ''STATE_NAMING_FORWARD_POSTCHECK_PASS'' AS migration_guard',
        'SELECT * FROM information_schema.__wind_funds_state_naming_forward_postcheck_failed__');
PREPARE state_naming_guard FROM @state_naming_guard_sql;
EXECUTE state_naming_guard;
DEALLOCATE PREPARE state_naming_guard;

SELECT 't_funding_account' AS table_name, 'state' AS column_name, state AS value_code, COUNT(*) AS row_count FROM t_funding_account GROUP BY state
UNION ALL SELECT 't_credit_account', 'state', state, COUNT(*) FROM t_credit_account GROUP BY state
UNION ALL SELECT 't_spend_control_scope', 'state', state, COUNT(*) FROM t_spend_control_scope GROUP BY state
UNION ALL SELECT 't_payment_instrument', 'flow_direction', flow_direction, COUNT(*) FROM t_payment_instrument GROUP BY flow_direction
UNION ALL SELECT 't_payment_instrument', 'state', state, COUNT(*) FROM t_payment_instrument GROUP BY state
UNION ALL SELECT 't_payment_instrument_binding', 'state', state, COUNT(*) FROM t_payment_instrument_binding GROUP BY state
UNION ALL SELECT 't_spend_rule_definition', 'state', state, COUNT(*) FROM t_spend_rule_definition GROUP BY state
UNION ALL SELECT 't_spend_rule_version', 'state', state, COUNT(*) FROM t_spend_rule_version GROUP BY state
UNION ALL SELECT 't_spend_rule_binding', 'state', state, COUNT(*) FROM t_spend_rule_binding GROUP BY state
UNION ALL SELECT 't_funds_transaction', 'state', state, COUNT(*) FROM t_funds_transaction GROUP BY state
UNION ALL SELECT 't_funds_transaction_detail', 'state', state, COUNT(*) FROM t_funds_transaction_detail GROUP BY state
UNION ALL SELECT 't_funds_frozen_order', 'state', state, COUNT(*) FROM t_funds_frozen_order GROUP BY state
UNION ALL SELECT 't_ledger', 'state', state, COUNT(*) FROM t_ledger GROUP BY state
UNION ALL SELECT 't_clearing_splittable_detail', 'admission_result', admission_result, COUNT(*) FROM t_clearing_splittable_detail GROUP BY admission_result
UNION ALL SELECT 't_clearing_splittable_detail', 'reconciliation_decision_result', reconciliation_decision_result, COUNT(*) FROM t_clearing_splittable_detail GROUP BY reconciliation_decision_result
UNION ALL SELECT 't_clearing_split_batch', 'state', state, COUNT(*) FROM t_clearing_split_batch GROUP BY state
UNION ALL SELECT 't_clearing_candidate', 'state', state, COUNT(*) FROM t_clearing_candidate GROUP BY state
UNION ALL SELECT 't_clearing_batch', 'state', state, COUNT(*) FROM t_clearing_batch GROUP BY state
UNION ALL SELECT 't_reconciliation_batch', 'state', state, COUNT(*) FROM t_reconciliation_batch GROUP BY state
UNION ALL SELECT 't_reconciliation_run_result', 'outcome', outcome, COUNT(*) FROM t_reconciliation_run_result GROUP BY outcome
UNION ALL SELECT 't_reconciliation_difference', 'state', state, COUNT(*) FROM t_reconciliation_difference GROUP BY state
UNION ALL SELECT 't_settlement_order', 'state', state, COUNT(*) FROM t_settlement_order GROUP BY state
UNION ALL SELECT 't_payout_order', 'state', state, COUNT(*) FROM t_payout_order GROUP BY state
UNION ALL SELECT 't_payout_receipt', 'state', state, COUNT(*) FROM t_payout_receipt GROUP BY state
UNION ALL SELECT 't_recovery_order', 'state', state, COUNT(*) FROM t_recovery_order GROUP BY state
UNION ALL SELECT 't_projection_replay_task', 'state', state, COUNT(*) FROM t_projection_replay_task GROUP BY state
ORDER BY table_name, column_name, value_code;
