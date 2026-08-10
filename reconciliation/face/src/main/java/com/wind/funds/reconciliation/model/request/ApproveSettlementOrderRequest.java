package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算单审批请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算单审批请求")
@Data
@Accessors(chain = true)
public class ApproveSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3920531559624810509L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "结算单流水号")
    @NotBlank
    private String settlementOrderSn;

    @Schema(description = "结算审批稳定引用")
    @NotBlank
    private String settlementApprovalRef;
}
