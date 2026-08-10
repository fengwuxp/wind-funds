package com.wind.funds.wallet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账本交易事实轻量快照。
 *
 * @author wuxp
 * @since 2026-06-30
 */
@Schema(description = "账本交易事实轻量快照")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class LedgerTransactionFactDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7635923965038380285L;

    @Schema(description = "账本交易流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "资金交易流水号")
    private String fundsTransactionSn;

    @Schema(description = "账本交易事件类型代码")
    private String eventType;
}
