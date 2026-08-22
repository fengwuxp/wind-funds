package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.value.SnapshotCoverage;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
 * 记录对账来源快照请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordReconciliationSourceSnapshotRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 7037971912338464708L;

    /**
     * 单次原子冻结允许提交的最大来源成员数。
     */
    public static final int MAX_SOURCE_ITEM_COUNT = 1000;

    public static final int MAX_EVIDENCE_REF_COUNT = 100;

    public static final int MAX_EVIDENCE_REF_LENGTH = 256;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账批次流水号")
    @NotBlank
    private String reconciliationBatchSn;

    @Schema(description = "来源角色")
    @NotNull
    private ReconciliationSourceRole sourceRole;

    @Schema(description = "逻辑来源命名空间")
    @NotBlank
    private String sourceNamespace;

    @Schema(description = "不可变来源快照身份")
    @Valid
    @NotNull
    private StableIdentity snapshotIdentity;

    @Schema(description = "来源快照版本")
    @NotBlank
    private String snapshotVersion;

    @Schema(description = "来源快照覆盖范围")
    @Valid
    @NotNull
    private SnapshotCoverage coverage;

    @Schema(description = "不可变来源事实及其内容摘要；允许一侧为空集合，但两侧不能同时为空")
    @Valid
    @NotNull
    @Size(max = MAX_SOURCE_ITEM_COUNT)
    private List<NormalizedComparisonFactInput> facts;

    @Schema(description = "来源文件、报表或采集结果的稳定证据引用")
    @NotEmpty
    @Size(max = MAX_EVIDENCE_REF_COUNT)
    private List<@NotBlank @Size(max = MAX_EVIDENCE_REF_LENGTH) String> evidenceRefs;
}
