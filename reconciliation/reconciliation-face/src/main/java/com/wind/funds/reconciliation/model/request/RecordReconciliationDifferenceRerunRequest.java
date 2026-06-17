package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对账差错重跑结果请求。
 *
 * <p>职责：登记差错处理后的重新对账结果，并保留重跑报告摘要。</p>
 *
 * <p>边界：重跑结果不能覆盖旧差错、旧审批或旧处理动作；关闭差错前必须已有处理动作回链。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordReconciliationDifferenceRerunRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 4822900991132861525L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账差错流水号")
    @NotBlank
    private String differenceSn;

    @Schema(description = "重跑流水号，用于幂等")
    @NotBlank
    private String rerunSn;

    @Schema(description = "重跑批次流水号")
    @NotBlank
    private String rerunBatchSn;

    @Schema(description = "重跑使用的规则版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "重跑后是否对平")
    @NotNull
    private Boolean balanced;

    @Schema(description = "重跑证据或报告引用")
    @NotBlank
    private String evidenceRef;

    @Schema(description = "重跑结果摘要")
    @NotBlank
    private String resultDigest;

    @Schema(description = "描述")
    private String description;
}
