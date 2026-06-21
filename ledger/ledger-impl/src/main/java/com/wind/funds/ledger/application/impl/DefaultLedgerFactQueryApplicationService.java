package com.wind.funds.ledger.application.impl;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.application.LedgerFactQueryApplicationService;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.funds.ledger.service.LedgerTransactionService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认账本事实查询应用服务。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class DefaultLedgerFactQueryApplicationService implements LedgerFactQueryApplicationService {

    private final LedgerTransactionService ledgerTransactionService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
        return ledgerTransactionService.getLedgerTransactionById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull WindPagination<LedgerTransactionDTO> queryLedgerTransactions(
            @NonNull LedgerTransactionQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return ledgerTransactionService.queryAccountLedgerTransactions(query, options);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
        return ledgerTransactionService.getLedgerEntryById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
            @NonNull LedgerEntryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return ledgerTransactionService.queryLedgerEntries(query, options);
    }
}
