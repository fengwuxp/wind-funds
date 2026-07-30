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
 * 在显式授权的专用 MySQL 库上执行对账生产 DDL 与部署回读。
 */
@EnabledIfEnvironmentVariable(named = "WIND_FUNDS_TEST_MYSQL_DESTRUCTIVE", matches = "true")
class ReconciliationMysqlMigrationIntegrationTests {

    private static final String EXPECTED_DATABASE = "wind_funds_reconciliation_test";

    private static final int EXPECTED_VERIFICATION_RESULT_SET_COUNT = 6;

    @Test
    void testForwardMigrationAndVerificationShouldPassOnTargetMysql() throws Exception {
        Path databaseDirectory = workspaceRoot().resolve("database/mysql/reconciliation");
        Path forwardDdl = databaseDirectory.resolve("001_create_reconciliation_tables.sql");
        Path verificationDdl = databaseDirectory.resolve("001_verify_reconciliation_tables.sql");
        String expectedVersionPrefix = requiredEnvironment("WIND_FUNDS_TEST_MYSQL_EXPECTED_VERSION_PREFIX");
        List<String> tableNames = extractTableNames(Files.readString(forwardDdl));
        assertThat(tableNames).hasSize(21);

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = openConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).isEqualTo(EXPECTED_DATABASE);
            dropTables(connection, tableNames);
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(forwardDdl));
            assertThat(verifyDeployment(connection, Files.readString(verificationDdl), expectedVersionPrefix))
                    .isEqualTo(EXPECTED_VERIFICATION_RESULT_SET_COUNT);
            verifyPayoutReceiptConflictRecoveryUsesCurrentRead();
            verifyRecoveryConflictRecoveryUsesCurrentRead();
        }
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
