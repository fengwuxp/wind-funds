package com.capte.funds.settlement;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantRecoveryServiceTests {

    private static final List<Path> PRODUCTION_SOURCE_ROOTS = List.of(
            Path.of("core/src/main/java"),
            Path.of("ledger/ledger-face/src/main/java"),
            Path.of("ledger/ledger-impl/src/main/java"),
            Path.of("transaction/transaction-face/src/main/java"),
            Path.of("transaction/transaction-impl/src/main/java"),
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    private static final List<String> MERCHANT_RECOVERY_TYPE_MARKERS = List.of(
            "MerchantRecovery",
            "MerchantLiability",
            "MerchantDebt",
            "MerchantNegativeBalance",
            "RecoveryCase",
            "RecoveryOrder",
            "RecoveryTask",
            "ReserveDeduction",
            "SettlementDeduction",
            "PostPayoutRefund",
            "ChargebackRecovery"
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

    private static final List<String> RECOVERY_LIABILITY_FACT_MARKERS = List.of(
            "MerchantRecovery",
            "MerchantLiability",
            "MerchantDebt",
            "MerchantNegativeBalance",
            "RecoveryCase",
            "RecoveryOrder",
            "RecoveryTask"
    );

    private static final List<String> RECOVERY_RESOLUTION_MARKERS = List.of(
            "RISK_RESERVE",
            "SETTLEMENT",
            "ADJUSTMENT",
            "NEGATIVE_AVAILABLE",
            "negativeAvailable",
            "ReserveDeduction",
            "SettlementDeduction",
            "BalanceAdjust",
            "BALANCE_ADJUST",
            "ALLOW_NEGATIVE"
    );

    private static final List<String> NEGATIVE_AVAILABLE_REQUIRED_CONTEXT_KEYS = List.of(
            FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE,
            FundsInstructionContextKeys.APPROVAL_REF,
            FundsInstructionContextKeys.ADJUST_REASON,
            FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
            FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT,
            FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT,
            FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT
    );

    /**
     * 场景：商户已出款后发生退款、拒付或差错，商户余额不足。
     * 输入：扫描资金域生产源码中的商户追偿、责任、负余额、准备金扣减或后续抵扣类型。
     * 输出：追偿处理直接引用资金事实写入组件或 Mapper 的违规列表。
     * 预期：追偿只能创建新的责任、负余额、准备金扣减、后续结算抵扣或调账入口。
     * 红线：不得直接改写历史 LedgerEntry、BalanceProjection、FundsTransaction 或 Wallet 账户事实。
     */
    @Test
    void testMerchantRecoveryShouldNotMutateHistoricalFundsFacts() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findForbiddenFactMutationReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("merchant recovery must create new facts and must not mutate historical funds facts")
                .isEmpty();
    }

    /**
     * 场景：商户负余额追偿进入实现阶段。
     * 输入：扫描资金域生产源码中的商户追偿类型。
     * 输出：只记录责任对象但没有任何追偿、准备金、负余额或后续抵扣路径的违规列表。
     * 预期：商户追偿必须至少有一个可解释的资金处理入口。
     * 红线：不得只建追偿单据而没有资金责任落点，导致后续报表或对账无法解释。
     */
    @Test
    void testMerchantRecoveryShouldDeclareRecoverableFundsPath() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findMissingFundsPathReferences(sourceRootPath));
        }

        assertThat(violations)
                .as("merchant recovery must declare negative balance, reserve, adjustment or settlement deduction path")
                .isEmpty();
    }

    /**
     * 场景：商户追偿选择形成受控负余额。
     * 输入：生产源码中带 `allowNegativeBalance` 的商户追偿处理。
     * 输出：缺少策略、审批/风控、原因、风险状态、单笔上限、累计上限或账龄起点的违规列表。
     * 预期：受控负余额必须带完整治理上下文，进入风控、追偿、抵扣或人工处理。
     * 红线：不得把受控负余额退化成一个布尔开关。
     */
    @Test
    void testMerchantRecoveryNegativeAvailableShouldCarryGovernanceContext() throws IOException {
        List<String> violations = new ArrayList<>();
        Path projectRoot = projectRoot();
        for (Path sourceRoot : PRODUCTION_SOURCE_ROOTS) {
            Path sourceRootPath = projectRoot.resolve(sourceRoot);
            assertThat(sourceRootPath).exists();
            violations.addAll(findIncompleteNegativeAvailableContext(sourceRootPath));
        }

        assertThat(violations)
                .as("merchant recovery negative available path must carry governance context")
                .isEmpty();
    }

    private static List<String> findForbiddenFactMutationReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMerchantRecoverySource(sourceFile)) {
                violations.addAll(findForbiddenReferences(sourceFile));
            }
        }
        return violations;
    }

    private static List<String> findMissingFundsPathReferences(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMerchantRecoverySource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (containsAny(source, RECOVERY_LIABILITY_FACT_MARKERS)
                        && !containsAny(source, RECOVERY_RESOLUTION_MARKERS)) {
                    violations.add(sourceFile + ": merchant recovery must declare a recoverable funds path");
                }
            }
        }
        return violations;
    }

    private static List<String> findIncompleteNegativeAvailableContext(Path sourceRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path sourceFile : listJavaSources(sourceRoot)) {
            if (isMerchantRecoverySource(sourceFile)) {
                String source = Files.readString(sourceFile);
                if (source.contains(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE)) {
                    for (String requiredKey : NEGATIVE_AVAILABLE_REQUIRED_CONTEXT_KEYS) {
                        if (!source.contains(requiredKey)) {
                            violations.add(sourceFile + ": missing negative available context " + requiredKey);
                        }
                    }
                }
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

    private static boolean isMerchantRecoverySource(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        return MERCHANT_RECOVERY_TYPE_MARKERS.stream().anyMatch(fileName::contains);
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
