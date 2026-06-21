package com.wind.funds.ledger.application.impl;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.application.LedgerBalanceProjectionApplicationService;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认账本余额投影查询应用服务。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class DefaultLedgerBalanceProjectionApplicationService implements LedgerBalanceProjectionApplicationService {

    private final LedgerService ledgerService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
        return ledgerService.getLedgerById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull WindPagination<LedgerDTO> queryLedgerBalances(
            @NonNull LedgerQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        return ledgerService.queryLedgers(query, options);
    }
}
