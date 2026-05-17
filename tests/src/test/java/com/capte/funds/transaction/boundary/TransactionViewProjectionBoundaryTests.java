package com.capte.funds.transaction.boundary;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionViewProjectionBoundaryTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> DEFERRED_VIEW_PROJECTION_TYPE_NAMES = List.of(
            "TransactionViewProjector",
            "TransactionViewProjection",
            "TransactionViewProjectionContext",
            "TransactionViewReplayService",
            "TransactionViewReplayRequest",
            "TransactionViewReplayTaskRequest",
            "TransactionViewQueryService",
            "TransactionViewQuery",
            "TransactionViewDTO",
            "ProjectionCode",
            "ProjectionPolicy",
            "DisplayDirection",
            "DisplayStatus",
            "ReplayScopeType"
    );

    /**
     * 场景：v4 阶段只冻结交易事实边界，交易展示投影留到 v5 重新设计实现。
     * 输入：扫描 funds 交易与钱包模块的生产源码类型名。
     * 输出：命中的交易展示投影生产类型列表。
     * 预期：v4 不出现 TransactionViewProjector、TransactionViewReplayService 等生产投影写模型。
     */
    @Test
    void testViewProjectionLayerShouldNotMutateFactsOrLedger() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findDeferredViewProjectionTypes(sourceRootPath));
        }

        assertThat(violations)
                .as("transaction view projection is deferred to v5 and should not be implemented in v4")
                .isEmpty();
    }

    private static List<String> findDeferredViewProjectionTypes(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            String fileName = sourceFile.getFileName().toString();
            if (containsDeferredViewProjectionTypeName(fileName)) {
                violations.add(sourceFile.toString());
            }
        }
        return violations;
    }

    private static List<Path> listJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean containsDeferredViewProjectionTypeName(String fileName) {
        return DEFERRED_VIEW_PROJECTION_TYPE_NAMES.stream().anyMatch(fileName::contains);
    }

    private static Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (containsAllProductionSourceRoots(current)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }

    private static boolean containsAllProductionSourceRoots(Path current) {
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            if (!Files.exists(current.resolve(sourceRoot))) {
                return false;
            }
        }
        return true;
    }
}
