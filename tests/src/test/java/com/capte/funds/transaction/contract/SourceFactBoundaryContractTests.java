package com.capte.funds.transaction.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFactBoundaryContractTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final Path TRANSACTION_LAYER_DSL_ROOT =
            Path.of("core/src/test/resources/dsl/transaction-layer");

    private static final List<String> DEFERRED_SOURCE_FACT_FIELDS = List.of(
            "sourceObjectType",
            "sourceObjectSn",
            "sourceFactRef"
    );

    /**
     * 场景：P0 阶段来源事实引用尚未独立建模，执行链路只使用 businessScene/businessSn/reference。
     * 输入：扫描 funds 核心、交易、钱包和账本生产源码。
     * 输出：遗留 sourceObjectType/sourceObjectSn 或提前泛化 sourceFactRef 的命中位置。
     * 预期：生产契约不暴露旧散字段，也不提前引入 sourceFactRef。
     * 红线：冻结、清结算、争议、对账差错等独立事实成熟前，不把业务流水伪装成来源事实。
     */
    @Test
    void testExecutionContractsShouldNotExposeDeferredSourceFactFields() throws IOException {
        Path projectRoot = projectRoot();
        List<String> violations = new ArrayList<>();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findDeferredSourceFactFields(sourceRootPath));
        }

        assertThat(violations)
                .as("source fact reference is deferred; current execution baseline is businessScene/businessSn/reference")
                .isEmpty();
    }

    /**
     * 场景：DSL 样例作为当前服务能力契约，不应先于独立事实模型暴露 sourceFactRef。
     * 输入：扫描 transaction-layer DSL JSON 样例。
     * 输出：遗留 sourceObjectType/sourceObjectSn 或提前泛化 sourceFactRef 的命中位置。
     * 预期：样例仍以业务身份和 reference 表达动作上下文。
     */
    @Test
    void testTransactionLayerDslFixturesShouldKeepCurrentSourceFactBoundary() throws IOException {
        Path dslRoot = projectRoot().resolve(TRANSACTION_LAYER_DSL_ROOT);
        assertThat(dslRoot).exists();

        assertThat(findDeferredSourceFactFields(dslRoot))
                .as("transaction-layer DSL fixtures should not publish deferred source fact fields")
                .isEmpty();
    }

    private static List<String> findDeferredSourceFactFields(Path root) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : listScannableFiles(root)) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                for (String field : DEFERRED_SOURCE_FACT_FIELDS) {
                    if (line.contains(field)) {
                        violations.add("%s:%d contains %s".formatted(file, index + 1, field));
                    }
                }
            }
        }
        return violations;
    }

    private static List<Path> listScannableFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(SourceFactBoundaryContractTests::isScannableFile)
                    .sorted()
                    .toList();
        }
    }

    private static boolean isScannableFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java") || fileName.endsWith(".json");
    }

    private static Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (containsAllScannedRoots(current)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }

    private static boolean containsAllScannedRoots(Path current) {
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            if (!Files.exists(current.resolve(sourceRoot))) {
                return false;
            }
        }
        return Files.exists(current.resolve(TRANSACTION_LAYER_DSL_ROOT));
    }
}
