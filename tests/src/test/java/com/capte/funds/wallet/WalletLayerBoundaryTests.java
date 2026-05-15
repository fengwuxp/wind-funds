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

    private static final List<Path> WALLET_PAYMENT_INSTRUMENT_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/PaymentInstrumentService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/PaymentInstrumentDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/PaymentInstrumentBindingDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/PaymentInstrumentQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/PaymentInstrumentBindingQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/CreatePaymentInstrumentRequest.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "CreatePaymentInstrumentBindingRequest.java")
    );

    private static final List<Path> CORE_PAYMENT_INSTRUMENT_ENUMS = List.of(
            Path.of("core/src/main/java/com/wind/integration/funds/wallet/enums/PaymentInstrumentDirection.java"),
            Path.of("core/src/main/java/com/wind/integration/funds/wallet/enums/PaymentInstrumentBindingRole.java")
    );

    private static final List<Path> TRANSACTION_PAYMENT_INSTRUMENT_CONTRACTS = List.of(
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "PaymentInstrumentService.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/"
                    + "PaymentInstrumentDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/"
                    + "PaymentInstrumentBindingDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/query/"
                    + "PaymentInstrumentQuery.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/query/"
                    + "PaymentInstrumentBindingQuery.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/"
                    + "CreatePaymentInstrumentRequest.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/"
                    + "CreatePaymentInstrumentBindingRequest.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/enums/"
                    + "PaymentInstrumentDirection.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/enums/"
                    + "PaymentInstrumentBindingRole.java")
    );

    private static final List<Path> WALLET_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/"
                    + "SpendSubjectFundingRelationService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/"
                    + "SpendSubjectFundingRelationDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/"
                    + "SpendSubjectFundingRelationQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "CreateSpendSubjectFundingRelationRequest.java")
    );

    private static final Path CORE_SPEND_SUBJECT_FUNDING_RELATION_TYPE = Path.of(
            "core/src/main/java/com/wind/integration/funds/wallet/enums/SpendSubjectFundingRelationType.java");

    private static final List<Path> TRANSACTION_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS = List.of(
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "SpendSubjectFundingRelationService.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/"
                    + "SpendSubjectFundingRelationDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/query/"
                    + "SpendSubjectFundingRelationQuery.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/"
                    + "CreateSpendSubjectFundingRelationRequest.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/enums/"
                    + "SpendSubjectFundingRelationType.java")
    );

    private static final List<Path> WALLET_ACCOUNT_CAPABILITY_IMPLEMENTATIONS = List.of(
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "FundingAccountServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "CreditAccountServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "BudgetGroupServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "DefaultFundsAccountQueryServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "DefaultLedgerProfileServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "DefaultSubjectLedgerInitializer.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "PlatformFundingAccountServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "PaymentInstrumentServiceImpl.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/services/impl/"
                    + "SpendSubjectFundingRelationServiceImpl.java")
    );

    private static final List<Path> WALLET_ACCOUNT_CAPABILITY_DAL = List.of(
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/FundingAccount.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/CreditAccount.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/BudgetGroup.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/PaymentInstrument.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/"
                    + "PaymentInstrumentBinding.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/entities/"
                    + "SpendSubjectFundingRel.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/FundingAccountMapper.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/CreditAccountMapper.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/BudgetGroupMapper.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/PaymentInstrumentMapper.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/"
                    + "PaymentInstrumentBindingMapper.java"),
            Path.of("wallet/wallet-impl/src/main/java/com/capte/funds/wallet/dal/mapper/"
                    + "SpendSubjectFundingRelMapper.java")
    );

    private static final List<Path> TRANSACTION_ACCOUNT_CAPABILITY_IMPLEMENTATIONS = List.of(
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "FundingAccountServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "CreditAccountServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "BudgetGroupServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "DefaultFundsAccountQueryServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "DefaultLedgerProfileServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "DefaultSubjectLedgerInitializer.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "PlatformFundingAccountServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "PaymentInstrumentServiceImpl.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/services/impl/"
                    + "SpendSubjectFundingRelationServiceImpl.java")
    );

    private static final List<Path> TRANSACTION_ACCOUNT_CAPABILITY_DAL = List.of(
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "FundingAccount.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "CreditAccount.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "BudgetGroup.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "PaymentInstrument.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "PaymentInstrumentBinding.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/entities/"
                    + "SpendSubjectFundingRel.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "FundingAccountMapper.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "CreditAccountMapper.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "BudgetGroupMapper.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "PaymentInstrumentMapper.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "PaymentInstrumentBindingMapper.java"),
            Path.of("transaction/transaction-impl/src/main/java/com/capte/funds/transaction/dal/mapper/"
                    + "SpendSubjectFundingRelMapper.java")
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

    /**
     * 场景：PaymentInstrument 是钱包侧支付/收款工具配置能力。
     * 输入：扫描 wallet-face、core 与 transaction-face 的支付工具契约。
     * 输出：服务、DTO、Query、Request 和枚举所在模块。
     * 预期：wallet-face 拥有支付工具配置契约，core 承载共享枚举，transaction-face 不再承载该账户配置能力。
     */
    @Test
    void testWalletFaceShouldOwnPaymentInstrumentContracts() {
        Path projectRoot = projectRoot();

        assertPathsExist(projectRoot, WALLET_PAYMENT_INSTRUMENT_CONTRACTS,
                "wallet-face should expose payment instrument contract");
        assertPathsExist(projectRoot, CORE_PAYMENT_INSTRUMENT_ENUMS,
                "core should expose shared payment instrument enum");
        assertPathsDoNotExist(projectRoot, TRANSACTION_PAYMENT_INSTRUMENT_CONTRACTS,
                "transaction-face should not own payment instrument contract");
    }

    /**
     * 场景：支出主体到真实 FundingAccount 的资金来源关系是钱包账户配置能力。
     * 输入：扫描 wallet-face、core 与 transaction-face 的支出主体资金关系契约。
     * 输出：服务、DTO、Query、Request 和枚举所在模块。
     * 预期：wallet-face 拥有关联配置契约，core 承载共享枚举，transaction-face 不再承载该账户配置能力。
     */
    @Test
    void testWalletFaceShouldOwnSpendSubjectFundingRelationContracts() {
        Path projectRoot = projectRoot();

        assertPathsExist(projectRoot, WALLET_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS,
                "wallet-face should expose spend subject funding relation contract");
        assertThat(projectRoot.resolve(CORE_SPEND_SUBJECT_FUNDING_RELATION_TYPE))
                .as("core should expose shared spend subject funding relation type")
                .exists();
        assertPathsDoNotExist(projectRoot, TRANSACTION_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS,
                "transaction-face should not own spend subject funding relation contract");
    }

    /**
     * 场景：wallet-impl 是账户能力实现层，transaction-impl 只保留交易事实、路由和入账编排。
     * 输入：扫描 wallet-impl 与 transaction-impl 的账户能力实现、账户配置 Entity 和 Mapper。
     * 输出：实现类和 DAL 类所在模块。
     * 预期：账户、账本配置、平台账户、支付工具和支出主体资金关系实现归属 wallet-impl；
     *      transaction-impl 不再长期拥有这些账户能力实现和账户配置 DAL。
     * 红线：FundsTransaction、FundsFrozenOrder 等交易事实实体不在本轮迁移范围。
     */
    @Test
    void testWalletImplShouldOwnAccountCapabilityImplementationsAndDal() {
        Path projectRoot = projectRoot();

        assertPathsExist(projectRoot, WALLET_ACCOUNT_CAPABILITY_IMPLEMENTATIONS,
                "wallet-impl should own account capability implementation");
        assertPathsExist(projectRoot, WALLET_ACCOUNT_CAPABILITY_DAL,
                "wallet-impl should own account capability dal");
        assertPathsDoNotExist(projectRoot, TRANSACTION_ACCOUNT_CAPABILITY_IMPLEMENTATIONS,
                "transaction-impl should not own account capability implementation");
        assertPathsDoNotExist(projectRoot, TRANSACTION_ACCOUNT_CAPABILITY_DAL,
                "transaction-impl should not own account capability dal");
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

    private static void assertPathsExist(Path projectRoot, List<Path> contracts, String message) {
        assertThat(contracts)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as(message + ": " + contract.getFileName())
                        .exists());
    }

    private static void assertPathsDoNotExist(Path projectRoot, List<Path> contracts, String message) {
        assertThat(contracts)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as(message + ": " + contract.getFileName())
                        .doesNotExist());
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
