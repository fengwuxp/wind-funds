package com.wind.funds.reconciliation.schema;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主资金链、对账与治理 MySQL 生产 DDL 和测试 schema 漂移守卫。
 */
class ReconciliationMysqlDdlContractTests {

    private static final List<String> CORE_TABLE_NAMES = List.of(
            "t_funding_account",
            "t_credit_account",
            "t_account_hierarchy_relation",
            "t_spend_control_scope",
            "t_payment_instrument",
            "t_payment_instrument_binding",
            "t_payment_instrument_binding_history",
            "t_spend_subject_funding_rel",
            "t_spend_rule_definition",
            "t_spend_rule_version",
            "t_spend_rule_binding",
            "t_spend_rule_decision_record",
            "t_spend_control_movement",
            "t_funds_transaction",
            "t_funds_transaction_detail",
            "t_funds_frozen_order",
            "t_ledger",
            "t_ledger_transaction",
            "t_ledger_posting_plan",
            "t_ledger_entry");

    private static final List<String> TABLE_NAMES = List.of(
            "t_clearing_splittable_detail",
            "t_clearing_split_batch",
            "t_clearing_split_batch_detail",
            "t_clearing_split_result_snapshot",
            "t_clearing_candidate",
            "t_clearing_batch",
            "t_clearing_batch_detail",
            "t_settlement_order",
            "t_settlement_order_item",
            "t_payout_order",
            "t_payout_receipt",
            "t_recovery_order",
            "t_recovery_result",
            "t_reconciliation_batch",
            "t_reconciliation_batch_lineage",
            "t_reconciliation_source_snapshot",
            "t_reconciliation_source_item",
            "t_reconciliation_run_result",
            "t_reconciliation_match_result",
            "t_reconciliation_difference",
            "t_reconciliation_difference_action",
            "t_reconciliation_gate_requirement",
            "t_reconciliation_gate_requirement_pair",
            "t_reconciliation_gate_requirement_head",
            "t_reconciliation_stage_gate_evidence");

    private static final List<String> GOVERNANCE_TABLE_NAMES = List.of(
            "t_projection_replay_task",
            "t_projection_replay_difference",
            "t_funds_transaction_projection");

    private static final List<String> LIFECYCLE_CORE_TABLE_NAMES = List.of(
            "t_funding_account",
            "t_credit_account",
            "t_spend_control_scope",
            "t_payment_instrument",
            "t_payment_instrument_binding",
            "t_spend_rule_definition",
            "t_spend_rule_version",
            "t_spend_rule_binding",
            "t_funds_transaction",
            "t_funds_transaction_detail",
            "t_funds_frozen_order",
            "t_ledger");

    private static final List<String> LIFECYCLE_RECONCILIATION_TABLE_NAMES = List.of(
            "t_clearing_split_batch",
            "t_clearing_candidate",
            "t_clearing_batch",
            "t_reconciliation_batch",
            "t_reconciliation_difference",
            "t_settlement_order",
            "t_payout_order",
            "t_payout_receipt",
            "t_recovery_order");

    @Test
    void testCoreForwardDdlShouldMatchTestSchemaWithoutDestructiveStatements() throws IOException {
        String testSchema = Files.readString(workspaceRoot().resolve("tests/src/test/resources/jdbc-schema.sql"));
        String forwardDdl = Files.readString(
                workspaceRoot().resolve("database/mysql/core/001_create_core_tables.sql"));

        assertThat(forwardDdl)
                .doesNotContain("DROP TABLE")
                .doesNotContain("CREATE TABLE IF NOT EXISTS");
        for (String tableName : CORE_TABLE_NAMES) {
            assertThat(normalize(extractCreateTable(forwardDdl, tableName)))
                    .as("MySQL core DDL table %s must match the executable test schema", tableName)
                    .isEqualTo(normalize(extractCreateTable(testSchema, tableName)));
        }
    }

    @Test
    void testForwardDdlShouldMatchTestSchemaWithoutDestructiveStatements() throws IOException {
        String testSchema = Files.readString(workspaceRoot().resolve("tests/src/test/resources/jdbc-schema.sql"));
        String forwardDdl = readDatabaseFile("001_create_reconciliation_tables.sql");

        assertThat(forwardDdl)
                .doesNotContain("DROP TABLE")
                .doesNotContain("CREATE TABLE IF NOT EXISTS")
                .containsOnlyOnce("CREATE TABLE `t_clearing_splittable_detail`");
        for (String tableName : TABLE_NAMES) {
            String forwardTableDdl = extractCreateTable(forwardDdl, tableName);
            assertThat(forwardTableDdl)
                    .as("MySQL DDL table %s must use exact string identity", tableName)
                    .containsOnlyOnce("DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin");
            assertThat(normalize(forwardTableDdl))
                    .as("MySQL DDL table %s must match the executable test schema", tableName)
                    .isEqualTo(normalize(extractCreateTable(testSchema, tableName)));
        }
    }

