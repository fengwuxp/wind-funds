-- 执行 001_create_reconciliation_tables.sql 后运行。
-- 除版本信息外，其余结构校验结果集均为空才表示与当前基线一致；非空必须停止发布并处理漂移。

SELECT @@version AS mysql_version,
       @@transaction_isolation AS transaction_isolation;

WITH expected_tables AS (
    SELECT 't_clearing_splittable_detail' AS table_name, 27 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 26 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 9 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 25 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 35 AS column_count, 9 AS index_count
    UNION ALL SELECT 't_clearing_batch' AS table_name, 32 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 14 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 27 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 9 AS column_count, 3 AS index_count
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 18 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 24 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 25 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 20 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 44 AS column_count, 10 AS index_count
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 14 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, 18 AS column_count, 4 AS index_count
    UNION ALL SELECT 't_reconciliation_gate_requirement_pair' AS table_name, 12 AS column_count, 3 AS index_count
    UNION ALL SELECT 't_reconciliation_gate_requirement_head' AS table_name, 13 AS column_count, 3 AS index_count
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, 16 AS column_count, 4 AS index_count
    UNION ALL SELECT 't_settlement_order' AS table_name, 61 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 15 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_payout_order' AS table_name, 34 AS column_count, 7 AS index_count
    UNION ALL SELECT 't_payout_receipt' AS table_name, 17 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_recovery_order' AS table_name, 21 AS column_count, 5 AS index_count
    UNION ALL SELECT 't_recovery_result' AS table_name, 13 AS column_count, 5 AS index_count
), actual_columns AS (
    SELECT table_name, COUNT(*) AS column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
    GROUP BY table_name
), actual_indexes AS (
    SELECT table_name, COUNT(DISTINCT index_name) AS index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name
)
SELECT expected.table_name,
       tables.engine,
       tables.table_collation,
       expected.column_count AS expected_column_count,
       column_counts.column_count AS actual_column_count,
       expected.index_count AS expected_index_count,
       index_counts.index_count AS actual_index_count
FROM expected_tables expected
LEFT JOIN information_schema.tables tables
       ON tables.table_schema = DATABASE()
      AND tables.table_name = expected.table_name
LEFT JOIN actual_columns column_counts ON column_counts.table_name = expected.table_name
LEFT JOIN actual_indexes index_counts ON index_counts.table_name = expected.table_name
WHERE tables.table_name IS NULL
   OR tables.engine <> 'InnoDB'
   OR tables.table_collation <> 'utf8mb4_bin'
   OR column_counts.column_count <> expected.column_count
   OR index_counts.index_count <> expected.index_count;

-- 列名、顺序、归一类型、可空性、默认值、自增、更新时间和字符语义必须精确匹配。
SET SESSION group_concat_max_len = 65535;

