package com.capte.funds.ledger.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerBalanceProjectionValidationTests extends LedgerBalanceProjectionServiceImplTestSupport {

    /**
     * 场景：一次余额投影请求混入多个资金账户分录。
     * 输入：funding_001 和 funding_002 的分录被放在同一个 project 调用。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：单次投影只处理同一资金账户，避免账户维度锁定和审计混乱。
     * 红线：不得把多个资金主体的余额变化混在一个账户投影事务中。
     */
    @Test
    void testProjectShouldRejectEntriesFromDifferentFundsAccounts() {
        FundsAccountId firstAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        FundsAccountId secondAccountId = FundsAccountId.immutable("funding_002", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(ledger(99L, EntrySide.DEBIT));
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(firstAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(
                entry(firstAccountId, 99L, EntrySide.DEBIT, 100L),
                entry(secondAccountId, 99L, EntrySide.CREDIT, 100L)
        )))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影只允许处理同一资金账户的分录");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录主体与账本主体不一致。
     * 输入：分录主体为 funding_001，但 ledgerId 指向 funding_002 的账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：不生成余额更新请求，错误信息说明主体不一致。
     * 红线：主体 A 的分录不得更新主体 B 的账本余额。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerBelongsToAnotherSubjectBeforeUpdate() {
        FundsAccountId entryAccountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO anotherSubjectLedger = ledger(99L, EntrySide.DEBIT)
                .setSubjectId("funding_002")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(anotherSubjectLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(entryAccountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(entryAccountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录科目与账本科目不一致。
     * 输入：分录科目为 AVAILABLE，但 ledgerId 指向 CASH 账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：分录科目和账本科目必须一致，且不生成余额更新请求。
     * 红线：不得把一个余额桶的资金变化更新到另一个账目。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerSubjectCodeDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO cashLedger = ledger(99L, LedgerSubjectCode.CASH, LedgerSubjectCategory.ASSET, EntrySide.DEBIT);
        RecordingLedgerService ledgerService = new RecordingLedgerService(cashLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录科目与账本科目不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录币种与账本币种不一致。
     * 输入：分录币种为 USD，但 ledgerId 指向 EUR 账本。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：分录币种和账本币种必须一致，且不生成余额更新请求。
     * 红线：不得在余额投影阶段隐式完成换汇或跨币种记账。
     */
    @Test
    void testProjectShouldRejectEntryWhenLedgerCurrencyDoesNotMatchBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO eurLedger = ledger(99L, EntrySide.DEBIT)
                .setCurrency(CurrencyIsoCode.EUR);
        RecordingLedgerService ledgerService = new RecordingLedgerService(eurLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录币种与账本币种不一致");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：分录 ledgerId 指向不存在的账本。
     * 输入：分录绑定 ledgerId=99，但 LedgerService 返回 null。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息能定位 ledgerId，且不生成余额更新请求。
     * 红线：写流程缺账本必须失败，不得自动建账或用空账本继续投影。
     */
    @Test
    void testProjectShouldRejectMissingLedgerBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        RecordingLedgerService ledgerService = new RecordingLedgerService(null);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(accountId, 99L, EntrySide.DEBIT, 100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本不存在")
                .hasMessageContaining("ledgerId = 99");
        assertThat(ledgerService.updateRequests).isEmpty();
    }

    /**
     * 场景：资金账户余额视图缺少分录对应的余额桶。
     * 输入：分录科目为 FROZEN，但当前余额视图只初始化 AVAILABLE。
     * 输出：余额投影在更新账本前拒绝。
     * 预期：错误信息能定位主体、账目和 ledgerId，且不生成余额更新请求。
     * 红线：写流程缺余额桶必须失败，不得把未初始化余额当 0 静默投影。
     */
    @Test
    void testProjectShouldRejectMissingBalanceBucketBeforeUpdate() {
        FundsAccountId accountId = FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT.name());
        LedgerDTO frozenLedger = ledger(99L, LedgerSubjectCode.FROZEN, LedgerSubjectCategory.CONTROL, EntrySide.CREDIT);
        RecordingLedgerService ledgerService = new RecordingLedgerService(frozenLedger);
        LedgerBalanceProjectionServiceImpl service = new LedgerBalanceProjectionServiceImpl(
                newFundsAccountQueryService(accountId, LedgerSubjectCode.AVAILABLE, 100L),
                ledgerService
        );

        assertThatThrownBy(() -> service.project(List.of(entry(
                accountId,
                99L,
                LedgerSubjectCode.FROZEN,
                LedgerSubjectCategory.CONTROL,
                EntrySide.CREDIT,
                100L
        ))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金账户余额桶未初始化")
                .hasMessageContaining("ledgerId = 99")
                .hasMessageContaining(LedgerSubjectCode.FROZEN.name());
        assertThat(ledgerService.updateRequests).isEmpty();
    }
}