    @Test
    void testGovernanceForwardDdlShouldMatchTestSchemaWithoutDestructiveStatements() throws IOException {
        String testSchema = Files.readString(workspaceRoot().resolve("tests/src/test/resources/jdbc-schema.sql"));
        String forwardDdl = Files.readString(
                workspaceRoot().resolve("database/mysql/governance/001_create_governance_tables.sql"));

        assertThat(forwardDdl)
                .doesNotContain("DROP TABLE")
                .doesNotContain("CREATE TABLE IF NOT EXISTS");
        for (String tableName : GOVERNANCE_TABLE_NAMES) {
            assertThat(normalize(extractCreateTable(forwardDdl, tableName)))
                    .as("MySQL governance DDL table %s must match the executable test schema", tableName)
                    .isEqualTo(normalize(extractCreateTable(testSchema, tableName)));
        }
        assertThat(workspaceRoot().resolve("database/mysql/governance/001_drop_governance_tables.sql"))
                .as("production DDL must not publish a destructive table-drop rollback")
                .doesNotExist();
    }

    /**
     * 场景：生命周期、准入结果、对账结论和支付工具方向跨 MySQL 与 H2 持久化。
     * 预期：物理列使用与 Java 业务属性一致的 state、result、outcome 或 direction 名称。
     * 红线：不得继续用通用 status 或同义列名依赖注解、别名或桥接 setter 映射。
     */
    @Test
    void testPhysicalColumnsShouldUseCanonicalBusinessNames() throws IOException {
        String testSchema = Files.readString(workspaceRoot().resolve("tests/src/test/resources/jdbc-schema.sql"));
        String coreDdl = Files.readString(
                workspaceRoot().resolve("database/mysql/core/001_create_core_tables.sql"));
        String reconciliationDdl = readDatabaseFile("001_create_reconciliation_tables.sql");
        String governanceDdl = Files.readString(
                workspaceRoot().resolve("database/mysql/governance/001_create_governance_tables.sql"));

        for (String tableName : LIFECYCLE_CORE_TABLE_NAMES) {
            assertUsesStateColumn(coreDdl, testSchema, tableName);
        }
        for (String tableName : LIFECYCLE_RECONCILIATION_TABLE_NAMES) {
            assertUsesStateColumn(reconciliationDdl, testSchema, tableName);
        }
        assertUsesStateColumn(governanceDdl, testSchema, "t_projection_replay_task");

        assertThat(extractCreateTable(reconciliationDdl, "t_clearing_splittable_detail"))
                .contains("`admission_result`", "`reconciliation_decision_result`")
                .doesNotContain("`status`", "`reconciliation_decision_status`");
        assertThat(extractCreateTable(testSchema, "t_clearing_splittable_detail"))
                .contains("`admission_result`", "`reconciliation_decision_result`")
                .doesNotContain("`status`", "`reconciliation_decision_status`");
        assertThat(extractCreateTable(reconciliationDdl, "t_reconciliation_run_result"))
                .contains("`outcome`")
                .doesNotContain("`status`");
        assertThat(extractCreateTable(testSchema, "t_reconciliation_run_result"))
                .contains("`outcome`")
                .doesNotContain("`status`");
        assertThat(extractCreateTable(coreDdl, "t_payment_instrument"))
                .contains("`flow_direction`")
                .doesNotContain("`instrument_direction`");
        assertThat(extractCreateTable(testSchema, "t_payment_instrument"))
                .contains("`flow_direction`")
                .doesNotContain("`instrument_direction`");
    }

