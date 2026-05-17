package com.capte.funds.wallet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalletLayerBoundaryTests extends WalletLayerBoundaryTestSupport {

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
}
