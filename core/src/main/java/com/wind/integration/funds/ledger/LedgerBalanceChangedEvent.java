package com.wind.integration.funds.ledger;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import lombok.Getter;

/**
 * 账本余额投影变更事件。
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
}
