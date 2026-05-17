package com.capte.funds.wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

abstract class WalletLayerBoundaryTestSupport {

    protected static final List<Path> WALLET_SOURCE_ROOTS = List.of(
            Path.of("wallet/wallet-face/src/main/java"),
            Path.of("wallet/wallet-impl/src/main/java")
    );

    protected static final Path WALLET_BALANCE_QUERY_SERVICE = Path.of(
            "wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/FundsSubjectBalanceQueryService.java");

    protected static final Path TRANSACTION_BALANCE_QUERY_SERVICE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "FundsSubjectBalanceQueryService.java");

    protected static final List<Path> WALLET_LEDGER_PROFILE_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/LedgerProfileService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/SubjectLedgerInitializer.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/LedgerProfileDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/LedgerProfileItemDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "InitializeSubjectLedgerRequest.java")
    );

    protected static final List<Path> TRANSACTION_LEDGER_PROFILE_CONTRACTS = List.of(
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "LedgerProfileService.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "SubjectLedgerInitializer.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/"
                    + "LedgerProfileDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/"
                    + "LedgerProfileItemDTO.java"),
            Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/"
                    + "InitializeSubjectLedgerRequest.java")
    );

    protected static final Path WALLET_PLATFORM_FUNDING_ACCOUNT_SERVICE = Path.of(
            "wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/PlatformFundingAccountService.java");

    protected static final Path CORE_PLATFORM_FUNDING_ACCOUNT_ROLE = Path.of(
            "core/src/main/java/com/wind/integration/funds/wallet/enums/PlatformFundingAccountRole.java");

    protected static final Path TRANSACTION_PLATFORM_FUNDING_ACCOUNT_SERVICE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/"
                    + "PlatformFundingAccountService.java");

    protected static final Path TRANSACTION_PLATFORM_FUNDING_ACCOUNT_ROLE = Path.of(
            "transaction/transaction-face/src/main/java/com/capte/funds/transaction/enums/"
                    + "PlatformFundingAccountRole.java");

    protected static final List<String> ACCOUNT_CONTRACT_SIMPLE_NAMES = List.of(
            "FundingAccount",
            "CreditAccount",
            "BudgetGroup"
    );

    protected static final List<Path> WALLET_PAYMENT_INSTRUMENT_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/PaymentInstrumentService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/PaymentInstrumentDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/"
                    + "PaymentInstrumentBindingDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/PaymentInstrumentQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/"
                    + "PaymentInstrumentBindingQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "CreatePaymentInstrumentRequest.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "CreatePaymentInstrumentBindingRequest.java")
    );

    protected static final List<Path> CORE_PAYMENT_INSTRUMENT_ENUMS = List.of(
            Path.of("core/src/main/java/com/wind/integration/funds/wallet/enums/PaymentInstrumentDirection.java"),
            Path.of("core/src/main/java/com/wind/integration/funds/wallet/enums/PaymentInstrumentBindingRole.java")
    );

    protected static final List<Path> TRANSACTION_PAYMENT_INSTRUMENT_CONTRACTS = List.of(
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

    protected static final List<Path> WALLET_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS = List.of(
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/"
                    + "SpendSubjectFundingRelationService.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/"
                    + "SpendSubjectFundingRelationDTO.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/"
                    + "SpendSubjectFundingRelationQuery.java"),
            Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/"
                    + "CreateSpendSubjectFundingRelationRequest.java")
    );

    protected static final Path CORE_SPEND_SUBJECT_FUNDING_RELATION_TYPE = Path.of(
            "core/src/main/java/com/wind/integration/funds/wallet/enums/SpendSubjectFundingRelationType.java");

    protected static final List<Path> TRANSACTION_SPEND_SUBJECT_FUNDING_RELATION_CONTRACTS = List.of(
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

    protected static final List<Path> WALLET_ACCOUNT_CAPABILITY_IMPLEMENTATIONS = List.of(
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

    protected static final List<Path> WALLET_ACCOUNT_CAPABILITY_DAL = List.of(
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

    protected static final List<Path> TRANSACTION_ACCOUNT_CAPABILITY_IMPLEMENTATIONS = List.of(
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

    protected static final List<Path> TRANSACTION_ACCOUNT_CAPABILITY_DAL = List.of(
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
            "com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder",
            "com.capte.funds.transaction.services.FundsInstructionLifecycleSaver",
            "com.capte.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver",
            "com.wind.integration.funds.ledger.LedgerBalanceProjectionService",
            "com.wind.integration.funds.ledger.LedgerTransactionPostingService"
    );

    protected static List<String> findForbiddenReferences(Path sourceRoot) throws IOException {
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

    protected static void assertPathsExist(Path projectRoot, List<Path> contracts, String message) {
        assertThat(contracts)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as(message + ": " + contract.getFileName())
                        .exists());
    }

    protected static void assertPathsDoNotExist(Path projectRoot, List<Path> contracts, String message) {
        assertThat(contracts)
                .allSatisfy(contract -> assertThat(projectRoot.resolve(contract))
                        .as(message + ": " + contract.getFileName())
                        .doesNotExist());
    }

    protected static Path walletService(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/service/" + contractName
                + "Service.java");
    }

    protected static Path walletDto(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/dto/" + contractName
                + "DTO.java");
    }

    protected static Path walletQuery(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/query/" + contractName
                + "Query.java");
    }

    protected static Path walletRequest(String contractName) {
        return Path.of("wallet/wallet-face/src/main/java/com/capte/funds/wallet/model/request/Create" + contractName
                + "Request.java");
    }

    protected static Path transactionService(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/services/" + contractName
                + "Service.java");
    }

    protected static Path transactionDto(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/dto/" + contractName
                + "DTO.java");
    }

    protected static Path transactionQuery(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/query/"
                + contractName + "Query.java");
    }

    protected static Path transactionRequest(String contractName) {
        return Path.of("transaction/transaction-face/src/main/java/com/capte/funds/transaction/model/request/Create"
                + contractName + "Request.java");
    }

    protected static Path projectRoot() {
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
