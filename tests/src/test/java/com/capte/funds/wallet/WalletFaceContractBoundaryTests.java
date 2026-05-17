package com.capte.funds.wallet;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WalletFaceContractBoundaryTests extends WalletLayerBoundaryTestSupport {

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
}
