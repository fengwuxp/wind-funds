package com.wind.funds.ledger.service;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;


import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * 账户账本服务
 *
 * @author wuxp
 * @since 2026-04-24
 */
public interface LedgerService {

    /**
     * 创建 账户账本
     *
     * @param request 创建请求对象
     * @return 账户账本 ID
     */
    @NonNull Long createLedger(@NonNull CreateLedgerRequest request);

    /**
     * 更新 账户账本
     *
     * @param request 更新请求对象
     */
    void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request);

    /**
     * 删除账户账本
     *
     * @param id 账户账本 id
     */
    default void deleteLedgerById(@NonNull Long id) {
        deleteLedgerByIds(id);
    }

    /**
     * 批量删除账户账本
     *
     * @param ids 账户账本 id
     */
    void deleteLedgerByIds(@NonNull Long... ids);

    /**
     * 根据 id 查询账户账本
     *
     * @param id 账户账本 id
     * @return Ledger
     */
    @NonNull LedgerDTO getLedgerById(@NonNull Long id);

    /**
     * 根据 id 查询账户账本
     *
     * @param ids 账户账本 ids
     * @return Ledger 集合
     */
    @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids);

    /**
     * 分页查询 账户账本
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return Ledger 分页对象
     */
    @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query, @NonNull WindQuery<? extends QueryOrderField> options);

}