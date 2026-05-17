package com.capte.funds.ledger;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本分录汇率摘要契约测试。
 */
class LedgerEntryExchangeRateDigestContractTests extends LedgerEntryDigestContractTestSupport {

    /**
     * 场景：跨币种或锁汇账务分录重建时，汇率必须参与摘要。
     * 输入：原币 EUR、目标币 USD、汇率 1.10 的账本分录。
     * 输出：修改汇率前后的分录级 sha256 摘要。
     * 预期：汇率变化会改变摘要。
     * 红线：不得忽略汇率导致不同外汇事实命中同一幂等摘要。
     */
    @Test
    void testLedgerEntryDigestShouldIncludeExchangeRate() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        entry.setExchangeRate(new BigDecimal("1.20"));

        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);
    }
}
