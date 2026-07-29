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
import java.sql.ResultSet;
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
        assertThat(tableNames).hasSize(15);

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = openConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).isEqualTo(EXPECTED_DATABASE);
            dropTables(connection, tableNames);
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(forwardDdl));
            assertThat(verifyDeployment(connection, Files.readString(verificationDdl), expectedVersionPrefix))
                    .isEqualTo(EXPECTED_VERIFICATION_RESULT_SET_COUNT);
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
}