    @Test
    void testDeploymentAssetsShouldCoverEveryReconciliationTable() throws IOException {
        String verificationDdl = readDatabaseFile("001_verify_reconciliation_tables.sql");
        String forwardDdl = readDatabaseFile("001_create_reconciliation_tables.sql");

        assertThat(verificationDdl)
                .doesNotContain("undefined")
                .doesNotContain("UNION ALL SELECT '' AS table_name")
                .contains("column_name", "data_type", "is_nullable", "column_default", "extra",
                        "character_set_name", "collation_name", "column_type", "datetime_precision",
                        "generation_expression");
        for (String tableName : TABLE_NAMES) {
            assertThat(verificationDdl).contains("'" + tableName + "'");
            String tableDdl = extractCreateTable(forwardDdl, tableName);
            Matcher tableCounts = Pattern.compile(
                            "(?:SELECT|UNION ALL SELECT)\\s+'" + Pattern.quote(tableName)
                                    + "'(?:\\s+AS\\s+table_name)?,\\s*(\\d+)(?:\\s+AS\\s+column_count)?,"
                                    + "\\s*(\\d+)(?:\\s+AS\\s+index_count)?")
                    .matcher(verificationDdl);
            assertThat(tableCounts.find()).as("missing verification counts for %s", tableName).isTrue();
            assertThat(Integer.parseInt(tableCounts.group(1)))
                    .as("verification column count for %s", tableName)
                    .isEqualTo(columnCount(tableDdl));
            assertThat(Integer.parseInt(tableCounts.group(2)))
                    .as("verification index count for %s", tableName)
                    .isEqualTo(indexCount(tableDdl));
            assertThat(verificationDdl).contains("'" + tableName + "' AS table_name, '"
                    + columnSignature(tableDdl) + "' AS column_signature");
            Matcher indexes = Pattern.compile(
                            "(?m)^\\s*(PRIMARY KEY|UNIQUE KEY|KEY)(?:\\s+`([^`]+)`)?\\s*\\(([^)]+)\\)")
                    .matcher(tableDdl);
            while (indexes.find()) {
                String indexType = indexes.group(1);
                String indexName = "PRIMARY KEY".equals(indexType) ? "PRIMARY" : indexes.group(2);
                int nonUnique = "KEY".equals(indexType) ? 1 : 0;
                String columnNames = indexes.group(3).replace("`", "").replaceAll("\\s+", "");
                assertThat(verificationDdl).contains("'" + tableName + "' AS table_name, '"
                        + indexName + "' AS index_name, " + nonUnique + " AS non_unique, '"
                        + columnNames + "' AS column_names");
            }
        }
        assertThat(workspaceRoot().resolve("database/mysql/reconciliation/001_drop_reconciliation_tables.sql"))
                .as("production DDL must not publish a destructive table-drop rollback")
                .doesNotExist();
    }

    @Test
    void testMysqlReconciliationCommandShouldRequireDedicatedDatabase() throws IOException {
        String justfile = Files.readString(workspaceRoot().resolve("Justfile"));
        String integrationTest = Files.readString(workspaceRoot().resolve(
                "tests/src/test/java/com/wind/funds/reconciliation/schema/"
                        + "ReconciliationMysqlMigrationIntegrationTests.java"));

        assertThat(justfile)
                .contains("database_name=\"${database_url##*/}\"")
                .contains("[[ \"$database_name\" == \"wind_funds_reconciliation_test\" ]]")
                .contains("WIND_FUNDS_TEST_MYSQL_EXPECTED_VERSION_PREFIX")
                .contains("ReconciliationMysqlMigrationIntegrationTests")
                .contains("PaymentInstrumentTransactionAuthorizationTests")
                .contains("SpendControlTransactionConsumptionApplicationServiceTests")
                .contains("WIND_FUNDS_TEST_SQL_INIT_MODE=never")
                .doesNotContain("WIND_FUNDS_TEST_SQL_INIT_MODE=always");
        assertThat(integrationTest)
                .contains("database/mysql/core", "database/mysql/governance",
                        "001_create_core_tables.sql", "001_create_governance_tables.sql", "hasSize(48)",
                        "verifyTargetTables", "verifyTableStructures",
                        "information_schema.columns", "information_schema.statistics")
                .contains("verifyRecoveryConflictRecoveryUsesCurrentRead",
                        "t_recovery_order", "t_recovery_result",
                        "idempotency_key", "funds_transaction_sn")
                .contains("verifyFundsTransactionBusinessKeyConflictRecoveryUsesCurrentRead",
                        "t_funds_transaction", "business_scene", "business_sn");
    }

    @Test
    void testMysqlReadmeShouldDescribeCurrentMigrationScope() throws IOException {
        String readme = readDatabaseFile("README.md");

        assertThat(readme)
                .contains("二十五张表", "三张治理", "四十八张表", "just test-mysql-reconciliation",
                        "支付工具授权", "支出控制")
                .doesNotContain("十九张表");
    }

