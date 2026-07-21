package com.wind.funds.ledger.service;

import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

/**
 * 账户账本交易服务
 *
 * @author wuxp
 * @since 2026-04-14
 */
public interface LedgerTransactionService {

    /**
     * 根据 id 查询账户账本交易
     *
     * @param id 账户账本交易 id
     * @return AccountLedgerTransaction
     */
    @NonNull
    LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id);

    /**
     * 根据账本交易流水号查询账户账本交易。
     *
     * <p>该方法用于跨模块按稳定业务流水读取账本交易事实，查不到时抛出业务异常。</p>
     *
     * @param tenantId 租户 ID
     * @param sn       账本交易流水号
     * @return 账户账本交易
     */
    @NonNull
    LedgerTransactionDTO getLedgerTransactionBySn(@NonNull Long tenantId, @NonNull String sn);

    /**
     * 分页查询 账户账本交易
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return AccountLedgerTransaction 分页对象
     */
    @NonNull
    WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(
            @NonNull LedgerTransactionQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 根据 id 查询账户账本条目
     *
     * @param id 账户账本条目 id
     * @return AccountLedgerEntry
     */
    @NonNull
    LedgerEntryDTO getLedgerEntryById(@NonNull Long id);

    /**
     * 根据账目分录流水号查询账户账本条目。
     *
     * <p>该方法用于跨模块按稳定业务流水读取账目分录事实，查不到时抛出业务异常。</p>
     *
     * @param tenantId 租户 ID
     * @param sn       账目分录流水号
     * @return 账户账本条目
     */
    @NonNull
    LedgerEntryDTO getLedgerEntryBySn(@NonNull Long tenantId, @NonNull String sn);

    /**
     * 分页查询 账户账本条目
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return AccountLedgerEntry 分页对象
     */
    @NonNull
    WindPagination<LedgerEntryDTO> queryLedgerEntries(
            @NonNull LedgerEntryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

}