WITH expected_column_signatures AS (
    SELECT 't_clearing_splittable_detail' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|funds_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|funds_transaction_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|ledger_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|posting_plan_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|ledger_entry_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;015|refund_amount|bigint|NO|0|0|0|<NULL>|<NULL>;016|business_line|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|split_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|split_rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|split_rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|exclusion_reason|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|reconciliation_decision_status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|gate_evidence_ref|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|reconciliation_evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|route_snapshot_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|business_line|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|split_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|split_rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|split_rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|detail_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;014|total_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;015|member_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|batch_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|active_batch_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|submitted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|submitted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;022|confirmed_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|confirmed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;024|cancelled_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|cancelled_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;026|cancel_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|split_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|splittable_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|active_splittable_detail_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|split_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|splittable_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|business_line|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|split_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;013|funds_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|funds_transaction_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|ledger_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|posting_plan_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|ledger_entry_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|route_snapshot_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|split_rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|split_rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|gate_evidence_ref|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|reconciliation_evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|snapshot_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_clearing_candidate' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|split_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|split_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|splittable_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|business_line|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|clearing_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;015|funds_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|funds_transaction_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|ledger_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|posting_plan_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|ledger_entry_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|route_snapshot_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|clearing_available_time|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;022|clearing_rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|clearing_rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|gate_evidence_ref|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|reconciliation_evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|candidate_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|active_splittable_detail_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;029|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;030|block_reason|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;031|exclusion_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;032|locked_clearing_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;033|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;034|updated_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;035|status_changed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_clearing_batch' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|business_line|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|clearing_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|clearing_rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|clearing_rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|candidate_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;014|total_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;015|amount_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|active_amount_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|submitted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|submitted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;022|confirmed_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|confirmed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;024|returned_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|returned_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;026|return_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|cancelled_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|cancelled_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;029|cancel_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;030|failed_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;031|failed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;032|failure_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|clearing_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|candidate_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|split_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|splittable_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|funds_transaction_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|ledger_entry_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;013|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|scope_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|scope_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|pair_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|pair_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|rule_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|rule_identity|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|window_start|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;015|window_end|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;016|time_semantics|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|timezone_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|previous_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|run_result_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|aborted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|aborted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;023|abort_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|replacement_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|replacement_evidence_ref|varchar(256)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|batch_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|scope_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|scope_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|pair_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|pair_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|current_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|source_role|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|source_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|snapshot_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|snapshot_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|snapshot_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|coverage_complete|tinyint|NO|<NULL>|0|0|<NULL>|<NULL>;012|coverage_watermark|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|coverage_member_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;014|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|semantic_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|evidence_bundle_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|source_snapshot_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|source_fact_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|source_fact_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|comparison_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|comparison_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;011|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|rule_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|rule_identity|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|comparison_status_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|comparison_proven|tinyint|NO|<NULL>|0|0|<NULL>|<NULL>;017|claim_kind|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|economic_component|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|direction|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|normalization_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|semantic_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|evidence_bundle_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|scope_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|scope_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|pair_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|pair_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|rule_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|rule_identity|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|reference_snapshot_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|comparison_snapshot_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|reference_source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|comparison_source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|result_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|total_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;022|matched_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;023|difference_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;024|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_run_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|reference_fact_owner_namespace|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|reference_fact_identity_value|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|comparison_fact_owner_namespace|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|comparison_fact_identity_value|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|comparison_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|comparison_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|result_kind|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|absolute_difference_currency|varchar(10)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|absolute_difference_amount|bigint|YES|<NULL>|0|0|<NULL>|<NULL>;016|larger_side|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|match_identity_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|result_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|difference_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|reconciliation_match_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|scope_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|scope_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|pair_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|pair_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|difference_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|severity|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|currency|varchar(10)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|difference_amount|bigint|YES|<NULL>|0|0|<NULL>|<NULL>;017|responsible_party_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|rule_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|rule_identity|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|current_lineage_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|action_type|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|adjustment_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|adjustment_idempotency_key|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|original_fact_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|adjustment_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|adjustment_approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;029|adjustment_evidence_ref|varchar(256)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;030|adjustment_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;031|last_rerun_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;032|last_rerun_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;033|last_rerun_rule_version|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;034|last_rerun_balanced|tinyint|YES|<NULL>|0|0|<NULL>|<NULL>;035|last_rerun_evidence_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;036|last_rerun_result_digest|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;037|rerun_count|int|NO|0|0|0|<NULL>|<NULL>;038|created_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;039|adjusted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;040|resolved_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;041|adjusted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;042|resolved_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;043|description|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;044|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|difference_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|action_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|adjustment_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|idempotency_key|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|original_fact_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|adjustment_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|approval_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|reason|varchar(512)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;004|stage_kind|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|stage_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|stage_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|requirement_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|requirement_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|requirement_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|semantic_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|evidence_bundle_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|previous_requirement_identity_owner_namespace|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|previous_requirement_identity_value|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|previous_requirement_version|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|previous_semantic_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|previous_evidence_bundle_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_gate_requirement_pair' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;004|requirement_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|requirement_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|scope_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|scope_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|pair_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|pair_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|rule_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|rule_identity|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_gate_requirement_head' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|stage_kind|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|stage_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|stage_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|current_requirement_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|current_requirement_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|current_requirement_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|current_semantic_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|current_evidence_bundle_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|stage_kind|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|stage_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|stage_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|requirement_identity_owner_namespace|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|requirement_identity_value|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|requirement_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|requirement_semantic_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|requirement_evidence_bundle_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|consumed_pair_evidence|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|decision_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_settlement_order' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|settlement_subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|settlement_subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|settlement_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|settlement_mode|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|settlement_destination|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|trigger_mode|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|timezone|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|cutoff|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|total_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;016|add_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;017|deduct_amount|bigint|NO|0|0|0|<NULL>|<NULL>;018|reserve_amount|bigint|NO|0|0|0|<NULL>|<NULL>;019|net_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;020|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|settlement_approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|lock_funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|release_funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|release_freeze_order_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|release_disposition|varchar(30)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|release_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|release_gate_evidence_ref|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|release_current_lineage_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;029|release_source_closure_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;030|release_authority_decision_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;031|release_authority_evidence_refs|text|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;032|release_approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;033|release_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;034|released_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;035|released_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;036|rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;037|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;038|policy_approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;039|amount_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;040|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;041|policy_snapshot_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;042|order_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;043|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;044|submitted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;045|submitted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;046|approved_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;047|approved_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;048|locked_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;049|locked_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;050|returned_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;051|returned_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;052|return_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;053|cancelled_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;054|cancelled_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;055|cancel_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;056|lock_gate_evidence_ref|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;057|active_order_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;058|failed_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;059|failed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;060|failure_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;061|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_settlement_order_item' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|settlement_order_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|item_type|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|direction|varchar(20)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|source_type|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|source_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;012|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|source_amount_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|active_source_claim|tinyint|YES|<NULL>|0|0|<NULL>|<NULL>;015|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_payout_order' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|settlement_order_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|settlement_subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|settlement_subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;010|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|status|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|payout_account_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|payee_endpoint_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|channel_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|external_rule_evidence_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|payout_gate_evidence_ref|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|admission_decision_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|admission_evidence_refs|text|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|submit_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|external_reference|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|completion_funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|rollback_funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|last_receipt_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|failure_code|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|failure_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|submitted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;029|submitted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;030|completed_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;031|cancelled_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;032|cancelled_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;033|cancel_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;034|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_payout_receipt' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|payout_order_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|channel_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|external_receipt_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|external_reference|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|status|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;012|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|source_receipt_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|normalized_receipt_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|external_occurred_at|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;017|received_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_recovery_order' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|source_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|source_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|responsible_subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|responsible_subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|expected_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;011|recovered_amount|bigint|NO|0|0|0|<NULL>|<NULL>;012|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|status|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|order_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|approval_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|last_funds_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|recovered_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;021|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_recovery_result' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|recovery_order_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|funds_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;008|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|idempotency_key|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|result_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|approval_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|recorded_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
), actual_column_signatures AS (
    SELECT table_name,
           GROUP_CONCAT(
                   CONCAT(
                           LPAD(ordinal_position, 3, '0'), '|', column_name, '|',
                           CASE
                               WHEN data_type IN ('varchar', 'char', 'binary', 'varbinary')
                                   THEN CONCAT(data_type, '(', character_maximum_length, ')')
                               WHEN data_type IN ('datetime', 'timestamp', 'time') AND datetime_precision > 0
                                   THEN CONCAT(data_type, '(', datetime_precision, ')')
                               ELSE data_type
                           END,
                           '|', is_nullable, '|',
                           COALESCE(REPLACE(UPPER(CAST(column_default AS CHAR)),
                                            'CURRENT_TIMESTAMP()', 'CURRENT_TIMESTAMP'), '<NULL>'),
                           '|', IF(LOWER(extra) LIKE '%auto_increment%', 1, 0),
                           '|', IF(LOWER(extra) LIKE '%on update current_timestamp%', 1, 0),
                           '|', COALESCE(character_set_name, '<NULL>'),
                           '|', COALESCE(collation_name, '<NULL>')
                   )
                   ORDER BY ordinal_position SEPARATOR ';'
           ) AS column_signature
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name IN (
        't_clearing_splittable_detail',
        't_clearing_split_batch',
        't_clearing_split_batch_detail',
        't_clearing_split_result_snapshot',
        't_clearing_candidate',
        't_clearing_batch',
        't_clearing_batch_detail',
        't_reconciliation_batch',
        't_reconciliation_batch_lineage',
        't_reconciliation_source_snapshot',
        't_reconciliation_source_item',
        't_reconciliation_run_result',
        't_reconciliation_match_result',
        't_reconciliation_difference',
        't_reconciliation_difference_action',
        't_reconciliation_gate_requirement',
        't_reconciliation_gate_requirement_pair',
        't_reconciliation_gate_requirement_head',
        't_reconciliation_stage_gate_evidence',
        't_settlement_order',
        't_settlement_order_item',
        't_payout_order',
        't_payout_receipt',
        't_recovery_order',
        't_recovery_result'
      )
    GROUP BY table_name
)
SELECT expected.table_name,
       expected.column_signature AS expected_column_signature,
       actual.column_signature AS actual_column_signature