    @Test
    void testOperationalDiscoveryQueriesShouldHaveMatchingIndexes() throws IOException {
        String forwardDdl = readDatabaseFile("001_create_reconciliation_tables.sql");

        assertThat(extractCreateTable(forwardDdl, "t_clearing_split_batch"))
                .contains("KEY `idx_clearing_split_batch_state_age` "
                        + "(`tenant_id`, `state`, `gmt_modified`)");
        assertThat(extractCreateTable(forwardDdl, "t_clearing_candidate"))
                .contains("KEY `idx_clearing_candidate_state_available` "
                        + "(`tenant_id`, `state`, `clearing_available_time`)")
                .contains("KEY `idx_clearing_candidate_state_changed` "
                        + "(`tenant_id`, `state`, `state_changed_time`)")
                .contains("KEY `idx_clearing_candidate_locked_batch` "
                        + "(`tenant_id`, `locked_clearing_batch_sn`, `state`)");
        assertThat(extractCreateTable(forwardDdl, "t_clearing_batch"))
                .contains("KEY `idx_clearing_batch_state_age` "
                        + "(`tenant_id`, `state`, `gmt_modified`)");
        assertThat(extractCreateTable(forwardDdl, "t_settlement_order_item"))
                .contains("UNIQUE KEY `uk_settlement_item_active_source` "
                        + "(`tenant_id`, `source_type`, `source_sn`, `active_source_claim`)");
        assertThat(extractCreateTable(forwardDdl, "t_recovery_order"))
                .contains("UNIQUE KEY `uk_recovery_order_source` "
                        + "(`tenant_id`, `source_type`, `source_sn`, `responsible_subject_type`, "
                        + "`responsible_subject_id`, `currency`)");
        assertThat(extractCreateTable(forwardDdl, "t_recovery_result"))
                .contains("UNIQUE KEY `uk_recovery_result_transaction` "
                        + "(`tenant_id`, `funds_transaction_sn`)")
                .contains("UNIQUE KEY `uk_recovery_result_idempotency` "
                        + "(`tenant_id`, `idempotency_key`)");
    }

    private void assertUsesStateColumn(String mysqlDdl, String testSchema, String tableName) {
        assertThat(extractCreateTable(mysqlDdl, tableName))
                .contains("`state`")
                .doesNotContain("`status`");
        assertThat(extractCreateTable(testSchema, tableName))
                .contains("`state`")
                .doesNotContain("`status`");
    }

    private String readDatabaseFile(String fileName) throws IOException {
        Path path = workspaceRoot().resolve("database/mysql/reconciliation").resolve(fileName);
        assertThat(path).exists().isRegularFile();
        return Files.readString(path);
    }

    private String extractCreateTable(String sql, String tableName) {
        String prefix = "CREATE TABLE `" + tableName + "`";
        int start = sql.indexOf(prefix);
        assertThat(start).as("missing CREATE TABLE for %s", tableName).isGreaterThanOrEqualTo(0);
        int end = sql.indexOf(';', start);
        assertThat(end).as("missing statement terminator for %s", tableName).isGreaterThan(start);
        return sql.substring(start, end + 1);
    }

    private String normalize(String sql) {
        return sql.replace("COLLATE = utf8mb4_bin", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int columnCount(String tableDdl) {
        Matcher columns = Pattern.compile("(?m)^\\s*`[^`]+`\\s+[A-Z]+").matcher(tableDdl);
        int count = 0;
        while (columns.find()) {
            count++;
        }
        return count;
    }

    private int indexCount(String tableDdl) {
        Matcher indexes = Pattern.compile("(?m)^\\s*(?:PRIMARY KEY|UNIQUE KEY|KEY)").matcher(tableDdl);
        int count = 0;
        while (indexes.find()) {
            count++;
        }
        return count;
    }

    private String columnSignature(String tableDdl) {
        Matcher columns = Pattern.compile(
                        "(?m)^\\s*`([^`]+)`\\s+([A-Z]+(?:\\(\\d+\\))?)\\s+(.+?)(?:,)?$")
                .matcher(tableDdl);
        List<String> signatures = new ArrayList<>();
        while (columns.find()) {
            String type = columns.group(2).toLowerCase();
            if (type.matches("(?:bigint|int|tinyint)\\(\\d+\\)")) {
                type = type.substring(0, type.indexOf('('));
            }
            String definition = columns.group(3);
            Matcher defaultValue = Pattern.compile("DEFAULT\\s+(CURRENT_TIMESTAMP|NULL|\\d+)")
                    .matcher(definition);
            String normalizedDefault = defaultValue.find() && !"NULL".equals(defaultValue.group(1))
                    ? defaultValue.group(1) : "<NULL>";
            boolean characterType = type.startsWith("varchar") || type.contains("text");
            signatures.add("%03d|%s|%s|%s|%s|%d|%d|%s|%s".formatted(
                    signatures.size() + 1,
                    columns.group(1),
                    type,
                    definition.contains("NOT NULL") ? "NO" : "YES",
                    normalizedDefault,
                    definition.contains("AUTO_INCREMENT") ? 1 : 0,
                    definition.contains("ON UPDATE CURRENT_TIMESTAMP") ? 1 : 0,
                    characterType ? "utf8mb4" : "<NULL>",
                    characterType ? "utf8mb4_bin" : "<NULL>"));
        }
        return String.join(";", signatures);
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        return "tests".equals(current.getFileName().toString()) ? current.getParent() : current;
    }
}
