package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 终止无效对账批次请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AbortReconciliationBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2364467419391484701L;

    public static final int MAX_REASON_LENGTH = 512;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账批次流水号")
    @NotBlank
    private String reconciliationBatchSn;

    @Schema(description = "终止原因；说明批次证据为何不可继续使用")
    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;
}
