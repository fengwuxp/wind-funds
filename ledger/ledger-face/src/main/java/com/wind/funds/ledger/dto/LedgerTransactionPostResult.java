package com.wind.funds.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账本交易入账结果。
 *
 * @author Codex
 * @date 2026-05-15
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerTransactionPostResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 5079292095101210626L;

    @Schema(description = "账本交易 ID")
    private Long ledgerTransactionId;

    @Schema(description = "本次调用是否新入账账本交易")
    private boolean newlyPosted;
}
