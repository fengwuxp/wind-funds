package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
import java.util.List;

/**
 * 记录对账运行结果请求。
 *
 * <p>匹配调用方只提交批次引用和逐笔匹配结论。对账范围、可选准入对象、规则版本、来源摘要与来源证据由服务从
 * 已冻结批次读取，调用方不能自报或覆盖这些事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordReconciliationRunResultRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1016856231213613720L;

    /**
     * 单次原子封版允许提交的最大逐笔匹配结果数。
     */
    public static final int MAX_MATCH_RESULT_COUNT = 2000;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账批次流水号；重跑必须使用新的批次流水号")
    @NotBlank
    private String reconciliationBatchSn;

    @Schema(description = "逐笔匹配结果")
    @Valid
    @NotNull
    @Size(max = MAX_MATCH_RESULT_COUNT)
    private List<ReconciliationMatchResultItem> matchResults;
}
