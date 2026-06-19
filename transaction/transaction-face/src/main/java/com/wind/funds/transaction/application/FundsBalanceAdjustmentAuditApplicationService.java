package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.dto.FundsBalanceAdjustmentAuditDTO;
import com.wind.funds.transaction.model.query.FundsBalanceAdjustmentAuditQuery;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * 余额调账审计查询应用服务。
 *
 * <p>职责边界：只聚合已经落库的资金交易事实、RouteSnapshot、账本交易和账本分录，
 * 不生成、不修复、不重放任何 route、ledger、projection 或余额事实。</p>
 *
 * @author Codex
 * @date 2026-06-19
 */
public interface FundsBalanceAdjustmentAuditApplicationService {

    /**
     * 按业务场景和业务流水查询余额调账审计链路。
     *
     * @param query 查询条件，必须包含 tenantId、businessScene 和 businessSn
     * @return 已存在的余额调账审计链路，不存在时返回 empty
     */
    @NonNull
    Optional<FundsBalanceAdjustmentAuditDTO> findByBusinessSn(
            @NonNull FundsBalanceAdjustmentAuditQuery query);

    /**
     * 按资金交易流水查询余额调账审计链路。
     *
     * @param query 查询条件，必须包含 tenantId 和 fundsTransactionSn
     * @return 已存在的余额调账审计链路，不存在时返回 empty
     */
    @NonNull
    Optional<FundsBalanceAdjustmentAuditDTO> findByTransactionSn(
            @NonNull FundsBalanceAdjustmentAuditQuery query);
}
