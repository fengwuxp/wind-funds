package com.capte.funds.ledger;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceProjectionArchiveContractTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> ARCHIVE_REBUILD_TYPE_MARKERS = List.of(
            "BalanceProjectionArchive",
            "BalanceProjectionRebuild",
            "BalanceProjectionReconstruction",
            "BalanceRebuild",
            "BalanceReconstruction",
            "BalanceCheckpointRebuild",
            "LedgerBalanceArchive",
            "ArchiveBalance"
    );

    private static final List<String> BALANCE_REBUILD_EXECUTION_TYPE_MARKERS = List.of(
            "BalanceProjectionRebuildService",
            "BalanceProjectionRebuildCalculator",
            "BalanceProjectionRebuildAssembler",
            "BalanceProjectionReconstructionService",
            "BalanceRebuildService",
            "BalanceRebuildCalculator",
            "BalanceRebuildAssembler",
            "BalanceReconstructionService",
            "BalanceReconstructionCalculator",
            "BalanceReconstructionAssembler",
            "BalanceCheckpointRebuildService"
    );

    private static final List<String> FORBIDDEN_DERIVED_BALANCE_SOURCE_REFERENCES = List.of(
            "TransactionView",
            "TransactionTimeline",
            "UserBill",
            "UserStatement",
            "MerchantBill",
            "MerchantStatement",
            "SettlementReport",
            "ClearingReport",
            "PayoutReport",
            "ReconciliationReport",
            "ReportProjection",
            "TimelineReport"
    );

    private static final List<String> FORBIDDEN_HOT_COLD_BOUNDARY_REFERENCES = List.of(
            "minusDays(",
            "plusDays(",
            "ChronoUnit.DAYS",
            "Period.ofDays",
            "Duration.ofDays",
            "ofDays(180",
            "minusDays(180",
            "plusDays(180",
            "retentionDays",
            "hotRetentionDays",
            "naturalDay",
            "archiveDate"
    );

    /**
     * 场景：余额检查点、水位、归档清单和余额重建进入实现阶段。
     * 输入：扫描资金域生产源码中的余额归档或余额重建类型。
     * 输出：余额归档/重建依赖账单、交易视图、运营时间线或报表投影的违规列表。
     * 预期：归档后余额只能由 LedgerEntry、BalanceCheckpoint、BalanceProjectionWatermark 和 ArchiveManifest 重建。
     * 红线：不得从用户账单、商户账单、交易视图或报表投影反推账本余额。
     */
    @Test
    void testBalanceProjectionArchiveShouldNotRebuildFromDerivedViewsOrReports() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenDerivedBalanceSourceReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("balance archive and rebuild must use ledger facts, checkpoints, watermarks, and manifests")
                .isEmpty();
    }

    /**
     * 场景：历史分录冷热分层后重建指定时点余额。
     * 输入：扫描资金域生产源码中的余额重建执行类型。
     * 输出：缺少 watermark 或使用自然日、归档日、热保留天数作为冷热拼接边界的违规列表。
     * 预期：冷热拼接边界由 BalanceProjectionWatermark 决定，热保留周期只用于归档资格预检查。
     * 红线：不得用 180 天、自然日或归档日期作为余额计算边界。
     */
    @Test
    void testBalanceRebuildShouldUseWatermarkAsHotColdBoundary() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findWatermarkBoundaryViolations(sourceRootPath));
        }

        assertThat(violations)
                .as("balance rebuild must use BalanceProjectionWatermark as the hot/cold boundary")
                .isEmpty();
    }

    private static List<String> findForbiddenDerivedBalanceSourceReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isArchiveRebuildSource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile, FORBIDDEN_DERIVED_BALANCE_SOURCE_REFERENCES));
            }
        }
        return violations;
    }

    private static List<String> findWatermarkBoundaryViolations(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isBalanceRebuildExecutionSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (!source.contains("BalanceProjectionWatermark") && !source.contains("watermark")) {
                    violations.add(sourceFile + ": missing BalanceProjectionWatermark boundary");
                }
                violations.addAll(findForbiddenReferences(sourceFile, FORBIDDEN_HOT_COLD_BOUNDARY_REFERENCES));
            }
        }
        return violations;
    }

    private static List<String> findForbiddenReferences(Path sourceFile, List<String> forbiddenReferences) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String line : Files.readAllLines(sourceFile)) {
            if (containsForbiddenReference(line, forbiddenReferences)) {
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

    private static boolean isArchiveRebuildSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return ARCHIVE_REBUILD_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean isBalanceRebuildExecutionSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return BALANCE_REBUILD_EXECUTION_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsForbiddenReference(String line, List<String> forbiddenReferences) {
        return forbiddenReferences.stream().anyMatch(line::contains);
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
