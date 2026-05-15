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

    private static final Path WALLET_BALANCE_QUERY_SERVICE = Path.of(
            "wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/FundsSubjectBalanceQueryService.java");

    private static final Path TRANSACTION_BALANCE_QUERY_SERVICE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/FundsSubjectBalanceQueryService.java");

    private static final List<Path> WALLET_LEDGER_PROFILE_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/LedgerProfileService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/SubjectLedgerInitializer.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/LedgerProfileDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/LedgerProfileItemDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/InitializeSubjectLedgerRequest.java")
    );

    private static final List<Path> TRANSACTION_LEDGER_PROFILE_CONTRACTS = List.of(
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/LedgerProfileService.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/SubjectLedgerInitializer.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/LedgerProfileDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/LedgerProfileItemDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/InitializeSubjectLedgerRequest.java")
    );

    private static final Path WALLET_PLATFORM_FUNDING_ACCOUNT_SERVICE = Path.of(
            "wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/PlatformFundingAccountService.java");

    private static final Path CORE_PLATFORM_FUNDING_ACCOUNT_ROLE = Path.of(
            "core/src/main/java/com/wind/integration/funds/wallet/enums/PlatformFundingAccountRole.java");

    private static final Path TRANSACTION_PLATFORM_FUNDING_ACCOUNT_SERVICE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/PlatformFundingAccountService.java");

    private static final Path TRANSACTION_PLATFORM_FUNDING_ACCOUNT_ROLE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/enums/PlatformFundingAccountRole.java");

    private static final List<String> ACCOUNT_CONTRACT_SIMPLE_NAMES = List.of(
            "FundingAccount",
            "CreditAccount",
            "BudgetGroup"
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

    /**
     * 场景：余额查询是 wallet/account 能力，不是 transaction 交易命令能力。
     * 输入：扫描 wallet-face 与 transaction-face 的余额查询服务文件。
     * 输出：对应服务文件是否存在。
     * 预期：wallet-face 暴露余额查询契约，transaction-face 不再承载该账户能力契约。
     * 红线：不得为了让 wallet 不为空而把付款、退款、提现、转账等交易命令迁回 wallet。
     */
    @Test
    void testWalletFaceShouldOwnSubjectBalanceQueryContract() {
        Path projectRoot = projectRoot();

        assertThat(projectRoot.resolve(WALLET_BALANCE_QUERY_SERVICE))
                .as("wallet-face should expose account balance query contract")
                .exists();
        assertThat(projectRoot.resolve(TRANSACTION_BALANCE_QUERY_SERVICE))
                .as("transaction-face should not own account balance query contract")
                .doesNotExist();
    }

    /**
     * 场景：主体开户时按 LedgerProfile 创建 required ledger，这是 wallet/account 产品开户能力。
     * 输入：扫描 wallet-face 与 transaction-face 的 LedgerProfile/SubjectLedgerInitializer 契约文件。
     * 输出：对应契约文件是否存在。
     * 预期：wallet-face 暴露账本 Profile 与主体开户初始化契约，transaction-face 不再承载该账户能力契约。
     * 红线：交易执行路径不得通过该接口隐式创建账本。
     */
    @Test
    void testWalletFaceShouldOwnLedgerProfileAndSubjectLedgerInitializationContracts() {
        Path projectRoot = projectRoot();

        assertThat(WALLET_LEDGER_PROFILE_CONTRACTS)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as("wallet-face should expose " + contract.getFileName())
                        .exists());
        assertThat(TRANSACTION_LEDGER_PROFILE_CONTRACTS)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as("transaction-face should not own " + contract.getFileName())
                        .doesNotExist());
    }

    /**
     * 场景：平台资金账户是平台账户配置与解析能力，交易路由只消费解析结果。
     * 输入：扫描 wallet-face、core 与 transaction-face 的平台资金账户契约。
     * 输出：服务契约和角色枚举所在模块。
     * 预期：wallet-face 暴露平台资金账户解析服务，core 承载共享角色枚举，transaction-face 不再拥有该账户能力契约。
     */
    @Test
    void testWalletFaceShouldOwnPlatformFundingAccountContract() {
        Path projectRoot = projectRoot();

        assertThat(projectRoot.resolve(WALLET_PLATFORM_FUNDING_ACCOUNT_SERVICE))
                .as("wallet-face should expose platform funding account service")
                .exists();
        assertThat(projectRoot.resolve(CORE_PLATFORM_FUNDING_ACCOUNT_ROLE))
                .as("core should expose shared platform funding account role")
                .exists();
        assertThat(projectRoot.resolve(TRANSACTION_PLATFORM_FUNDING_ACCOUNT_SERVICE))
                .as("transaction-face should not own platform funding account service")
                .doesNotExist();
        assertThat(projectRoot.resolve(TRANSACTION_PLATFORM_FUNDING_ACCOUNT_ROLE))
                .as("transaction-face should not own platform funding account role")
                .doesNotExist();
    }

    /**
     * 场景：FundingAccount、CreditAccount、BudgetGroup 是钱包主体账户管理能力。
     * 输入：扫描 wallet-face 与 transaction-face 的账户服务、DTO、Query、Request 契约。
     * 输出：三类账户契约所在模块。
     * 预期：wallet-face 拥有账户主体管理契约，transaction-face 不再承载这些账户能力契约。
     */
    @Test
    void testWalletFaceShouldOwnAccountSubjectManagementContracts() {
        Path projectRoot = projectRoot();

        for (String contractName : ACCOUNT_CONTRACT_SIMPLE_NAMES) {
            assertThat(projectRoot.resolve(walletService(contractName)))
                    .as("wallet-face should expose " + contractName + "Service")
                    .exists();
            assertThat(projectRoot.resolve(walletDto(contractName)))
                    .as("wallet-face should expose " + contractName + "DTO")
                    .exists();
            assertThat(projectRoot.resolve(walletQuery(contractName)))
                    .as("wallet-face should expose " + contractName + "Query")
                    .exists();
            assertThat(projectRoot.resolve(walletRequest(contractName)))
                    .as("wallet-face should expose Create" + contractName + "Request")
                    .exists();

            assertThat(projectRoot.resolve(transactionService(contractName)))
                    .as("transaction-face should not own " + contractName + "Service")
                    .doesNotExist();
            assertThat(projectRoot.resolve(transactionDto(contractName)))
                    .as("transaction-face should not own " + contractName + "DTO")
                    .doesNotExist();
            assertThat(projectRoot.resolve(transactionQuery(contractName)))
                    .as("transaction-face should not own " + contractName + "Query")
                    .doesNotExist();
            assertThat(projectRoot.resolve(transactionRequest(contractName)))
                    .as("transaction-face should not own Create" + contractName + "Request")
                    .doesNotExist();
        }
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

    private static Path walletService(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/" + contractName
                + "Service.java");
    }

    private static Path walletDto(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/" + contractName
                + "DTO.java");
    }

    private static Path walletQuery(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/" + contractName
                + "Query.java");
    }

    private static Path walletRequest(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/Create" + contractName
                + "Request.java");
    }

    private static Path transactionService(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/" + contractName
                + "Service.java");
    }

    private static Path transactionDto(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/" + contractName
                + "DTO.java");
    }

    private static Path transactionQuery(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/query/"
                + contractName + "Query.java");
    }

    private static Path transactionRequest(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/Create"
                + contractName + "Request.java");
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
