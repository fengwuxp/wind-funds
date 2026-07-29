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
 * 将待复核清算批次退回草稿请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReturnClearingBatchToDraftRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 808577339732033044L;

    public static final int MAX_REASON_LENGTH = 512;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算批次流水号")
    @NotBlank
    private String clearingBatchSn;

    @Schema(description = "退回原因")
    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;
}
