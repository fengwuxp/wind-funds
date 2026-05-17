package com.capte.funds.ledger;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricFactMutationBoundaryTests extends MetricBoundaryTestSupport {

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
}