FROM expected_column_signatures expected
LEFT JOIN actual_column_signatures actual ON actual.table_name = expected.table_name
WHERE actual.table_name IS NULL
   OR BINARY actual.column_signature <> BINARY expected.column_signature;

-- 当前基线不包含 unsigned、zerofill 或生成列；返回任何行都表示类型修饰漂移。
SELECT table_name, column_name, column_type, generation_expression
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
        't_clearing_splittable_detail',
        't_clearing_split_batch',
        't_clearing_split_batch_detail',
        't_clearing_split_result_snapshot',
        't_clearing_candidate',
        't_clearing_batch',
        't_clearing_batch_detail',
        't_reconciliation_batch',
        't_reconciliation_batch_lineage',
        't_reconciliation_source_snapshot',
        't_reconciliation_source_item',
        't_reconciliation_run_result',
        't_reconciliation_match_result',
        't_reconciliation_difference',
        't_reconciliation_difference_action',
        't_reconciliation_gate_requirement',
        't_reconciliation_gate_requirement_pair',
        't_reconciliation_gate_requirement_head',
        't_reconciliation_stage_gate_evidence',
        't_settlement_order',
        't_settlement_order_item',
        't_payout_order',
        't_payout_receipt',
        't_recovery_order',
        't_recovery_result'
      )
  AND (LOWER(column_type) REGEXP 'unsigned|zerofill'
       OR COALESCE(generation_expression, '') <> '');


