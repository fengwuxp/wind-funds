package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算单资金锁定请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算单资金锁定请求")
@Data
@Accessors(chain = true)
public class LockSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5578433678370178866L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "结算单流水号")
    @NotBlank
    private String settlementOrderSn;

}
