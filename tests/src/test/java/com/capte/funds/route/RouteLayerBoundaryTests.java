package com.capte.funds.route;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RouteLayerBoundaryTests {

    private static final Path ROUTE_SOURCE_ROOT = Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/route");

    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "com.capte.funds.transaction.dal.",
            "com.capte.funds.transaction.services.FundsInstructionLifecycleSaver",
            "com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver",
            "com.capte.funds.ledger.dal.",
            "com.capte.funds.ledger.DefaultLedgerTransactionPostingServiceImpl",
            "com.wind.integration.funds.ledger.LedgerTransactionPostingService",
            "FundsTransactionMapper",
            "FundsTransactionDetailMapper",
            "LedgerTransactionMapper",
            "LedgerEntryMapper"
    );

    /**
     * 场景：route 层只负责解析资金路径，防止直接写交易事实或账本事实。
     * 输入：扫描 funds/transaction-impl 下 route 包的生产源码。
     * 输出：命中的禁止依赖引用列表。
     * 预期：route 包不依赖交易事实 Mapper、生命周期写入器或账本写入端口。
     */
    @Test
    void testRouteLayerShouldNotWriteFactsOrLedger() throws IOException {
        Path sourceRoot = projectRoot().resolve(ROUTE_SOURCE_ROOT);
        assertThat(sourceRoot).exists();

        List<String> violations = findForbiddenReferences(sourceRoot);

        assertThat(violations)
                .as("route layer should only resolve route facts and must not write transaction or ledger facts")
                .isEmpty();
    }

    private static List<String> findForbiddenReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            for (String line : Files.readAllLines(sourceFile)) {
                if (containsForbiddenReference(line)) {
                    violations.add(sourceFile + ": " + line.trim());
                }
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

    private static boolean containsForbiddenReference(String line) {
        return FORBIDDEN_REFERENCES.stream().anyMatch(line::contains);
    }

    private static Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(ROUTE_SOURCE_ROOT))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }
}
