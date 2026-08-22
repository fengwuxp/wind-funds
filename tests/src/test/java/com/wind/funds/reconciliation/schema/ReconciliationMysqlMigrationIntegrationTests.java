package com.wind.funds.reconciliation.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在显式授权的专用 MySQL 库上执行主资金链、对账和治理生产 DDL 与部署回读。
 */
@EnabledIfEnvironmentVariable(named = "WIND_FUNDS_TEST_MYSQL_DESTRUCTIVE", matches = "true")
class ReconciliationMysqlMigrationIntegrationTests {

    private static final String EXPECTED_DATABASE = "wind_funds_reconciliation_test";

    private static final int EXPECTED_VERIFICATION_RESULT_SET_COUNT = 6;

    @Test
    void testForwardMigrationAndVerificationShouldPassOnTargetMysql() throws Exception {
        Path coreDdl = workspaceRoot().resolve("database/mysql/core/001_create_core_tables.sql");
        Path reconciliationDirectory = workspaceRoot().resolve("database/mysql/reconciliation");
        Path reconciliationDdl = reconciliationDirectory.resolve("001_create_reconciliation_tables.sql");
        Path verificationDdl = reconciliationDirectory.resolve("001_verify_reconciliation_tables.sql");
        Path governanceDdl = workspaceRoot().resolve(
                "database/mysql/governance/001_create_governance_tables.sql");
        String expectedVersionPrefix = requiredEnvironment("WIND_FUNDS_TEST_MYSQL_EXPECTED_VERSION_PREFIX");
        String coreDdlSql = Files.readString(coreDdl);
        String governanceDdlSql = Files.readString(governanceDdl);
        List<String> coreTableNames = extractTableNames(coreDdlSql);
        List<String> governanceTableNames = extractTableNames(governanceDdlSql);
        List<String> tableNames = new ArrayList<>(coreTableNames);
        tableNames.addAll(extractTableNames(Files.readString(reconciliationDdl)));
        tableNames.addAll(governanceTableNames);
        assertThat(tableNames).doesNotHaveDuplicates().hasSize(48);

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = openConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).isEqualTo(EXPECTED_DATABASE);
            dropTables(connection, tableNames);
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(coreDdl));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(reconciliationDdl));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(governanceDdl));
            verifyTargetTables(connection, tableNames);
            verifyTableStructures(connection, coreDdlSql, coreTableNames);
            verifyTableStructures(connection, governanceDdlSql, governanceTableNames);
            assertThat(verifyDeployment(connection, Files.readString(verificationDdl), expectedVersionPrefix))
                    .isEqualTo(EXPECTED_VERIFICATION_RESULT_SET_COUNT);
            verifyFundsTransactionBusinessKeyConflictRecoveryUsesCurrentRead();
            verifyPayoutReceiptConflictRecoveryUsesCurrentRead();
            verifyRecoveryConflictRecoveryUsesCurrentRead();
            cleanupConflictFixtures(connection);
        }
    }

    private void verifyTargetTables(Connection connection, List<String> tableNames) throws SQLException {
        List<String> actualTableNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """)) {
            statement.setString(1, EXPECTED_DATABASE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    actualTableNames.add(resultSet.getString("table_name"));
                }
            }
        }
        assertThat(actualTableNames).containsExactlyInAnyOrderElementsOf(tableNames);

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                  AND (engine <> 'InnoDB' OR table_collation <> 'utf8mb4_bin')
                """)) {
            statement.setString(1, EXPECTED_DATABASE);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("all target tables must use InnoDB and utf8mb4_bin").isFalse();
            }
        }
    }

    private void verifyTableStructures(Connection connection, String ddl,
                                       List<String> tableNames) throws SQLException {
        for (String tableName : tableNames) {
            String tableDdl = extractCreateTable(ddl, tableName);
            assertThat(readColumnSignatures(connection, tableName))
                    .as("MySQL core table %s columns must match forward DDL", tableName)
                    .containsExactlyElementsOf(parseColumnSignatures(tableDdl));
            assertThat(readIndexSignatures(connection, tableName))
                    .as("MySQL core table %s indexes must match forward DDL", tableName)
                    .containsExactlyInAnyOrderElementsOf(parseIndexSignatures(tableDdl));
        }
    }

    private List<String> readColumnSignatures(Connection connection, String tableName) throws SQLException {
        List<String> signatures = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ordinal_position, column_name, column_type, is_nullable, column_default,
                       extra, character_set_name, collation_name
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """)) {
            statement.setString(1, EXPECTED_DATABASE);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String extra = resultSet.getString("extra").toLowerCase();
                    signatures.add(columnSignature(
                            resultSet.getInt("ordinal_position"),
                            resultSet.getString("column_name"),
                            normalizeColumnType(resultSet.getString("column_type")),
                            resultSet.getString("is_nullable"),
                            normalizeDefault(resultSet.getString("column_default")),
                            extra.contains("auto_increment"),
                            extra.contains("on update current_timestamp"),
                            resultSet.getString("character_set_name"),
                            resultSet.getString("collation_name")));
                }
            }
        }
        return signatures;
    }

    private List<String> parseColumnSignatures(String tableDdl) {
        Matcher columns = Pattern.compile(
                        "(?m)^\\s*`([^`]+)`\\s+([A-Z]+(?:\\(\\d+(?:,\\d+)?\\))?)\\s+(.+?)(?:,)?$")
                .matcher(tableDdl);
        List<String> signatures = new ArrayList<>();
        while (columns.find()) {
            String type = normalizeColumnType(columns.group(2));
            String definition = columns.group(3);
            signatures.add(columnSignature(
                    signatures.size() + 1,
                    columns.group(1),
                    type,
                    definition.contains("NOT NULL") ? "NO" : "YES",
                    parseDefault(definition),
                    definition.contains("AUTO_INCREMENT"),
                    definition.contains("ON UPDATE CURRENT_TIMESTAMP"),
                    isCharacterType(type) ? "utf8mb4" : null,
                    isCharacterType(type) ? "utf8mb4_bin" : null));
        }
        return signatures;
    }

    private String columnSignature(int position, String name, String type, String nullable,
                                   String defaultValue, boolean autoIncrement, boolean onUpdate,
                                   String characterSet, String collation) {
        return "%03d|%s|%s|%s|%s|%d|%d|%s|%s".formatted(
                position, name, type, nullable, defaultValue,
                autoIncrement ? 1 : 0, onUpdate ? 1 : 0,
                characterSet == null ? "<NULL>" : characterSet,
                collation == null ? "<NULL>" : collation);
    }

    private String parseDefault(String definition) {
        Matcher defaultValue = Pattern.compile(
                        "\\bDEFAULT\\s+('(?:[^']|'')*'|CURRENT_TIMESTAMP(?:\\(\\))?|NULL|-?\\d+(?:\\.\\d+)?)")
                .matcher(definition);
        if (!defaultValue.find() || "NULL".equals(defaultValue.group(1))) {
            return "<NULL>";
        }
        String value = defaultValue.group(1);
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1).replace("''", "'");
        }
        return normalizeDefault(value);
    }

    private String normalizeDefault(String value) {
        if (value == null) {
            return "<NULL>";
        }
        return "CURRENT_TIMESTAMP()".equalsIgnoreCase(value) ? "CURRENT_TIMESTAMP" : value;
    }

    private String normalizeColumnType(String type) {
        String normalized = type.toLowerCase();
        return normalized.matches("(?:bigint|int|tinyint)\\(\\d+\\)")
                ? normalized.substring(0, normalized.indexOf('(')) : normalized;
    }

    private boolean isCharacterType(String type) {
        return type.startsWith("varchar") || type.contains("text");
    }

    private List<String> readIndexSignatures(Connection connection, String tableName) throws SQLException {
        List<String> signatures = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT index_name, non_unique,
                       GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS column_names
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = ?
                GROUP BY index_name, non_unique
                ORDER BY index_name
                """)) {
            statement.setString(1, EXPECTED_DATABASE);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    signatures.add(indexSignature(resultSet.getString("index_name"),
                            resultSet.getInt("non_unique"), resultSet.getString("column_names")));
                }
            }
        }
        return signatures;
    }

    private List<String> parseIndexSignatures(String tableDdl) {
        Matcher indexes = Pattern.compile(
                        "(?m)^\\s*(PRIMARY KEY|UNIQUE KEY|KEY)(?:\\s+`([^`]+)`)?\\s*\\(([^)]+)\\)")
                .matcher(tableDdl);
        List<String> signatures = new ArrayList<>();
        while (indexes.find()) {
            String type = indexes.group(1);
            signatures.add(indexSignature(
                    "PRIMARY KEY".equals(type) ? "PRIMARY" : indexes.group(2),
                    "KEY".equals(type) ? 1 : 0,
                    indexes.group(3).replace("`", "").replaceAll("\\s+", "")));
        }
        return signatures;
    }

    private String indexSignature(String name, int nonUnique, String columns) {
        return name + "|" + nonUnique + "|" + columns;
    }

    private String extractCreateTable(String sql, String tableName) {
        int start = sql.indexOf("CREATE TABLE `" + tableName + "`");
        assertThat(start).as("missing CREATE TABLE for %s", tableName).isGreaterThanOrEqualTo(0);
        int end = sql.indexOf(';', start);
        assertThat(end).as("missing statement terminator for %s", tableName).isGreaterThan(start);
        return sql.substring(start, end + 1);
    }

    private void verifyFundsTransactionBusinessKeyConflictRecoveryUsesCurrentRead() throws Exception {
        try (Connection staleSnapshot = openConnection(); Connection winner = openConnection()) {
            configureRepeatableRead(staleSnapshot);
            configureRepeatableRead(winner);

            assertThat(fundsTransactionExists(staleSnapshot, false)).isFalse();
            insertFundsTransaction(winner, "mysql-rr-funds-winner");
            winner.commit();
            assertDuplicateKey(() -> insertFundsTransaction(staleSnapshot, "mysql-rr-funds-loser"));
            assertThat(fundsTransactionExists(staleSnapshot, false)).isFalse();
            assertThat(fundsTransactionExists(staleSnapshot, true)).isTrue();
            staleSnapshot.rollback();
        }
    }

    private void insertFundsTransaction(Connection connection, String sn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO t_funds_transaction
                    (sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                     status, amount, currency)
                VALUES (?, 1, 'DIRECT', 'PAY', 'MYSQL_RR', 'mysql-rr-business',
                        'SUCCEEDED', 100, 'USD')
                """)) {
            statement.setString(1, sn);
            statement.executeUpdate();
        }
    }

    private boolean fundsTransactionExists(Connection connection, boolean forUpdate) throws SQLException {
        return rowExists(connection, """
                SELECT id FROM t_funds_transaction
                WHERE tenant_id = 1
                  AND business_scene = 'MYSQL_RR'
                  AND business_sn = 'mysql-rr-business'
                """, forUpdate);
    }

    private void verifyPayoutReceiptConflictRecoveryUsesCurrentRead() throws Exception {
        try (Connection staleSnapshot = openConnection(); Connection winner = openConnection()) {
            staleSnapshot.setAutoCommit(false);
            staleSnapshot.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            winner.setAutoCommit(false);
            winner.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            assertThat(receiptExists(staleSnapshot, false)).isFalse();
            insertReceipt(winner, "mysql-rr-winner", "mysql-rr-order-winner");
            winner.commit();

            try {
                insertReceipt(staleSnapshot, "mysql-rr-loser", "mysql-rr-order-loser");
                throw new AssertionError("duplicate external receipt reference must fail");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23000");
            }
            assertThat(receiptExists(staleSnapshot, false)).isFalse();
            assertThat(receiptExists(staleSnapshot, true)).isTrue();
            staleSnapshot.rollback();
        }
    }

    private void insertReceipt(Connection connection, String sn, String payoutOrderSn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO t_payout_receipt
                    (sn, tenant_id, payout_order_sn, channel_ref, external_receipt_ref, external_reference,
                     status, amount, currency, source_receipt_digest, normalized_receipt_digest,
                     evidence_ref, external_occurred_at, received_by)
                VALUES (?, 1, ?, 'mysql-rr-channel', 'mysql-rr-receipt', 'mysql-rr-external',
                        'SUCCEEDED', 100, 'USD', 'source-digest', 'normalized-digest',
                        'evidence-ref', CURRENT_TIMESTAMP, 'mysql-test')
                """)) {
            statement.setString(1, sn);
            statement.setString(2, payoutOrderSn);
            statement.executeUpdate();
        }
    }

    private boolean receiptExists(Connection connection, boolean forUpdate) throws SQLException {
        String sql = """
                SELECT id FROM t_payout_receipt
                WHERE tenant_id = 1
                  AND channel_ref = 'mysql-rr-channel'
                  AND external_receipt_ref = 'mysql-rr-receipt'
                """ + (forUpdate ? " FOR UPDATE" : "");
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }

    private void verifyRecoveryConflictRecoveryUsesCurrentRead() throws Exception {
        try (Connection staleSnapshot = openConnection(); Connection winner = openConnection()) {
            configureRepeatableRead(staleSnapshot);
            configureRepeatableRead(winner);

            assertThat(recoveryOrderExists(staleSnapshot, false)).isFalse();
            insertRecoveryOrder(winner, "mysql-rr-order-winner");
            winner.commit();
            assertDuplicateKey(() -> insertRecoveryOrder(staleSnapshot, "mysql-rr-order-loser"));
            assertThat(recoveryOrderExists(staleSnapshot, false)).isFalse();
            assertThat(recoveryOrderExists(staleSnapshot, true)).isTrue();
            staleSnapshot.rollback();

            assertThat(recoveryResultExists(staleSnapshot, "idempotency_key", "mysql-rr-idempotency", false))
                    .isFalse();
            insertRecoveryResult(winner, "mysql-rr-result-idempotency-winner",
                    "mysql-rr-idempotency", "mysql-rr-transaction-idempotency-winner");
            winner.commit();
            assertDuplicateKey(() -> insertRecoveryResult(staleSnapshot, "mysql-rr-result-idempotency-loser",
                    "mysql-rr-idempotency", "mysql-rr-transaction-idempotency-loser"));
            assertThat(recoveryResultExists(staleSnapshot, "idempotency_key", "mysql-rr-idempotency", false))
                    .isFalse();
            assertThat(recoveryResultExists(staleSnapshot, "idempotency_key", "mysql-rr-idempotency", true))
                    .isTrue();
            staleSnapshot.rollback();

            assertThat(recoveryResultExists(staleSnapshot, "funds_transaction_sn",
                    "mysql-rr-shared-transaction", false)).isFalse();
            insertRecoveryResult(winner, "mysql-rr-result-transaction-winner",
                    "mysql-rr-idempotency-transaction-winner", "mysql-rr-shared-transaction");
            winner.commit();
            assertDuplicateKey(() -> insertRecoveryResult(staleSnapshot, "mysql-rr-result-transaction-loser",
                    "mysql-rr-idempotency-transaction-loser", "mysql-rr-shared-transaction"));
            assertThat(recoveryResultExists(staleSnapshot, "funds_transaction_sn",
                    "mysql-rr-shared-transaction", false)).isFalse();
            assertThat(recoveryResultExists(staleSnapshot, "funds_transaction_sn",
                    "mysql-rr-shared-transaction", true)).isTrue();
            staleSnapshot.rollback();
        }
    }

    private void configureRepeatableRead(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }

    private void insertRecoveryOrder(Connection connection, String sn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO t_recovery_order
                    (sn, tenant_id, source_type, source_sn, responsible_subject_type, responsible_subject_id,
                     expected_amount, recovered_amount, currency, status, source_digest, order_digest,
                     approval_ref, evidence_ref, created_by, version)
                VALUES (?, 1, 'MYSQL_RR', 'mysql-rr-source', 'FUNDING_ACCOUNT', 'mysql-rr-subject',
                        100, 0, 'USD', 'CREATED', 'source-digest', 'order-digest',
                        'approval-ref', 'evidence-ref', 'mysql-test', 0)
                """)) {
            statement.setString(1, sn);
            statement.executeUpdate();
        }
    }

    private boolean recoveryOrderExists(Connection connection, boolean forUpdate) throws SQLException {
        return rowExists(connection, """
                SELECT id FROM t_recovery_order
                WHERE tenant_id = 1
                  AND source_type = 'MYSQL_RR'
                  AND source_sn = 'mysql-rr-source'
                  AND responsible_subject_type = 'FUNDING_ACCOUNT'
                  AND responsible_subject_id = 'mysql-rr-subject'
                  AND currency = 'USD'
                """, forUpdate);
    }

    private void insertRecoveryResult(Connection connection,
                                      String sn,
                                      String idempotencyKey,
                                      String fundsTransactionSn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO t_recovery_result
                    (sn, tenant_id, recovery_order_sn, funds_transaction_sn, amount, currency,
                     idempotency_key, result_digest, approval_ref, evidence_ref, recorded_by)
                VALUES (?, 1, 'mysql-rr-order-winner', ?, 50, 'USD', ?, 'result-digest',
                        'approval-ref', 'evidence-ref', 'mysql-test')
                """)) {
            statement.setString(1, sn);
            statement.setString(2, fundsTransactionSn);
            statement.setString(3, idempotencyKey);
            statement.executeUpdate();
        }
    }

    private boolean recoveryResultExists(Connection connection,
                                         String keyColumn,
                                         String keyValue,
                                         boolean forUpdate) throws SQLException {
        return rowExists(connection, "SELECT id FROM t_recovery_result WHERE tenant_id = 1 AND "
                + keyColumn + " = '" + keyValue + "'", forUpdate);
    }

    private boolean rowExists(Connection connection, String sql, boolean forUpdate) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql + (forUpdate ? " FOR UPDATE" : ""))) {
            return resultSet.next();
        }
    }

    private void assertDuplicateKey(SqlAction action) throws SQLException {
        try {
            action.execute();
            throw new AssertionError("duplicate unique key must fail");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState()).isEqualTo("23000");
        }
    }

    private void cleanupConflictFixtures(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM t_recovery_result WHERE sn LIKE 'mysql-rr-%'");
            statement.executeUpdate("DELETE FROM t_recovery_order WHERE sn LIKE 'mysql-rr-%'");
            statement.executeUpdate("DELETE FROM t_payout_receipt WHERE sn LIKE 'mysql-rr-%'");
            statement.executeUpdate("DELETE FROM t_funds_transaction WHERE sn LIKE 'mysql-rr-%'");
        }
    }

    private Connection openConnection() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", requiredEnvironment("WIND_FUNDS_TEST_DATASOURCE_USERNAME"));
        properties.setProperty("password", System.getenv().getOrDefault("WIND_FUNDS_TEST_DATASOURCE_PASSWORD", ""));
        properties.setProperty("allowMultiQueries", "true");
        return DriverManager.getConnection(requiredEnvironment("WIND_FUNDS_TEST_DATASOURCE_URL"), properties);
    }

    private void dropTables(Connection connection, List<String> tableNames) throws Exception {
        List<String> reverseTableNames = new ArrayList<>(tableNames);
        Collections.reverse(reverseTableNames);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tableName : reverseTableNames) {
                    statement.executeUpdate("DROP TABLE IF EXISTS `" + tableName + "`");
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private int verifyDeployment(Connection connection, String verificationSql,
                                 String expectedVersionPrefix) throws Exception {
        int resultSetCount = 0;
        try (Statement statement = connection.createStatement()) {
            boolean hasResult = statement.execute(verificationSql);
            while (hasResult || statement.getUpdateCount() != -1) {
                if (hasResult) {
                    resultSetCount++;
                    try (ResultSet resultSet = statement.getResultSet()) {
                        if (resultSetCount == 1) {
                            assertThat(resultSet.next()).isTrue();
                            assertThat(resultSet.getString("mysql_version")).startsWith(expectedVersionPrefix);
                            assertThat(resultSet.getString("transaction_isolation")).isEqualTo("REPEATABLE-READ");
                            assertThat(resultSet.next()).isFalse();
                        } else {
                            assertThat(resultSet.next())
                                    .as("deployment verification result set %s must be empty", resultSetCount)
                                    .isFalse();
                        }
                    }
                }
                hasResult = statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
            }
        }
        return resultSetCount;
    }

    private List<String> extractTableNames(String sql) {
        Matcher matcher = Pattern.compile("(?m)^CREATE TABLE `([^`]+)`").matcher(sql);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        return "tests".equals(current.getFileName().toString()) ? current.getParent() : current;
    }

    @FunctionalInterface
    private interface SqlAction {

        void execute() throws SQLException;
    }
}
