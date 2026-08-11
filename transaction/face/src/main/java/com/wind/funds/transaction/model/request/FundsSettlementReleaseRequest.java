package com.wind.funds.transaction.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 结算锁定资金全额释放请求。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算锁定资金全额释放请求")
@Data
@Accessors(chain = true)
public class FundsSettlementReleaseRequest {

    @Schema(description = "原结算锁定资金交易流水号")
    @NotBlank
    private String lockFundsTransactionSn;

    @Schema(description = "归属结算单流水号")
    @NotBlank
    private String settlementOrderSn;
}
