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
 * 替代已完成对账批次请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReplaceReconciliationBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3784943557978232184L;

    public static final int MAX_REASON_LENGTH = 512;

    public static final int MAX_EVIDENCE_REF_LENGTH = 256;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "被替代的已完成对账批次流水号")
    @NotBlank
    @Size(max = 64)
    private String reconciliationBatchSn;

    @Schema(description = "替代批次使用的匹配或对账规则版本")
    @NotBlank
    @Size(max = 64)
    private String ruleVersion;

    @Schema(description = "原批次证据失效并需要替代的原因")
    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;

    @Schema(description = "证明原批次证据失效的安全引用")
    @NotBlank
    @Size(max = MAX_EVIDENCE_REF_LENGTH)
    private String evidenceRef;
}
