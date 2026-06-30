package com.wind.funds.wallet.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账本交易事实轻量快照。
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class LedgerTransactionFactDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7635923965038380285L;

    private String sn;

    private Long tenantId;

    private String fundsTransactionSn;

    private String eventType;
}
