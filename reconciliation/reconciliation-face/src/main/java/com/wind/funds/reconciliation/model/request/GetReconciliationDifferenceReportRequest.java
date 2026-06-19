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
 * 查询对账差异报告请求。
 *
 * <p>职责：承载单笔差错报告查询条件和报告视图开关。</p>
 *
 * <p>边界：请求只用于只读解释，不触发重跑、准入消费、补事实、调账、清算、结算或出款动作。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class GetReconciliationDifferenceReportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -6688259583927260722L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账差错流水号")
    @NotBlank
    private String differenceSn;

    @Schema(description = "是否包含准入 gate 决策摘要，默认包含")
    private Boolean includeGateDecision;

    @Schema(description = "是否包含证据引用列表，默认包含")
    private Boolean includeEvidenceRefs;
}