-- 字符串事实必须保持精确比较；返回任何行都表示字符集或排序规则漂移。
SELECT table_name, column_name, character_set_name, collation_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
        't_clearing_splittable_detail',
        't_clearing_split_batch',
        't_clearing_split_batch_detail',
        't_clearing_split_result_snapshot',
        't_clearing_candidate',
        't_clearing_batch',
        't_clearing_batch_detail',
        't_reconciliation_batch',
        't_reconciliation_batch_lineage',
        't_reconciliation_source_snapshot',
        't_reconciliation_source_item',
        't_reconciliation_run_result',
        't_reconciliation_match_result',
        't_reconciliation_difference',
        't_reconciliation_difference_action',
        't_reconciliation_gate_requirement',
        't_reconciliation_gate_requirement_pair',
        't_reconciliation_gate_requirement_head',
        't_reconciliation_stage_gate_evidence',
        't_settlement_order',
        't_settlement_order_item',
        't_payout_order',
        't_payout_receipt',
        't_recovery_order',
        't_recovery_result'
      )
  AND character_set_name IS NOT NULL
  AND (character_set_name <> 'utf8mb4' OR collation_name <> 'utf8mb4_bin');

-- 索引名称、唯一性和字段顺序必须精确匹配；返回任何行都表示索引漂移。
WITH expected_indexes AS (
    SELECT 't_clearing_splittable_detail' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'uk_clearing_splittable_detail_sn' AS index_name, 0 AS non_unique, 'sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'uk_clearing_splittable_detail_entry' AS index_name, 0 AS non_unique, 'tenant_id,ledger_entry_sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_source' AS index_name, 1 AS non_unique, 'tenant_id,funds_transaction_sn,funds_transaction_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_subject' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,split_period' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 'uk_clearing_split_batch_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 'uk_clearing_split_batch_active_digest' AS index_name, 0 AS non_unique, 'tenant_id,active_batch_digest' AS column_names
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 'idx_clearing_split_batch_scope' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,currency,business_line,split_period,status' AS column_names
    UNION ALL SELECT 't_clearing_split_batch' AS table_name, 'idx_clearing_split_batch_status_age' AS index_name, 1 AS non_unique, 'tenant_id,status,gmt_modified' AS column_names
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 'uk_clearing_split_batch_detail_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 'uk_clearing_split_batch_detail_member' AS index_name, 0 AS non_unique, 'tenant_id,split_batch_sn,splittable_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 'uk_clearing_split_batch_detail_active' AS index_name, 0 AS non_unique, 'tenant_id,active_splittable_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_split_batch_detail' AS table_name, 'idx_clearing_split_batch_detail_batch' AS index_name, 1 AS non_unique, 'tenant_id,split_batch_sn' AS column_names
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 'uk_clearing_split_result_snapshot_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 'uk_clearing_split_result_snapshot_detail' AS index_name, 0 AS non_unique, 'tenant_id,split_batch_sn,splittable_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 'uk_clearing_split_result_snapshot_digest' AS index_name, 0 AS non_unique, 'tenant_id,snapshot_digest' AS column_names
    UNION ALL SELECT 't_clearing_split_result_snapshot' AS table_name, 'idx_clearing_split_result_snapshot_subject' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,currency,split_period' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'uk_clearing_candidate_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'uk_clearing_candidate_digest' AS index_name, 0 AS non_unique, 'tenant_id,candidate_digest' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'uk_clearing_candidate_active_detail' AS index_name, 0 AS non_unique, 'tenant_id,active_splittable_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'idx_clearing_candidate_source' AS index_name, 1 AS non_unique, 'tenant_id,split_result_sn,splittable_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'idx_clearing_candidate_subject' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,currency,clearing_period' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'idx_clearing_candidate_status_available' AS index_name, 1 AS non_unique, 'tenant_id,status,clearing_available_time' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'idx_clearing_candidate_status_changed' AS index_name, 1 AS non_unique, 'tenant_id,status,status_changed_time' AS column_names
    UNION ALL SELECT 't_clearing_candidate' AS table_name, 'idx_clearing_candidate_locked_batch' AS index_name, 1 AS non_unique, 'tenant_id,locked_clearing_batch_sn,status' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'uk_clearing_batch_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'uk_clearing_batch_active_digest' AS index_name, 0 AS non_unique, 'tenant_id,active_amount_digest' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'idx_clearing_batch_scope' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,currency,business_line,clearing_period,status' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'idx_clearing_batch_status_age' AS index_name, 1 AS non_unique, 'tenant_id,status,gmt_modified' AS column_names
    UNION ALL SELECT 't_clearing_batch' AS table_name, 'idx_clearing_batch_funds_transaction' AS index_name, 1 AS non_unique, 'tenant_id,funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 'uk_clearing_batch_detail_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 'uk_clearing_batch_detail_candidate' AS index_name, 0 AS non_unique, 'tenant_id,clearing_batch_sn,candidate_sn' AS column_names
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 'idx_clearing_batch_detail_batch' AS index_name, 1 AS non_unique, 'tenant_id,clearing_batch_sn' AS column_names
    UNION ALL SELECT 't_clearing_batch_detail' AS table_name, 'idx_clearing_batch_detail_entry' AS index_name, 1 AS non_unique, 'tenant_id,ledger_entry_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_digest' AS index_name, 0 AS non_unique, 'tenant_id,batch_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_previous' AS index_name, 0 AS non_unique, 'tenant_id,previous_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'idx_reconciliation_batch_scope_pair' AS index_name, 1 AS non_unique, 'tenant_id,scope_owner_namespace,scope_identity_value,pair_owner_namespace,pair_identity_value,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'idx_reconciliation_batch_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'uk_reconciliation_batch_lineage_pair' AS index_name, 0 AS non_unique, 'tenant_id,scope_owner_namespace,scope_identity_value,pair_owner_namespace,pair_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'idx_reconciliation_batch_lineage_current' AS index_name, 1 AS non_unique, 'tenant_id,current_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'uk_reconciliation_source_snapshot_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'uk_reconciliation_source_snapshot_role' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_batch_sn,source_role' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'uk_reconciliation_source_snapshot_identity' AS index_name, 0 AS non_unique, 'tenant_id,snapshot_owner_namespace,snapshot_identity_value,snapshot_version' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'idx_reconciliation_source_snapshot_digest' AS index_name, 1 AS non_unique, 'tenant_id,source_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'uk_reconciliation_source_item_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'uk_reconciliation_source_item_fact' AS index_name, 0 AS non_unique, 'tenant_id,source_snapshot_sn,source_fact_owner_namespace,source_fact_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'idx_reconciliation_source_item_comparison' AS index_name, 1 AS non_unique, 'tenant_id,source_snapshot_sn,comparison_owner_namespace,comparison_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'idx_reconciliation_source_item_semantic_digest' AS index_name, 1 AS non_unique, 'tenant_id,source_snapshot_sn,semantic_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'uk_reconciliation_run_result_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'uk_reconciliation_run_result_business' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'idx_reconciliation_run_result_scope_pair' AS index_name, 1 AS non_unique, 'tenant_id,scope_owner_namespace,scope_identity_value,pair_owner_namespace,pair_identity_value,status' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'idx_reconciliation_run_result_digest' AS index_name, 1 AS non_unique, 'tenant_id,result_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'uk_reconciliation_match_result_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'uk_reconciliation_match_result_identity' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_run_result_sn,match_identity_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'idx_reconciliation_match_result_digest' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_run_result_sn,result_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'idx_reconciliation_match_result_comparison' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_run_result_sn,comparison_owner_namespace,comparison_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'idx_reconciliation_match_result_batch' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'uk_reconciliation_difference_sn' AS index_name, 0 AS non_unique, 'tenant_id,difference_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'uk_reconciliation_difference_match_result' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_match_result_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_batch' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_scope_pair' AS index_name, 1 AS non_unique, 'tenant_id,scope_owner_namespace,scope_identity_value,pair_owner_namespace,pair_identity_value,status' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_lineage' AS index_name, 1 AS non_unique, 'tenant_id,current_lineage_ref,status' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_adjustment' AS index_name, 1 AS non_unique, 'tenant_id,adjustment_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_rerun' AS index_name, 1 AS non_unique, 'tenant_id,last_rerun_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_rerun_batch' AS index_name, 1 AS non_unique, 'tenant_id,last_rerun_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_adjustment' AS index_name, 0 AS non_unique, 'tenant_id,adjustment_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_idempotency' AS index_name, 0 AS non_unique, 'tenant_id,idempotency_key' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'idx_reconciliation_difference_action_difference' AS index_name, 1 AS non_unique, 'tenant_id,difference_sn,id' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, 'uk_reconciliation_gate_requirement_identity' AS index_name, 0 AS non_unique, 'tenant_id,requirement_identity_owner_namespace,requirement_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, 'uk_reconciliation_gate_requirement_stage_version' AS index_name, 0 AS non_unique, 'tenant_id,stage_kind,stage_identity_owner_namespace,stage_identity_value,requirement_version' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement' AS table_name, 'idx_reconciliation_gate_requirement_previous' AS index_name, 1 AS non_unique, 'tenant_id,previous_requirement_identity_owner_namespace,previous_requirement_identity_value,previous_requirement_version' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_pair' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_pair' AS table_name, 'uk_reconciliation_gate_requirement_pair' AS index_name, 0 AS non_unique, 'tenant_id,requirement_identity_owner_namespace,requirement_identity_value,scope_owner_namespace,scope_identity_value,pair_owner_namespace,pair_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_pair' AS table_name, 'idx_reconciliation_gate_requirement_pair_requirement' AS index_name, 1 AS non_unique, 'tenant_id,requirement_identity_owner_namespace,requirement_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_head' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_head' AS table_name, 'uk_reconciliation_gate_requirement_head_stage' AS index_name, 0 AS non_unique, 'tenant_id,stage_kind,stage_identity_owner_namespace,stage_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_gate_requirement_head' AS table_name, 'idx_reconciliation_gate_requirement_head_current' AS index_name, 1 AS non_unique, 'tenant_id,current_requirement_identity_owner_namespace,current_requirement_identity_value,current_requirement_version' AS column_names
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, 'uk_reconciliation_stage_gate_evidence_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, 'uk_reconciliation_stage_gate_evidence_stage' AS index_name, 0 AS non_unique, 'tenant_id,stage_kind,stage_identity_owner_namespace,stage_identity_value' AS column_names
    UNION ALL SELECT 't_reconciliation_stage_gate_evidence' AS table_name, 'idx_reconciliation_stage_gate_evidence_requirement' AS index_name, 1 AS non_unique, 'tenant_id,requirement_identity_owner_namespace,requirement_identity_value,requirement_version' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'uk_settlement_order_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'uk_settlement_order_active_digest' AS index_name, 0 AS non_unique, 'tenant_id,active_order_digest' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'idx_settlement_order_subject' AS index_name, 1 AS non_unique, 'tenant_id,settlement_subject_type,settlement_subject_id,settlement_period' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'idx_settlement_order_status' AS index_name, 1 AS non_unique, 'tenant_id,status,gmt_modified' AS column_names
    UNION ALL SELECT 't_settlement_order' AS table_name, 'idx_settlement_order_lock_transaction' AS index_name, 1 AS non_unique, 'tenant_id,lock_funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 'uk_settlement_order_item_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 'uk_settlement_order_item_source' AS index_name, 0 AS non_unique, 'tenant_id,settlement_order_sn,source_type,source_sn' AS column_names
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 'uk_settlement_item_active_source' AS index_name, 0 AS non_unique, 'tenant_id,source_type,source_sn,active_source_claim' AS column_names
    UNION ALL SELECT 't_settlement_order_item' AS table_name, 'idx_settlement_order_item_order' AS index_name, 1 AS non_unique, 'tenant_id,settlement_order_sn' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'uk_payout_order_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'uk_payout_order_settlement' AS index_name, 0 AS non_unique, 'tenant_id,settlement_order_sn' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'uk_payout_order_external' AS index_name, 0 AS non_unique, 'tenant_id,channel_ref,external_reference' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'idx_payout_order_status' AS index_name, 1 AS non_unique, 'tenant_id,status,gmt_modified' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'idx_payout_order_completion' AS index_name, 1 AS non_unique, 'tenant_id,completion_funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_payout_order' AS table_name, 'idx_payout_order_rollback' AS index_name, 1 AS non_unique, 'tenant_id,rollback_funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_payout_receipt' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_payout_receipt' AS table_name, 'uk_payout_receipt_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_payout_receipt' AS table_name, 'uk_payout_receipt_source' AS index_name, 0 AS non_unique, 'tenant_id,channel_ref,external_receipt_ref' AS column_names
    UNION ALL SELECT 't_payout_receipt' AS table_name, 'idx_payout_receipt_order' AS index_name, 1 AS non_unique, 'tenant_id,payout_order_sn,id' AS column_names
    UNION ALL SELECT 't_payout_receipt' AS table_name, 'idx_payout_receipt_external' AS index_name, 1 AS non_unique, 'tenant_id,channel_ref,external_reference' AS column_names
    UNION ALL SELECT 't_recovery_order' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_recovery_order' AS table_name, 'uk_recovery_order_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_recovery_order' AS table_name, 'uk_recovery_order_source' AS index_name, 0 AS non_unique, 'tenant_id,source_type,source_sn,responsible_subject_type,responsible_subject_id,currency' AS column_names
    UNION ALL SELECT 't_recovery_order' AS table_name, 'idx_recovery_order_subject_status' AS index_name, 1 AS non_unique, 'tenant_id,responsible_subject_type,responsible_subject_id,status,gmt_modified' AS column_names
    UNION ALL SELECT 't_recovery_order' AS table_name, 'idx_recovery_order_last_transaction' AS index_name, 1 AS non_unique, 'tenant_id,last_funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_recovery_result' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_recovery_result' AS table_name, 'uk_recovery_result_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_recovery_result' AS table_name, 'uk_recovery_result_transaction' AS index_name, 0 AS non_unique, 'tenant_id,funds_transaction_sn' AS column_names
    UNION ALL SELECT 't_recovery_result' AS table_name, 'uk_recovery_result_idempotency' AS index_name, 0 AS non_unique, 'tenant_id,idempotency_key' AS column_names
    UNION ALL SELECT 't_recovery_result' AS table_name, 'idx_recovery_result_order' AS index_name, 1 AS non_unique, 'tenant_id,recovery_order_sn,id' AS column_names
), actual_index_columns AS (
    SELECT table_name,
           index_name,
           MAX(non_unique) AS non_unique,
           GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS column_names
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    GROUP BY table_name, index_name
)
SELECT expected.table_name,
       expected.index_name,
       expected.non_unique AS expected_non_unique,
       actual.non_unique AS actual_non_unique,
       expected.column_names AS expected_column_names,
       actual.column_names AS actual_column_names
FROM expected_indexes expected
LEFT JOIN actual_index_columns actual
       ON actual.table_name = expected.table_name
      AND actual.index_name = expected.index_name
WHERE actual.index_name IS NULL
   OR actual.non_unique <> expected.non_unique
   OR actual.column_names <> expected.column_names;
