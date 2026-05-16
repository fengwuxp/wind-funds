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

class SettlementReportProjectionBoundaryTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> REPORT_PROJECTION_TYPE_MARKERS = List.of(
            "SettlementReport",
            "SettlementReportProjection",
            "SettlementReportProjector",
            "SettlementReportQuery",
            "SettlementReportDTO",
            "ClearingReport",
            "ClearingReportProjection",
            "PayoutReport",
            "PayoutReportProjection",
            "ReconciliationReport",
            "ReconciliationReportProjection",
            "TimelineReport",
            "TransactionTimelineReportProjection"
    );

    private static final List<String> FORBIDDEN_FACT_WRITE_REFERENCES = List.of(
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
     * 场景：结算、清算、出款、对账和运营报表投影进入实现阶段。
     * 输入：扫描资金域生产源码中的报表投影类型。
     * 输出：报表投影直接引用事实写入组件或 Mapper 的违规列表。
     * 预期：报表只能从交易、账本、清结算明细和快照派生，不直接写 ledger、wallet 或 transaction 事实。
     * 红线：报表投影不得反向修改账本分录、余额投影、钱包账户或交易生命周期事实。
     */
    @Test
    void testSettlementReportProjectionShouldNotWriteFundsFacts() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenReportProjectionReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("settlement report projections must be derived read models and must not write funds facts")
                .isEmpty();
    }

    private static List<String> findForbiddenReportProjectionReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isReportProjectionSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findForbiddenReferences(Path sourceFile) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String line : Files.readAllLines(sourceFile)) {
            if (containsForbiddenFactWriteReference(line)) {
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

    private static boolean isReportProjectionSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return REPORT_PROJECTION_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsForbiddenFactWriteReference(String line) {
        return FORBIDDEN_FACT_WRITE_REFERENCES.stream().anyMatch(line::contains);
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
