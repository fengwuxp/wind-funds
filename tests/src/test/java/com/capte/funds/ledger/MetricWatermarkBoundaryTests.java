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

class MetricWatermarkBoundaryTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> METRIC_SOURCE_TYPE_MARKERS = List.of(
            "MetricWatermark",
            "MetricSnapshot",
            "MetricAggregation",
            "MetricRecalculation",
            "MetricRecompute",
            "MetricRebuild",
            "MetricProjection",
            "MetricGovernance",
            "FundsMetric",
            "SettlementMetric",
            "RiskMetric"
    );

    private static final List<String> METRIC_ADVANCE_TYPE_MARKERS = List.of(
            "MetricWatermarkAdvance",
            "MetricWatermarkProcessor",
            "MetricWatermarkService",
            "MetricWatermarkTask",
            "MetricAggregationProcessor",
            "MetricAggregationService",
            "MetricAggregationTask",
            "MetricRecalculationProcessor",
            "MetricRecalculationService",
            "MetricRecomputeProcessor",
            "MetricRecomputeService"
    );

    private static final List<String> METRIC_WATERMARK_ADVANCE_MARKERS = List.of(
            "advanceMetricWatermark",
            "advanceWatermark",
            "updateMetricWatermark",
            "updateWatermark",
            "saveMetricWatermark",
            "markMetricWatermarkAdvanced"
    );

    private static final List<String> COMPUTE_PHASE_MARKERS = List.of(
            "calculate",
            "compute",
            "aggregate",
            "summarize",
            "buildSnapshot",
            "MetricSnapshot"
    );

    private static final List<String> WRITE_PHASE_MARKERS = List.of(
            "write",
            "save",
            "insert",
            "upsert",
            "persist",
            "MetricSnapshot",
            "digest"
    );

    private static final List<String> VERIFY_PHASE_MARKERS = List.of(
            "verify",
            "validate",
            "checksum",
            "digest",
            "reconcile",
            "compare"
    );

    private static final List<String> ARCHIVE_BOUNDARY_MARKERS = List.of(
            "BalanceProjectionWatermark",
            "ArchiveManifest",
            "archiveCutoff",
            "archiveCutoffTime",
            "hotRetentionDays",
            "retentionDays"
    );

    private static final List<String> FACT_MUTATION_MARKERS = List.of(
            "LedgerEntryMapper",
            "LedgerTransactionMapper",
            "LedgerBalanceProjectionMapper",
            "FundsTransactionMapper",
            "FundsTransactionDetailMapper",
            "FundsInstructionLifecycleRecorder",
            "LedgerTransactionPostingService"
    );

    /**
     * 场景：指标治理进入实现阶段，指标任务需要独立维护指标水位。
     * 输入：扫描资金域生产源码中的指标水位、指标快照或指标重算类型。
     * 输出：复用余额归档水位、归档清单或热保留周期作为指标边界的违规列表。
     * 预期：指标任务只能使用 MetricWatermark + MetricSnapshot 或等价指标口径版本控制自身边界。
     * 红线：指标水位不得复用 BalanceProjectionWatermark、ArchiveManifest 或 180 天热保留边界。
     */
    @Test
    void testMetricWatermarkShouldNotReuseBalanceArchiveBoundary() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findArchiveBoundaryReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("metric watermark must be independent from balance archive boundary")
                .isEmpty();
    }

    /**
     * 场景：指标批处理按窗口聚合钱包、交易、清结算、风控等指标。
     * 输入：扫描资金域生产源码中的指标水位推进执行类型。
     * 输出：先推进指标水位或缺少计算、快照写入、校验前置阶段的违规列表。
     * 预期：指标批处理必须先计算窗口、写入 MetricSnapshot/digest 并校验通过，再推进 MetricWatermark。
     * 红线：指标批处理失败时不得推进水位，不得跳过未处理窗口。
     */
    @Test
    void testMetricWatermarkShouldAdvanceOnlyAfterSnapshotWriteAndVerify() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findMetricWatermarkAdvanceOrderingViolations(sourceRootPath));
        }

        assertThat(violations)
                .as("metric watermark must advance only after compute, snapshot write, and verify phases")
                .isEmpty();
    }

    /**
     * 场景：指标计算发现指标与明细不一致。
     * 输入：扫描资金域生产源码中的指标治理类型。
     * 输出：指标异常直接调用交易、账本或余额写侧能力的违规列表。
     * 预期：指标异常只能生成差异、告警、人工复核、对账任务或调账流程入口。
     * 红线：指标任务不得直接修改 LedgerEntry、BalanceProjection 或 FundsTransaction 事实。
     */
    @Test
    void testMetricExceptionShouldNotMutateFundsFacts() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findFactMutationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("metric exception must not mutate ledger, balance, or transaction facts")
                .isEmpty();
    }

    private static List<String> findArchiveBoundaryReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMetricGovernanceSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                for (String marker : ARCHIVE_BOUNDARY_MARKERS) {
                    if (source.contains(marker)) {
                        violations.add(sourceFile + ": metric boundary must not reference " + marker);
                    }
                }
            }
        }
        return violations;
    }

    private static List<String> findMetricWatermarkAdvanceOrderingViolations(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMetricWatermarkAdvanceSource(sourceFile)) {
                violations.addAll(findSourceFileOrderingViolations(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findSourceFileOrderingViolations(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);
        int watermarkAdvanceIndex = firstIndexOf(source, METRIC_WATERMARK_ADVANCE_MARKERS);
        if (watermarkAdvanceIndex < 0) {
            return List.of(sourceFile + ": missing explicit metric watermark advance phase");
        }
        String beforeWatermarkAdvance = source.substring(0, watermarkAdvanceIndex);
        List<String> violations = new ArrayList<>();
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, COMPUTE_PHASE_MARKERS,
                "missing metric compute phase before watermark advance", violations);
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, WRITE_PHASE_MARKERS,
                "missing metric snapshot or digest write phase before watermark advance", violations);
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, VERIFY_PHASE_MARKERS,
                "missing metric verify phase before watermark advance", violations);
        return violations;
    }

    private static List<String> findFactMutationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMetricGovernanceSource(sourceFile)) {
                String source = Files.readString(sourceFile);
                for (String marker : FACT_MUTATION_MARKERS) {
                    if (source.contains(marker)) {
                        violations.add(sourceFile + ": metric governance must not mutate funds facts through " + marker);
                    }
                }
            }
        }
        return violations;
    }

    private static void addMissingPhaseViolation(Path sourceFile, String source, List<String> phaseMarkers,
                                                String message, List<String> violations) {
        if (!containsAny(source, phaseMarkers)) {
            violations.add(sourceFile + ": " + message);
        }
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

    private static boolean isMetricGovernanceSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return METRIC_SOURCE_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean isMetricWatermarkAdvanceSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return METRIC_ADVANCE_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static int firstIndexOf(String source, List<String> markers) {
        int firstIndex = -1;
        for (String marker : markers) {
            int markerIndex = source.indexOf(marker);
            if (markerIndex >= 0 && (firstIndex < 0 || markerIndex < firstIndex)) {
                firstIndex = markerIndex;
            }
        }
        return firstIndex;
    }

    private static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
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
