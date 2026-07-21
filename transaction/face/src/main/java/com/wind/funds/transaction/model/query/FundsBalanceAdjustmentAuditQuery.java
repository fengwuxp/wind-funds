package com.wind.funds.transaction.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 余额调账审计查询条件。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsBalanceAdjustmentAuditQuery {

    @Schema(description = "租户 ID，用于隔离审计查询边界")
    private Long tenantId;

    @Schema(description = "业务场景，按业务流水查询时必填")
    private String businessScene;

    @Schema(description = "业务流水，按业务流水查询时必填")
    private String businessSn;

    @Schema(description = "资金交易流水，按资金交易查询时必填")
    private String fundsTransactionSn;
}
