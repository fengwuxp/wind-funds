package com.capte.funds.ledger.service;

import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionCreateResult;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import com.capte.funds.ledger.dto.LedgerTransactionPostResult;
import com.capte.funds.ledger.query.LedgerEntryQuery;
import com.capte.funds.ledger.query.LedgerTransactionQuery;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 账户账本交易服务
 *
 * @author wuxp
 * @since 2026-04-14
 */
public interface LedgerTransactionService {

    /**
     * 入账账户账本交易。
     *
     * @param transaction 入账请求对象
     * @return 账户账本交易入账结果
     */
    @NonNull
    default LedgerTransactionPostResult postLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
        LedgerTransactionCreateResult result = createLedgerTransaction(transaction);
        return new LedgerTransactionPostResult()
                .setLedgerTransactionId(result.getLedgerTransactionId())
                .setNewlyPosted(result.isCreated());
    }

    /**
     * 创建 账户账本交易。
     *
     * @param transaction 创建请求对象
     * @return 账户账本交易创建结果
     * @deprecated use {@link #postLedgerTransaction(LedgerTransactionSpec)}. 该操作具有幂等入账语义。
     */
    @Deprecated(since = "1.0.1", forRemoval = false)
    @NonNull LedgerTransactionCreateResult createLedgerTransaction(@NonNull LedgerTransactionSpec transaction);

    /**
     * 更新 账户账本交易
     *
     * @param request 更新请求对象
     */
    void updateLedgerTransaction(@NonNull UpdateLedgerTransactionRequest request);

    /**
     * 删除账户账本交易
     *
     * @param id 账户账本交易 id
     */
    default void deleteLedgerTransactionById(@NonNull Long id) {
        deleteLedgerTransactionByIds(id);
    }

    /**
     * 批量删除账户账本交易
     *
     * @param ids 账户账本交易 id
     */
    void deleteLedgerTransactionByIds(@NonNull Long... ids);

    /**
     * 根据 id 查询账户账本交易
     *
     * @param id 账户账本交易 id
     * @return AccountLedgerTransaction
     */
    @NonNull
    LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id);

    /**
     * 分页查询 账户账本交易
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return AccountLedgerTransaction 分页对象
     */
    @NonNull WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(@NonNull LedgerTransactionQuery query, @NonNull WindQuery<?
            extends QueryOrderField> options);

    /**
     * 根据 id 查询账户账本条目
     *
     * @param id 账户账本条目 id
     * @return AccountLedgerEntry
     */
    @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id);

    /**
     * 分页查询 账户账本条目
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return AccountLedgerEntry 分页对象
     */
    @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(@NonNull LedgerEntryQuery query, @NonNull WindQuery<? extends QueryOrderField> options);

}
