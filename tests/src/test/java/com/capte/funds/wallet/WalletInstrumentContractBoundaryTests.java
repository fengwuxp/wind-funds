package com.capte.funds.wallet;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WalletInstrumentContractBoundaryTests extends WalletLayerBoundaryTestSupport {

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
}
