package com.capte.funds.wallet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLayerBoundaryTests {

    private static final List<Path> WALLET_SOURCE_ROOTS = List.of(
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "com.capte.funds.ledger.dal.",
            "com.capte.funds.ledger.impl.",
            "com.capte.funds.ledger.DefaultLedgerTransactionPostingServiceImpl",
            "com.capte.funds.transaction.dal.",
            "com.capte.funds.transaction.services.FundsInstructionLifecycleSaver",
            "com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver",
            "com.wind.integration.funds.ledger.LedgerBalanceProjectionService",
            "com.wind.integration.funds.ledger.LedgerTransactionPostingService"
    );

    /**
     * 场景：wallet 层作为产品门面，只编排资金指令，不直接写交易事实或账本事实。
     * 输入：扫描 funds/wallet-face 和 funds/wallet-impl 的生产源码。
     * 输出：命中的禁止依赖引用列表。
     * 预期：wallet 层不依赖交易生命周期写入器、交易事实 Mapper 或账本写入端口。
     */
    @Test
    void testWalletLayerShouldNotWriteFactsOrLedgerDirectly() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : WALLET_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("wallet layer should delegate facts and ledger writes to FundsInstructionOrchestrator")
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
            if (Files.exists(current.resolve("wallet/wallet-face/src/main/java"))
                    && Files.exists(current.resolve("wallet/wallet-impl/src/main/java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("project root not found");
    }
}
