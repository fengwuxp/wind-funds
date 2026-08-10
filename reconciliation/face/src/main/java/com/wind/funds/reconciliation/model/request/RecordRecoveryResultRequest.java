package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资金追偿结果记录请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "资金追偿结果记录请求")
@Data
@Accessors(chain = true)
public class RecordRecoveryResultRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5067142681553565060L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "追偿单流水号")
    @NotBlank
    private String recoveryOrderSn;

    @Schema(description = "本次追偿资金交易流水号")
    @NotBlank
    private String fundsTransactionSn;

    @Schema(description = "本次结果记录幂等键")
    @NotBlank
    private String idempotencyKey;

    @Schema(description = "追偿审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "追偿结果证据引用")
    @NotBlank
    private String evidenceRef;
}
