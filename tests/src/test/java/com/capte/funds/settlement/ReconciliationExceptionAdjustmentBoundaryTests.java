package com.capte.funds.settlement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationExceptionAdjustmentBoundaryTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> RECONCILIATION_EXCEPTION_TYPE_MARKERS = List.of(
            "ReconciliationException",
            "ReconciliationExceptionAdjustment",
            "ReconciliationAdjustment",
            "ReconciliationDifference",
            "ReconciliationDiscrepancy",
            "ReconciliationMatching",
            "ReconciliationBatch",
            "ReconciliationResult",
            "ReconciliationTask"
    );

    private static final List<String> FORBIDDEN_DIRECT_FACT_MUTATION_REFERENCES = List.of(
            "com.capte.funds.ledger.DefaultLedgerTransactionPostingServiceImpl",
            "com.capte.funds.ledger.dal.mapper.",
            "com.capte.funds.transaction.dal.mapper.",
            "com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder",
            "com.capte.funds.transaction.services.FundsInstructionLifecycleSaver",
            "com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver",
            "com.capte.funds.wallet.dal.mapper.",
            "com.wind.integration.funds.ledger.LedgerTransactionPostingService",
            "FundsTransactionMapper",
            "FundsTransactionDetailMapper",
            "FundsFrozenOrderMapper",
            "LedgerEntryMapper",
            "LedgerTransactionMapper",
            "LedgerBalanceProjectionMapper",
            "FundingAccountMapper",
            "CreditAccountMapper",
            "BudgetGroupMapper"
    );

    /**
     * 场景：对账差错匹配、阻断、放行、调账或核销能力进入实现阶段。
     * 输入：扫描资金域生产源码中的对账差错和差错调账类型。
     * 输出：对账差错处理直接引用资金事实写入组件或 Mapper 的违规列表。
     * 预期：对账差错只能创建独立差错事实，并通过交易或余额控制契约生成新的调账事实。
     * 红线：对账差异不得直接修改历史 ledger entry、余额投影、钱包账户或交易生命周期事实。
     */
    @Test
    void testReconciliationExceptionAdjustmentShouldNotMutateHistoricalFundsFacts() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenReconciliationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("reconciliation exception adjustment must create new facts and must not mutate history")
                .isEmpty();
    }

    private static List<String> findForbiddenReconciliationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isReconciliationExceptionSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findForbiddenReferences(Path sourceFile) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String line : Files.readAllLines(sourceFile)) {
            if (containsForbiddenDirectFactMutationReference(line)) {
                violations.add(sourceFile + ": " + line.trim());
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

    private static boolean isReconciliationExceptionSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return RECONCILIATION_EXCEPTION_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsForbiddenDirectFactMutationReference(String line) {
        return FORBIDDEN_DIRECT_FACT_MUTATION_REFERENCES.stream().anyMatch(line::contains);
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
