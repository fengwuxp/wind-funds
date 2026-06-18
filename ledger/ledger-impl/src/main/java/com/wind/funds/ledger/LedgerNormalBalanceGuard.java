package com.wind.funds.ledger;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;

/**
 * 账本科目类别与正常余额方向护栏。
 *
 * @author Codex
 * @date 2026-06-18
 */
public final class LedgerNormalBalanceGuard {

    private LedgerNormalBalanceGuard() {
        throw new AssertionError();
    }

    public static void assertCategoryNormalBalance(String owner,
                                                   Long ledgerId,
                                                   LedgerSubjectCategory category,
                                                   EntrySide normalBalanceSide) {
        EntrySide expected = category == null ? null : category.getNormalBalance();
        if (expected == null) {
            return;
        }
        AssertUtils.isTrue(expected == normalBalanceSide,
                "{}账本科目类别与正常余额方向不一致，ledgerId = {}, ledgerSubjectCategory = {}, normalBalanceSide = {}, expectedNormalBalanceSide = {}",
                owner,
                ledgerId,
                category,
                normalBalanceSide,
                expected);
    }
}
