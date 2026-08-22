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
 * 记录对账运行结果请求。
 *
 * <p>调用方只请求执行指定批次。逐笔结果、运行结果和摘要全部由提供方从已冻结来源事实计算。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordReconciliationRunResultRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1016856231213613720L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账批次流水号；重跑必须使用新的批次流水号")
    @NotBlank
    private String reconciliationBatchSn;

}
