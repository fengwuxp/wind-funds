package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.LedgerEntryFactDTO;
import com.wind.funds.wallet.model.dto.LedgerTransactionFactDTO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 账本只读查询服务。
 *
 * <p>职责：为交易解释、审计和引用链路提供轻量账本事实快照。调用方不得通过本服务补写或修复账本事实。</p>
 */
public interface LedgerQueryService {

    @NonNull List<LedgerTransactionFactDTO> queryLedgerTransactions(@NonNull Long tenantId,
                                                                    @NonNull String fundsTransactionSn,
                                                                    @Nullable String eventType,
                                                                    int limit);

    @NonNull List<LedgerEntryFactDTO> queryLedgerEntries(@NonNull Long tenantId,
                                                         @NonNull String ledgerTransactionSn,
                                                         int limit);
}
