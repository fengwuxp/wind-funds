package com.capte.funds.transaction.contract;

import com.capte.funds.transaction.application.FundsBalanceControlService;
import com.capte.funds.transaction.application.FundsBalanceControlTransactionService;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionServiceApiContractTests {

    /**
     * 场景：P0-G 命名治理中，余额控制服务从交易命名中收敛出来。
     * 输入：新的 FundsBalanceControlService、旧的 FundsBalanceControlTransactionService 和统一实现类。
     * 输出：旧契约作为新契约的废弃兼容别名，实现类可按新旧类型注入。
     * 预期：冻结、解冻、调账语义归属余额控制命令，不再被主命名误导为标准资金交易事实。
     * 红线：不得破坏旧调用方编译兼容，也不得把余额控制服务迁回 wallet 层。
     */
    @Test
    void testBalanceControlServiceShouldReplaceTransactionNamedCompatibilityAlias() {
        assertThat(FundsBalanceControlService.class)
                .isAssignableFrom(FundsBalanceControlTransactionService.class);
        assertThat(FundsBalanceControlService.class)
                .isAssignableFrom(FundsTransactionCommandServiceImpl.class);
        assertThat(FundsBalanceControlTransactionService.class)
                .isAssignableFrom(FundsTransactionCommandServiceImpl.class);
        assertThat(FundsBalanceControlTransactionService.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
    }
}
