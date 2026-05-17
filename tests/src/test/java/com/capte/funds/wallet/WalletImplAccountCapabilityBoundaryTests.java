package com.capte.funds.wallet;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class WalletImplAccountCapabilityBoundaryTests extends WalletLayerBoundaryTestSupport {

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
}
