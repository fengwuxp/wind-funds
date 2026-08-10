package com.wind.funds.ledger;

import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 账本余额投影服务
 *
 * @author wuxp
 * @date 2026-04-14 09:28
 **/
public interface LedgerBalanceProjectionService {

    /**
     * 投影
     *
     * @param entries 账本条目定义
     */
    default void project(@NonNull List<LedgerEntrySpec> entries) {
        project(entries, LedgerPostingAccessType.NORMAL);
    }

    /**
     * 投影
     *
     * @param entries           账本条目定义
     * @param postingAccessType 入账准入类型
     */
    void project(@NonNull List<LedgerEntrySpec> entries, @NonNull LedgerPostingAccessType postingAccessType);

    /**
     * 是否支持
     *
     * @param accountId 账户ID
     * @return true:支持
     */
    boolean supports(@NonNull FundsAccountId accountId);
}
