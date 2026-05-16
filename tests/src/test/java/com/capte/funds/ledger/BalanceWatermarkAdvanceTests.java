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

class BalanceWatermarkAdvanceTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> WATERMARK_ADVANCE_TYPE_MARKERS = List.of(
            "BalanceProjectionWatermarkAdvance",
            "BalanceProjectionWatermarkProcessor",
            "BalanceProjectionWatermarkService",
            "BalanceProjectionWatermarkTask",
            "BalanceWatermarkAdvance",
            "BalanceWatermarkProcessor",
            "BalanceWatermarkService",
            "BalanceWatermarkTask",
            "ProjectionWatermarkAdvance",
            "ProjectionWatermarkProcessor"
    );

    private static final List<String> WATERMARK_ADVANCE_MARKERS = List.of(
            "advanceWatermark",
            "advanceBalanceWatermark",
            "advanceProjectionWatermark",
            "updateWatermark",
            "updateBalanceWatermark",
            "updateProjectionWatermark",
            "setWatermarkTime",
            "saveWatermark",
            "saveBalanceWatermark",
            "markWatermarkAdvanced"
    );

    private static final List<String> COMPUTE_PHASE_MARKERS = List.of(
            "calculate",
            "compute",
            "aggregate",
            "summarize",
            "buildCheckpoint",
            "createCheckpoint",
            "BalanceCheckpoint",
            "LedgerEntry"
    );

    private static final List<String> WRITE_PHASE_MARKERS = List.of(
            "write",
            "save",
            "insert",
            "upsert",
            "persist",
            "ArchiveManifest",
            "BalanceCheckpoint"
    );

    private static final List<String> VERIFY_PHASE_MARKERS = List.of(
            "verify",
            "validate",
            "checksum",
            "digest",
            "reconcile",
            "assertBalanced"
    );

    /**
     * 场景：余额投影水位推进批处理进入实现阶段。
     * 输入：扫描资金域生产源码中的余额水位推进执行类型。
     * 输出：先推进水位或缺少计算、写入、校验前置阶段的违规列表。
     * 预期：批处理必须先计算 [start, end) 分录，写入 checkpoint/manifest/digest 并校验通过，再推进 watermark=end。
     * 红线：不得先推进 BalanceProjectionWatermark 再计算区间分录；失败时水位必须停留在 start。
     */
    @Test
    void testBalanceWatermarkShouldAdvanceOnlyAfterComputeWriteAndVerify() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findWatermarkAdvanceOrderingViolations(sourceRootPath));
        }

        assertThat(violations)
                .as("balance watermark must advance only after compute, write, and verify phases")
                .isEmpty();
    }

    private static List<String> findWatermarkAdvanceOrderingViolations(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isWatermarkAdvanceSource(sourceFile)) {
                violations.addAll(findSourceFileOrderingViolations(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findSourceFileOrderingViolations(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);
        int watermarkAdvanceIndex = firstIndexOf(source, WATERMARK_ADVANCE_MARKERS);
        if (watermarkAdvanceIndex < 0) {
            return List.of(sourceFile + ": missing explicit watermark advance phase");
        }
        String beforeWatermarkAdvance = source.substring(0, watermarkAdvanceIndex);
        List<String> violations = new ArrayList<>();
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, COMPUTE_PHASE_MARKERS,
                "missing compute phase before watermark advance", violations);
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, WRITE_PHASE_MARKERS,
                "missing checkpoint or manifest write phase before watermark advance", violations);
        addMissingPhaseViolation(sourceFile, beforeWatermarkAdvance, VERIFY_PHASE_MARKERS,
                "missing verify phase before watermark advance", violations);
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

    private static boolean isWatermarkAdvanceSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return WATERMARK_ADVANCE_TYPE_MARKERS.stream().anyMatch(fileName::contains);
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
