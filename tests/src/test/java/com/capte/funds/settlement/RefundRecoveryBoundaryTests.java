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

class RefundRecoveryBoundaryTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> REFUND_RECOVERY_TYPE_MARKERS = List.of(
            "RefundRecovery",
            "RefundLiability",
            "RefundResponsibility",
            "MerchantRecovery",
            "PostPayoutRefund",
            "PayoutRefund",
            "SettlementLockedRefund",
            "SettlementRefundRecovery",
            "RecoveryCase",
            "RecoveryOrder",
            "RecoveryTask"
    );

    private static final List<String> AUTOMATIC_REFUND_ROUTE_MARKERS = List.of(
            "FundsTransactionRefundRequest",
            "RouteReplayType.REFUND",
            "FundsRouteCodes.DIRECT_REFUND_STANDARD",
            "FundsRouteCodes.DIRECT_REFUND_REPLAY",
            "DefaultFundsTransactionType.REFUND",
            "FundsTransactionEventType.REFUND",
            "\"DIRECT_REFUND_STANDARD\"",
            "\"DIRECT_REFUND_REPLAY\""
    );

    private static final List<String> SETTLEMENT_TO_AVAILABLE_MARKERS = List.of(
            "LedgerSubjectCode.SETTLEMENT",
            "LedgerSubjectCode.AVAILABLE"
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
     * 场景：商户资金已进入 SETTLEMENT、出款中或已出款后发生退款。
     * 输入：扫描资金域生产源码中的退款追偿、出款后退款或责任处理类型。
     * 输出：直接复用普通自动退款 route，把商户 SETTLEMENT 反向退到用户 AVAILABLE 的违规列表。
     * 预期：该类场景必须拒绝自动退款，或进入人工、追偿、准备金、负余额、后续结算抵扣路径。
     * 红线：不得直接做 SETTLEMENT -> 用户 AVAILABLE 的普通退款。
     */
    @Test
    void testPostPayoutRefundShouldNotUseAutomaticSettlementToAvailableRefundRoute() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findAutomaticSettlementRefundReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("post-payout refund must not use automatic SETTLEMENT -> AVAILABLE refund route")
                .isEmpty();
    }

    /**
     * 场景：出款中或已出款后退款进入追偿、人工处理或后续结算抵扣。
     * 输入：扫描资金域生产源码中的退款追偿、出款后退款或责任处理类型。
     * 输出：处理类直接引用资金事实写入组件或 Mapper 的违规列表。
     * 预期：追偿处理只能创建新的责任、追偿、准备金、负余额或调账事实入口。
     * 红线：不得直接改写历史 LedgerEntry、BalanceProjection、FundsTransaction 或 Wallet 账户事实。
     */
    @Test
    void testRefundRecoveryShouldNotMutateHistoricalFundsFacts() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenFactMutationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("refund recovery must create new facts and must not mutate historical funds facts")
                .isEmpty();
    }

    private static List<String> findAutomaticSettlementRefundReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isRefundRecoverySource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAll(source, SETTLEMENT_TO_AVAILABLE_MARKERS)
                        && containsAny(source, AUTOMATIC_REFUND_ROUTE_MARKERS)) {
                    violations.add(sourceFile + ": automatic refund route must not move SETTLEMENT to AVAILABLE");
                }
            }
        }
        return violations;
    }

    private static List<String> findForbiddenFactMutationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isRefundRecoverySource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findForbiddenReferences(Path sourceFile) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String line : Files.readAllLines(sourceFile)) {
            if (containsAny(line, FORBIDDEN_DIRECT_FACT_MUTATION_REFERENCES)) {
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

    private static boolean isRefundRecoverySource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return REFUND_RECOVERY_TYPE_MARKERS.stream().anyMatch(fileName::contains);
    }

    private static boolean containsAll(String source, List<String> markers) {
        return markers.stream().allMatch(source::contains);
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
