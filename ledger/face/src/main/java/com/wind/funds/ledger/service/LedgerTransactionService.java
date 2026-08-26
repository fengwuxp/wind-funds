package com.wind.funds.ledger.service;

import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.query.LedgerEntryQuery;
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
     * 判断记账计划是否属于指定账本交易。
     *
     * <p>用于清分、对账等只读消费方验证分录来源链，不暴露 LedgerPostingPlan Entity。</p>
     *
     * @param tenantId           租户 ID
     * @param postingPlanSn      记账计划流水号
     * @param ledgerTransactionSn 账本交易流水号
     * @return true 表示记账计划存在且属于指定账本交易
     */
    boolean existsPostingPlan(@NonNull Long tenantId,
                              @NonNull String postingPlanSn,
                              @NonNull String ledgerTransactionSn);

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
