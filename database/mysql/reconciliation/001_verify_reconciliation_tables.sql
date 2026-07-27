-- 执行 001_create_reconciliation_tables.sql 后运行。
-- 除版本信息外，其余结构校验结果集均为空才表示与当前基线一致；非空必须停止发布并处理漂移。

SELECT @@version AS mysql_version,
       @@transaction_isolation AS transaction_isolation;

WITH expected_tables AS (
    SELECT 't_clearing_splittable_detail' AS table_name, 27 AS column_count, 6 AS index_count
    UNION ALL SELECT 't_reconciliation_batch', 20, 7
    UNION ALL SELECT 't_reconciliation_batch_lineage', 8, 3
    UNION ALL SELECT 't_reconciliation_source_snapshot', 11, 4
    UNION ALL SELECT 't_reconciliation_source_item', 8, 4
    UNION ALL SELECT 't_reconciliation_run_result', 19, 6
    UNION ALL SELECT 't_reconciliation_match_result', 18, 5
    UNION ALL SELECT 't_reconciliation_difference', 41, 9
    UNION ALL SELECT 't_reconciliation_difference_action', 14, 5
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
    SELECT 't_clearing_splittable_detail' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|funds_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|source_transaction_version|int|NO|<NULL>|0|0|<NULL>|<NULL>;008|funds_transaction_detail_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|ledger_transaction_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|posting_plan_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|ledger_entry_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|subject_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|subject_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|currency|varchar(10)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|principal_amount|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;016|refund_amount|bigint|NO|0|0|0|<NULL>|<NULL>;017|clearing_period|varchar(30)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|rule_code|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|exclusion_reason|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|reconciliation_decision_status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|reconciliation_run_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|reconciliation_result_digest|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|reconciliation_evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|reconciliation_scope_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|gate_object_type|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|gate_object_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|window_start|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;011|window_end|datetime|NO|<NULL>|0|0|<NULL>|<NULL>;012|timezone_id|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|previous_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|run_result_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|aborted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|aborted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;018|abort_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|batch_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_scope_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|gate_object_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|gate_object_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|current_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|source_role|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|source_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|record_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;010|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|source_snapshot_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|source_item_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|content_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|reconciliation_scope_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|gate_object_type|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|gate_object_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|reference_source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|comparison_source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|source_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|result_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;015|total_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;016|matched_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;017|difference_count|int|NO|<NULL>|0|0|<NULL>|<NULL>;018|evidence_refs|mediumtext|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|reconciliation_run_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|reference_source_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|comparison_source_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|source_quality|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|match_strength|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|difference_type|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|severity|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|currency|varchar(10)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|difference_amount|bigint|YES|<NULL>|0|0|<NULL>|<NULL>;015|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|match_identity_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|match_digest|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|gmt_modified|datetime|NO|CURRENT_TIMESTAMP|0|1|<NULL>|<NULL>;004|difference_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;005|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;006|reconciliation_batch_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|reconciliation_match_result_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|source_quality|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|match_strength|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|difference_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|severity|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|status|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|currency|varchar(10)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|difference_amount|bigint|YES|<NULL>|0|0|<NULL>|<NULL>;015|responsible_party_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;016|blocking_object_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;017|blocking_object_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;018|rule_version|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;019|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;020|action_type|varchar(50)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;021|adjustment_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;022|adjustment_idempotency_key|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;023|original_fact_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;024|adjustment_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;025|adjustment_approval_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;026|adjustment_evidence_ref|varchar(256)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;027|adjustment_reason|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;028|last_rerun_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;029|last_rerun_batch_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;030|last_rerun_rule_version|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;031|last_rerun_balanced|tinyint|YES|<NULL>|0|0|<NULL>|<NULL>;032|last_rerun_evidence_ref|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;033|last_rerun_result_digest|varchar(128)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;034|rerun_count|int|NO|0|0|0|<NULL>|<NULL>;035|created_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;036|adjusted_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;037|resolved_by|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;038|adjusted_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;039|resolved_time|datetime|YES|<NULL>|0|0|<NULL>|<NULL>;040|description|varchar(512)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;041|version|int|NO|0|0|0|<NULL>|<NULL>' AS column_signature
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, '001|id|bigint|NO|<NULL>|1|0|<NULL>|<NULL>;002|gmt_create|datetime|NO|CURRENT_TIMESTAMP|0|0|<NULL>|<NULL>;003|sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;004|tenant_id|bigint|NO|<NULL>|0|0|<NULL>|<NULL>;005|difference_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;006|action_type|varchar(50)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;007|adjustment_sn|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;008|idempotency_key|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;009|original_fact_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;010|adjustment_transaction_sn|varchar(64)|YES|<NULL>|0|0|utf8mb4|utf8mb4_bin;011|approval_ref|varchar(128)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;012|evidence_ref|varchar(256)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;013|reason|varchar(512)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin;014|created_by|varchar(64)|NO|<NULL>|0|0|utf8mb4|utf8mb4_bin' AS column_signature
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
            't_reconciliation_batch',
            't_reconciliation_batch_lineage',
            't_reconciliation_source_snapshot',
            't_reconciliation_source_item',
            't_reconciliation_run_result',
            't_reconciliation_match_result',
            't_reconciliation_difference',
            't_reconciliation_difference_action'
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
        't_reconciliation_batch',
        't_reconciliation_batch_lineage',
        't_reconciliation_source_snapshot',
        't_reconciliation_source_item',
        't_reconciliation_run_result',
        't_reconciliation_match_result',
        't_reconciliation_difference',
        't_reconciliation_difference_action'
  )
  AND (LOWER(column_type) REGEXP 'unsigned|zerofill'
       OR COALESCE(generation_expression, '') <> '');


