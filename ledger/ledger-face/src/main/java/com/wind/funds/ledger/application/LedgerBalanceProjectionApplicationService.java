package com.wind.funds.ledger.application;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import org.jspecify.annotations.NonNull;

/**
 * 账本余额投影查询应用服务。
 *
 * <p>职责：面向跨模块调用方读取由账本分录派生出的账本余额投影，
 * 用于余额校验、对账解释、清结算检查和审计查询。</p>
 *
 * <p>边界：本服务只读账本余额投影，不提供直接调整借方累计额、贷方累计额、
 * 删除账本或修正账本事实的能力。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface LedgerBalanceProjectionApplicationService {

    /**
     * 根据 ID 查询账本余额投影。
     *
     * @param id 账本 ID
     * @return 账本余额投影
     */
    @NonNull
    LedgerDTO getLedgerById(@NonNull Long id);

    /**
     * 分页查询账本余额投影。
     *
     * @param query   查询条件
     * @param options 查询选项
     * @return 账本余额投影分页结果
     */
    @NonNull
    WindPagination<LedgerDTO> queryLedgerBalances(
            @NonNull LedgerQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
