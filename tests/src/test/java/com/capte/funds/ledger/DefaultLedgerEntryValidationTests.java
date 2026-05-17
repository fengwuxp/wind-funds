package com.capte.funds.ledger;

import com.capte.funds.support.FundsTransactionTestSupport;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerEntryValidationTests extends LedgerTransactionPostingTestSupport {

    /**
     * 场景：账本分录缺少金额。
     * 输入：posting plan 内借方分录 amount 为 null，贷方分录正常。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：错误信息能定位到 planId、ledgerTransactionSn、主体和账目。
     * 红线：不得让缺少金额的分录进入币种、平衡或持久化阶段。
     */
    @Test
    void testPostShouldRejectEntryWithoutAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000016";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutAmount = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutAmount.setAmount(null);
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, "MISSING_AMOUNT", List.of(
                entryWithoutAmount,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录金额不能为空")
                .hasMessageContaining("MISSING_AMOUNT")
                .hasMessageContaining("LE_000000000016")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录金额为 0。
     * 输入：posting plan 借贷分录金额均为 0，形式上仍然平衡。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：账本分录金额必须大于 0，平衡不等于有效资金事实。
     * 红线：不得产生零金额账本事实污染交易追踪和余额投影。
     */
    @Test
    void testPostShouldRejectNonPositiveEntryAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000008";
        LedgerPostingPlanSpec zeroAmountPlan = uncheckedPostingPlan(ledgerTransactionSn, "ZERO_AMOUNT", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 0L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 0L)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(zeroAmountPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(zeroAmountPlan.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录金额必须大于 0")
                .hasMessageContaining("ZERO_AMOUNT")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录缺少 ledgerId。
     * 输入：posting plan 借方分录未绑定账本 ID，贷方分录正常。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每条分录必须绑定具体账本，才能校验主体、账目和 profile。
     * 红线：不得把没有 ledgerId 的分录交给余额投影或账本持久化。
     */
    @Test
    void testPostShouldRejectEntryWithoutLedgerIdBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000003";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutLedgerId = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutLedgerId.setLedgerId(null);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entryWithoutLedgerId,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录 ledgerId 不能为空")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录绑定的 ledgerId 与分录主体不一致。
     * 输入：分录 subjectId 为 funding_001，但 ledgerId 指向 funding_002 的账本。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：ledgerId、subjectId、subjectType、ledgerSubjectCode 必须共同指向同一账本。
     * 红线：不得让主体 A 的资金变化记入主体 B 的账本。
     */
    @Test
    void testPostShouldRejectLedgerBindingMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        RecordingLedgerService ledgerService = defaultLedgerService();
        ledgerService.addLedger(ledger(2L, "funding_002", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, ledgerService, List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000010";
        FundsTransactionTestSupport.MutableLedgerEntrySpec mismatchedEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        mismatchedEntry.setLedgerId(2L);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                mismatchedEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致")
                .hasMessageContaining("ledgerId = 2");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：分录要求允许负余额，但账本 profile 不允许。
     * 输入：AVAILABLE 分录设置 ALLOW_NEGATIVE，账本 profile 为不可负。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：分录级余额约束不得突破账本 profile 的余额控制红线。
     * 红线：不得通过 posting plan 参数绕过账本账户余额约束。
     */
    @Test
    void testPostShouldRejectAllowNegativeWhenLedgerProfileDisallowsNegativeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000011";
        FundsTransactionTestSupport.MutableLedgerEntrySpec allowNegativeEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        allowNegativeEntry.setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                allowNegativeEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本 profile 不允许负余额")
                .hasMessageContaining("ledgerId = 1")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

}
