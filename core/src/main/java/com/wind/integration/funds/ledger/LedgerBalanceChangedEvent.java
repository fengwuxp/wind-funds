package com.wind.integration.funds.ledger;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账本余额投影变更事件。
 *
 * <p>该事件只表达由账本分录和余额投影派生的业务观察信号，不是新的余额事实源。
 *
 * @author Codex
 * @date 2026-05-08
 */
@Getter
@Builder
public class LedgerBalanceChangedEvent {

    /**
     * 账务主体 ID。
     */
    private final String subjectId;

    /**
     * 账务主体类型。
     */
    private final String subjectType;

    /**
     * 账本 ID。
     */
    private final Long ledgerId;

    /**
     * 账本科目编码。
     */
    private final LedgerSubjectCode ledgerSubjectCode;

    /**
     * 币种。
     */
    private final CurrencyIsoCode currency;

    /**
     * 变更前正常余额，单位：分。
     */
    private final Long beforeBalance;

    /**
     * 变更后正常余额，单位：分。
     */
    private final Long balance;

    /**
     * 本次变更额，单位：分。
     */
    private final Long balanceDelta;

    /**
     * 账本交易流水。
     */
    private final String ledgerTransactionSn;

    /**
     * 账本分录流水。当前投影输入无法提供持久化分录流水时，该字段可为空。
     */
    private final String ledgerEntrySn;

    /**
     * 账本分录摘要，用于在缺少持久化分录流水时辅助追溯来源分录。
     */
    private final String ledgerEntryDigest;

    /**
     * 业务场景。
     */
    private final String businessScene;

    /**
     * 业务流水。
     */
    private final String businessSn;

    /**
     * 交易发生时间。
     */
    private final LocalDateTime transactionTime;

    /**
     * 来源分录上下文。
     */
    private final Map<String, Object> contextVariables;
}
