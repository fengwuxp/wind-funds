package com.wind.funds.wallet.services.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.funds.ledger.service.LedgerTransactionService;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.model.dto.LedgerEntryFactDTO;
import com.wind.funds.wallet.model.dto.LedgerTransactionFactDTO;
import com.wind.funds.wallet.service.LedgerFactQueryService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 默认账本事实只读查询服务。
 */
@Service
@AllArgsConstructor
public class DefaultLedgerFactQueryService implements LedgerFactQueryService {

    private final LedgerTransactionService ledgerTransactionService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<LedgerTransactionFactDTO> queryLedgerTransactions(@NonNull Long tenantId,
                                                                           @NonNull String fundsTransactionSn,
                                                                           @Nullable FundsTransactionEventType eventType,
                                                                           int limit) {
        LedgerTransactionQuery query = new LedgerTransactionQuery()
                .setTenantId(tenantId)
                .setFundsTransactionSn(fundsTransactionSn);
        if (eventType != null) {
            query.setEventType(eventType.name());
        }
        return ledgerTransactionService.queryAccountLedgerTransactions(query, DefaultPageQueryOptions.defaults(limit))
                .getRecords()
                .stream()
                .map(this::toTransactionFact)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<LedgerEntryFactDTO> queryLedgerEntries(@NonNull Long tenantId,
                                                                @NonNull String ledgerTransactionSn,
                                                                int limit) {
        return ledgerTransactionService.queryLedgerEntries(new LedgerEntryQuery()
                                .setTenantId(tenantId)
                                .setLedgerTransactionSn(ledgerTransactionSn),
                        DefaultPageQueryOptions.defaults(limit))
                .getRecords()
                .stream()
                .map(this::toEntryFact)
                .toList();
    }

    private LedgerTransactionFactDTO toTransactionFact(LedgerTransactionDTO source) {
        return new LedgerTransactionFactDTO()
                .setSn(source.getSn())
                .setTenantId(source.getTenantId())
                .setFundsTransactionSn(source.getFundsTransactionSn())
                .setEventType(source.getEventType());
    }

    private LedgerEntryFactDTO toEntryFact(LedgerEntryDTO source) {
        return new LedgerEntryFactDTO()
                .setSn(source.getSn())
                .setLedgerTransactionSn(source.getLedgerTransactionSn())
                .setFundsTransactionSn(source.getFundsTransactionSn())
                .setBusinessScene(source.getBusinessScene())
                .setBusinessSn(source.getBusinessSn())
                .setSubjectId(source.getSubjectId())
                .setSubjectType(source.getSubjectType())
                .setLedgerSubjectCode(source.getLedgerSubjectCode())
                .setLedgerSubjectCategory(source.getLedgerSubjectCategory())
                .setEntryType(source.getEntryType())
                .setBalanceConstraintType(source.getBalanceConstraintType())
                .setBalanceEffectType(source.getBalanceEffectType())
                .setAmount(source.getAmount() == null ? null : source.getAmount().getAmount())
                .setCurrency(source.getAmount() == null ? null : source.getAmount().getCurrency().name());
    }
}
