package com.capte.funds.transaction.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerPostingAssemblerValidationTests extends DefaultLedgerPostingAssemblerTestSupport {

    /**
     * 场景：交易路径引用的主体账本尚未初始化。
     * 输入：无法查询到账本 bucket 的转账路径。
     * 输出：Assembler 在生成分录前失败。
     * 预期：写流程缺账本直接拒绝，不自动建账。
     * 红线：Route -> Posting 不得在交易路径中隐式创建账本或降级为空入账。
     */
    @Test
    void testAssembleShouldRejectMissingLedger() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.of()));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本不存在或不唯一");
    }

    /**
     * 场景：交易路径金额币种与账本币种不一致。
     * 输入：路径金额为 USD，但目标主体账本为 EUR。
     * 输出：Assembler 在生成分录前拒绝。
     * 预期：账本 bucket 币种必须和 route leg 金额币种一致。
     * 红线：不得把未经过换汇建模的多币种资金变化写入同一条 route leg。
     */
    @Test
    void testAssembleShouldRejectLedgerCurrencyMismatch() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, CurrencyIsoCode.EUR)
        )));

        assertThatThrownBy(() -> assembler.assemble(instruction(), "FT_001", route(Map.of())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本币种与路径金额币种不一致");
    }
}
