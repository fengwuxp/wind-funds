package com.wind.funds.route;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由解析事实写入边界测试。
 */
class RouteResolverFactBoundaryTests {

    private static final List<String> ROUTE_SOURCE_DIRS = List.of(
            "core/src/main/java/com/wind/funds/route",
            "transaction/transaction-impl/src/main/java/com/wind/funds/route");

    private static final List<String> FACT_WRITE_IMPORT_PREFIXES = List.of(
            "com.wind.funds.ledger.dal.",
            "com.wind.funds.ledger.impl.",
            "com.wind.funds.ledger.service.",
            "com.wind.funds.transaction.application.",
            "com.wind.funds.transaction.dal.",
            "com.wind.funds.transaction.ledger.",
            "com.wind.funds.transaction.services.impl.",
            "com.wind.funds.ledger.LedgerBalanceProjectionService",
            "com.wind.funds.ledger.LedgerTransactionPostingService");

    private static final List<String> FACT_WRITE_TYPE_TOKENS = List.of(
            "DefaultLedgerTransactionPostingServiceImpl",
            "LedgerBalanceProjectionServiceImpl",
            "LedgerTransactionServiceImpl",
            "LedgerServiceImpl",
            "FundsTransactionCommandServiceImpl",
            "FundsDirectTransactionService",
            "FundsAuthorizationTransactionService",
            "LedgerTransactionPostingService",
            "LedgerBalanceProjectionService");

    /**
     * 场景：路由解析器参与资金交易主链路选路和 route snapshot 回放。
     * 输入：core route 契约和 transaction-impl route 包生产源码。
     * 输出：不依赖交易命令、账本写入、账本投影或 DAL 事实写入入口。
     * 预期：RouteResolver 只解析路径、参与方、账目和 route leg，不写交易事实、账本事实或余额投影。
     * 红线：路由层不得通过引入写事实服务形成隐式副作用。
     */
    @Test
    void testRouteResolverLayerShouldNotDependOnFactWritePorts() throws IOException {
        List<String> violations = new ArrayList<>();
        for (SourceFile sourceFile : routeSourceFiles()) {
            List<String> importStatements = sourceFile.content().lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .toList();
            for (String importStatement : importStatements) {
                collectImportViolations(violations, sourceFile.path(), importStatement);
            }
            for (String forbiddenToken : FACT_WRITE_TYPE_TOKENS) {
                if (sourceFile.content().contains(forbiddenToken)) {
                    violations.add(sourceViolation(sourceFile.path(), forbiddenToken));
                }
            }
        }

        assertThat(violations)
                .as("RouteResolver layer must not depend on transaction, ledger, or projection fact write ports")
                .isEmpty();
    }

    private void collectImportViolations(List<String> violations, Path sourcePath, String importStatement) {
        for (String forbiddenPrefix : FACT_WRITE_IMPORT_PREFIXES) {
            if (importsPrefix(importStatement, forbiddenPrefix)) {
                violations.add(sourceViolation(sourcePath, importStatement));
            }
        }
    }

    private boolean importsPrefix(String importStatement, String forbiddenPrefix) {
        return importStatement.startsWith("import " + forbiddenPrefix)
                || importStatement.startsWith("import static " + forbiddenPrefix);
    }

    private List<SourceFile> routeSourceFiles() throws IOException {
        List<SourceFile> sourceFiles = new ArrayList<>();
        for (String sourceDir : ROUTE_SOURCE_DIRS) {
            for (Path sourcePath : javaSourcePaths(workspaceRoot().resolve(sourceDir))) {
                sourceFiles.add(new SourceFile(sourcePath, Files.readString(sourcePath)));
            }
        }
        return sourceFiles;
    }

    private Collection<Path> javaSourcePaths(Path sourceRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private String sourceViolation(Path sourcePath, String violation) {
        return workspaceRoot().relativize(sourcePath) + " contains " + violation;
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }

    private record SourceFile(Path path, String content) {
    }
}
