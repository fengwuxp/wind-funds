package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算单取消请求。
 *
 * @author wuxp
 * @since 2026-07-27
 */
@Schema(description = "结算单取消请求")
@Data
@Accessors(chain = true)
public class CancelSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5400604943092953095L;

    public static final int MAX_REASON_LENGTH = 512;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "结算单流水号")
    @NotBlank
    private String settlementOrderSn;

    @Schema(description = "取消原因")
    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;
}
