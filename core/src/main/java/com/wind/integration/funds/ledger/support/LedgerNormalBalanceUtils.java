package com.wind.integration.funds.ledger.support;

import com.wind.integration.funds.ledger.enums.EntrySide;
import org.jspecify.annotations.Nullable;

/**
 * 账本 normal balance 计算工具。
 *
 * @author Codex
 * @date 2026-05-06
 */
public final class LedgerNormalBalanceUtils {

    private LedgerNormalBalanceUtils() {
        throw new AssertionError();
    }

    public static Long computeNormalBalance(@Nullable EntrySide normalBalanceSide,
                                            @Nullable Long debitAmount,
                                            @Nullable Long creditAmount) {
        if (normalBalanceSide == null || debitAmount == null || creditAmount == null) {
            return null;
        }
        long rawBalance = debitAmount - creditAmount;
        return normalBalanceSide == EntrySide.DEBIT ? rawBalance : -rawBalance;
    }
}
