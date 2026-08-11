package com.wind.funds.transaction.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算锁定资金释放结果。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算锁定资金释放结果")
@Data
@Accessors(chain = true)
public class FundsSettlementReleaseResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1806951350761210690L;

    @Schema(description = "结算释放资金交易流水号")
    private String releaseFundsTransactionSn;

    @Schema(description = "承接释放金额的冻结单流水号")
    private String releaseFreezeOrderSn;
}
