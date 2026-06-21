package com.wind.funds.ledger.application;

import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 账本入账应用服务。
 *
 * <p>职责：作为跨模块生产调用方的账本写入入口，消费已经解析完成、
 * 已满足账务平衡要求的 LedgerTransactionSpec，并委托账本入账内核完成
 * 账本交易、账务计划、账目分录和余额投影。</p>
 *
 * <p>边界：本服务不暴露账本余额直接更新、账本事实更新或删除能力；需要修正账务事实时，
 * 应通过冲正、退款、调整或受控运维流程形成新的账本事实。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface LedgerPostingApplicationService {

    /**
     * 入账账本交易。
     *
     * @param transaction 账本交易事实
     */
    void postLedgerTransaction(@NonNull LedgerTransactionSpec transaction);
}
