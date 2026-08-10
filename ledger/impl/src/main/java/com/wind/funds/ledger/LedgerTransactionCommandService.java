package com.wind.funds.ledger;

import com.wind.funds.ledger.dto.LedgerTransactionPostResult;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 账本实现模块内部的账本交易写入端口。
 *
 * @author wuxp
 * @since 2026-08-10
 */
public interface LedgerTransactionCommandService {

    /**
     * 持久化账本交易、记账计划和分录。
     *
     * @param transaction 账本交易
     * @return 入账结果
     */
    @NonNull LedgerTransactionPostResult postLedgerTransaction(@NonNull LedgerTransactionSpec transaction);
}
