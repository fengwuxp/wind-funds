package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
 * <p>受控的内部对账调用方提供已完成归一和匹配的逐笔结果；资金底座生成稳定流水号、状态、计数与摘要并追加保存。
 * 本请求不证明来源内容和覆盖范围本身可信，不能向普通业务调用方开放。</p>
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

    @Schema(description = "本次结果适用的准入对象类型")
    @NotNull
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "本次结果适用的准入对象流水号")
    @NotBlank
    private String gateObjectSn;

    @Schema(description = "匹配或对账规则版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "归一化内部事实集合 SHA-256")
    @NotBlank
    private String internalSourceDigest;

    @Schema(description = "归一化外部来源事实集合 SHA-256")
    @NotBlank
    private String externalSourceDigest;

    @Schema(description = "逐笔匹配结果")
    @NotEmpty
    private List<ReconciliationMatchResultItem> matchResults;

    @Schema(description = "来源文件、报表或匹配报告的稳定证据引用")
    @NotEmpty
    private List<String> evidenceRefs;
}
