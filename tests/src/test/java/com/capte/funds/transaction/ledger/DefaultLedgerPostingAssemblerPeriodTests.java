package com.capte.funds.transaction.ledger;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultLedgerPostingAssemblerPeriodTests extends DefaultLedgerPostingAssemblerTestSupport {

    /**
     * 场景：RouteLeg 未显式提供 periodId，但账期类型为默认生命周期账期。
     * 输入：`periodType=LIFETIME` 且 `periodId=null` 的路径。
     * 输出：组装得到的账本分录 ledgerId 列表。
     * 预期：Assembler 自动补齐 `LIFETIME` 账期标识，并正确命中生命周期账本。
     * 红线：默认生命周期账期不得因为 periodId 缺省而错配到账期 bucket。
     */
    @Test
    void testAssembleShouldDefaultLifetimePeriodIdWhenLegDoesNotProvideIt() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT)
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001",
                route(new MissingPeriodIdRouteLeg()));

        LedgerPostingPhaseSpec phase = transaction.getPostingPlans().getFirst().getPostingPhases().getFirst();
        assertThat(phase.getEntries())
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L);
    }

    /**
     * 场景：同主体同科目存在多个不同账期的账本 bucket。
     * 输入：月账期路径，目标 periodId 为 `2026-05`。
     * 输出：组装得到的账本分录 ledgerId 列表。
     * 预期：Assembler 只能命中同周期账本，不得串到 `2026-04` 等其他 period bucket。
     * 红线：同主体同科目不同账期的资金余额不得相互污染。
     */
    @Test
    void testAssembleShouldUsePeriodKeyForLedgerLookup() {
        DefaultLedgerPostingAssembler assembler = new DefaultLedgerPostingAssembler(ledgerService(Map.ofEntries(
                ledger(101L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-05"),
                ledger(102L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-05"),
                ledger(201L, "funding_001", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-04"),
                ledger(202L, "funding_002", LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
                        AccountBalancePeriodType.MONTHLY, "2026-04")
        )));

        LedgerTransactionSpec transaction = assembler.assemble(instruction(), "FT_001",
                route(new MonthlyRouteLeg("2026-05")));

        LedgerPostingPhaseSpec phase = transaction.getPostingPlans().getFirst().getPostingPhases().getFirst();
        assertThat(phase.getEntries())
                .extracting(LedgerEntrySpec::getLedgerId)
                .containsExactly(101L, 102L)
                .doesNotContain(201L, 202L);
    }
}
