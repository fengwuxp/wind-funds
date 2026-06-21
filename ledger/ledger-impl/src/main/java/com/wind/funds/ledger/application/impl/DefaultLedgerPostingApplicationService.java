package com.wind.funds.ledger.application.impl;

import com.wind.funds.ledger.LedgerTransactionPostingService;
import com.wind.funds.ledger.application.LedgerPostingApplicationService;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认账本入账应用服务。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class DefaultLedgerPostingApplicationService implements LedgerPostingApplicationService {

    private final LedgerTransactionPostingService ledgerTransactionPostingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
        ledgerTransactionPostingService.post(transaction);
    }
}
