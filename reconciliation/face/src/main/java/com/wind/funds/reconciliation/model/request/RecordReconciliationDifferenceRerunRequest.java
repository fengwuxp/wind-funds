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
 * <p>职责：将差错处理结果绑定到已经固化的重新对账运行结果。</p>
 *
 * <p>边界：调用方只提交运行结果引用，是否对平、批次、规则版本、摘要和证据均由服务读取真实运行结果派生；
 * 重跑结果不能覆盖旧差错、旧审批或旧处理动作，关闭差错前必须已有处理动作回链。</p>
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

    @Schema(description = "已固化的重新对账运行结果流水号")
    @NotBlank
    private String reconciliationRunResultSn;
}
