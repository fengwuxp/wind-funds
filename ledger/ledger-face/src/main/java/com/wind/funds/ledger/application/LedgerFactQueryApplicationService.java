package com.wind.funds.ledger.application;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import org.jspecify.annotations.NonNull;

/**
 * 账本事实查询应用服务。
 *
 * <p>职责：面向交易、清结算、对账和审计等跨模块调用方，查询不可变账本交易事实
 * 与账目分录事实。</p>
 *
 * <p>边界：本服务只读账本事实，不更新、删除或补写账本交易、账务计划和账目分录。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface LedgerFactQueryApplicationService {

    /**
     * 根据 ID 查询账本交易事实。
     *
     * @param id 账本交易 ID
     * @return 账本交易事实
     */
    @NonNull
    LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id);

    /**
     * 分页查询账本交易事实。
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return 账本交易分页结果
     */
    @NonNull
    WindPagination<LedgerTransactionDTO> queryLedgerTransactions(
            @NonNull LedgerTransactionQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 根据 ID 查询账目分录事实。
     *
     * @param id 账目分录 ID
     * @return 账目分录事实
     */
    @NonNull
    LedgerEntryDTO getLedgerEntryById(@NonNull Long id);

    /**
     * 分页查询账目分录事实。
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return 账目分录分页结果
     */
    @NonNull
    WindPagination<LedgerEntryDTO> queryLedgerEntries(
            @NonNull LedgerEntryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
