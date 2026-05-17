package com.capte.funds.ledger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

abstract class MetricBoundaryTestSupport {

    protected static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    protected static final List<String> METRIC_SOURCE_TYPE_MARKERS = List.of(
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

    protected static List<Path> listJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    protected static boolean isMetricGovernanceSource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return METRIC_SOURCE_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    protected static int firstIndexOf(String source, List<String> markers) {
        int firstIndex = -1;
        for (String marker : markers) {
            int markerIndex = source.indexOf(marker);
            if (markerIndex >= 0 && (firstIndex < 0 || markerIndex < firstIndex)) {
                firstIndex = markerIndex;
            }
        }
        return firstIndex;
    }

    protected static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
    }

    protected static Path projectRoot() {
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
