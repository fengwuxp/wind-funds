package com.capte.funds.transaction.services;

import com.capte.funds.transaction.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.transaction.model.query.FundsSubjectBalanceQuery;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 资金主体余额查询服务。
 *
 * <p>职责：只读取 ledger 当前余额投影，不初始化账本，不修复余额，不承载历史回放能力。</p>
 *
 * @author Codex
 * @date 2026-05-12
 */
public interface FundsSubjectBalanceQueryService {

    /**
     * 查询当前余额。
     *
     * <p>能力范围：按同一租户、同一币种批量读取主体当前余额；返回结果顺序必须与 subjectRefs 输入顺序一致。
     * 主体尚未建账时返回 initialized=false 和空余额桶。</p>
     *
     * @param query 查询条件
     * @return 当前余额列表
     */
    @NonNull List<FundsSubjectBalanceDTO> queryCurrentBalances(@NonNull FundsSubjectBalanceQuery query);

    /**
     * 查询单个必需当前余额。
     *
     * <p>要求 query.subjectRefs 只包含一个主体；主体尚未建账或缺少请求的账本科目时直接失败。</p>
     *
     * @param query 查询条件
     * @return 当前余额
     */
    @NonNull FundsSubjectBalanceDTO getRequiredCurrentBalance(@NonNull FundsSubjectBalanceQuery query);
}