-- 字符串事实必须保持精确比较；返回任何行都表示字符集或排序规则漂移。
SELECT table_name, column_name, character_set_name, collation_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
        't_clearing_splittable_detail',
        't_reconciliation_batch',
        't_reconciliation_batch_lineage',
        't_reconciliation_source_snapshot',
        't_reconciliation_source_item',
        't_reconciliation_run_result',
        't_reconciliation_match_result',
        't_reconciliation_difference',
        't_reconciliation_difference_action'
  )
  AND character_set_name IS NOT NULL
  AND (character_set_name <> 'utf8mb4' OR collation_name <> 'utf8mb4_bin');

-- 索引名称、唯一性和字段顺序必须精确匹配；返回任何行都表示索引漂移。
WITH expected_indexes AS (
    SELECT 't_clearing_splittable_detail' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'uk_clearing_splittable_detail_sn' AS index_name, 0 AS non_unique, 'sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'uk_clearing_splittable_detail_entry' AS index_name, 0 AS non_unique, 'tenant_id,ledger_entry_sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_source' AS index_name, 1 AS non_unique, 'tenant_id,funds_transaction_sn,funds_transaction_detail_sn' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_subject' AS index_name, 1 AS non_unique, 'tenant_id,subject_type,subject_id,clearing_period' AS column_names
    UNION ALL SELECT 't_clearing_splittable_detail' AS table_name, 'idx_clearing_splittable_detail_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_digest' AS index_name, 0 AS non_unique, 'tenant_id,batch_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'uk_reconciliation_batch_previous' AS index_name, 0 AS non_unique, 'tenant_id,previous_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'idx_reconciliation_batch_scope' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_scope_ref,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'idx_reconciliation_batch_gate' AS index_name, 1 AS non_unique, 'tenant_id,gate_object_type,gate_object_sn,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch' AS table_name, 'idx_reconciliation_batch_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'uk_reconciliation_batch_lineage_object' AS index_name, 0 AS non_unique, 'tenant_id,gate_object_type,gate_object_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_batch_lineage' AS table_name, 'idx_reconciliation_batch_lineage_current' AS index_name, 1 AS non_unique, 'tenant_id,current_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'uk_reconciliation_source_snapshot_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'uk_reconciliation_source_snapshot_role' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_batch_sn,source_role' AS column_names
    UNION ALL SELECT 't_reconciliation_source_snapshot' AS table_name, 'idx_reconciliation_source_snapshot_digest' AS index_name, 1 AS non_unique, 'tenant_id,source_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'uk_reconciliation_source_item_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'uk_reconciliation_source_item_ref' AS index_name, 0 AS non_unique, 'tenant_id,source_snapshot_sn,source_item_ref' AS column_names
    UNION ALL SELECT 't_reconciliation_source_item' AS table_name, 'idx_reconciliation_source_content_digest' AS index_name, 1 AS non_unique, 'tenant_id,source_snapshot_sn,content_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'uk_reconciliation_run_result_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'uk_reconciliation_run_result_business' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'idx_reconciliation_run_result_scope' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_scope_ref,status' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'idx_reconciliation_run_result_gate' AS index_name, 1 AS non_unique, 'tenant_id,gate_object_type,gate_object_sn,status' AS column_names
    UNION ALL SELECT 't_reconciliation_run_result' AS table_name, 'idx_reconciliation_run_result_digest' AS index_name, 1 AS non_unique, 'tenant_id,result_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'uk_reconciliation_match_result_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'uk_reconciliation_match_result_identity' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_run_result_sn,match_identity_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'idx_reconciliation_match_result_digest' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_run_result_sn,match_digest' AS column_names
    UNION ALL SELECT 't_reconciliation_match_result' AS table_name, 'idx_reconciliation_match_result_batch' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'uk_reconciliation_difference_sn' AS index_name, 0 AS non_unique, 'tenant_id,difference_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'uk_reconciliation_difference_match_result' AS index_name, 0 AS non_unique, 'tenant_id,reconciliation_match_result_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_batch' AS index_name, 1 AS non_unique, 'tenant_id,reconciliation_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_status' AS index_name, 1 AS non_unique, 'tenant_id,status' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_blocking_object' AS index_name, 1 AS non_unique, 'tenant_id,blocking_object_type,blocking_object_sn,status' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_adjustment' AS index_name, 1 AS non_unique, 'tenant_id,adjustment_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_rerun' AS index_name, 1 AS non_unique, 'tenant_id,last_rerun_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference' AS table_name, 'idx_reconciliation_difference_rerun_batch' AS index_name, 1 AS non_unique, 'tenant_id,last_rerun_batch_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'PRIMARY' AS index_name, 0 AS non_unique, 'id' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_sn' AS index_name, 0 AS non_unique, 'tenant_id,sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_adjustment' AS index_name, 0 AS non_unique, 'tenant_id,adjustment_sn' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'uk_reconciliation_difference_action_idempotency' AS index_name, 0 AS non_unique, 'tenant_id,idempotency_key' AS column_names
    UNION ALL SELECT 't_reconciliation_difference_action' AS table_name, 'idx_reconciliation_difference_action_difference' AS index_name, 1 AS non_unique, 'tenant_id,difference_sn,id' AS column_names
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
