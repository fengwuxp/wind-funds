package com.capte.funds.ledger;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerPostingScope;
import com.wind.integration.funds.ledger.enums.LedgerReconcileStatus;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本分录摘要契约测试。
 */
class LedgerEntryDigestContractTests extends LedgerEntryDigestContractTestSupport {

    /**
     * 场景：同一笔账本分录重建后应得到稳定摘要。
     * 输入：生产 LedgerTransactionServiceImpl 生成的账本分录。
     * 输出：分录级 sha256 摘要与测试侧契约字段重算结果。
     * 预期：摘要只由稳定账务事实字段决定，重算结果与持久化摘要一致。
     * 红线：不得把 entry sn、数据库 ID、posting plan sn 或审计时间纳入幂等摘要。
     */
    @Test
    void testLedgerEntryDigestShouldUseStableAccountingFieldsOnly() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        assertThat(stableEntryHash(entry)).isEqualTo(originalHash);

        entry.setSn("LGE999999");
        entry.setId(999L);
        entry.setLedgerTransactionSn("LEDGER_TXN_OTHER");
        entry.setPostingPlanSn("PLAN_OTHER");
        entry.setFundsTransactionSn("FUNDS_TXN_OTHER");
        entry.setLedgerId(999L);
        entry.setGmtCreate(LocalDateTime.of(2026, 5, 8, 11, 0));
        entry.setGmtModified(LocalDateTime.of(2026, 5, 8, 11, 30));
        entry.setDescription("changed display text");
        entry.setContextVariables("{\"traceId\":\"TRACE_999\"}");
        entry.setSettlementStatus(LedgerSettlementStatus.FAILED);
        entry.setSettlementCompletedTime(LocalDateTime.of(2026, 5, 8, 12, 0));
        entry.setReconcileStatus(LedgerReconcileStatus.MATCHED);
        entry.setReconciliationBatch("RECON_202605080001");
        entry.setReconciliationCompletedTime(LocalDateTime.of(2026, 5, 8, 12, 30));

        assertThat(stableEntryHash(entry)).isEqualTo(originalHash);
    }

    /**
     * 场景：账本分录摘要必须覆盖核心账务语义，防止重建时把不同账务事实误判为同一事实。
     * 输入：生产 LedgerTransactionServiceImpl 生成的账本分录，依次改变账目类别、intent、scope、effect 和 phase。
     * 输出：变更前后的分录级 sha256 摘要。
     * 预期：任一核心账务语义字段变化都会改变摘要。
     * 红线：不得只用金额、主体和业务流水生成摘要，导致冻结、消费、费用等不同语义互相覆盖。
     */
    @Test
    void testLedgerEntryDigestShouldIncludeAccountingSemantics() {
        LedgerEntry entry = firstPersistedEntry();
        String originalHash = entry.getSha256();

        entry.setLedgerSubjectCategory(LedgerSubjectCategory.ASSET);
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY);
        entry.setIntent(LedgerPostingIntentType.FEE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setIntent(LedgerPostingIntentType.TRANSFER.name());
        entry.setPostingScope(LedgerPostingScope.WITHIN_SUBJECT.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setPostingScope(LedgerPostingScope.BETWEEN_SUBJECTS.name());
        entry.setBalanceEffectType(LedgerBalanceEffectType.RESTORE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);

        entry.setBalanceEffectType(LedgerBalanceEffectType.CONSUME.name());
        entry.setPhaseCode(LedgerPhaseCode.FEE.name());
        assertThat(stableEntryHash(entry)).isNotEqualTo(originalHash);
    }

}
