package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出款单创建请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "出款单创建请求")
@Data
@Accessors(chain = true)
public class CreatePayoutOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5551630232543593886L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "来源结算单流水号")
    @NotBlank
    private String settlementOrderSn;
}
