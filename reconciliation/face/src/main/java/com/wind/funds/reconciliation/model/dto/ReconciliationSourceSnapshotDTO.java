package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.request.NormalizedComparisonFactInput;
import com.wind.funds.reconciliation.model.value.SnapshotCoverage;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对账来源快照 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationSourceSnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6635871815784801449L;

    @Schema(description = "来源快照流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "来源角色")
    private ReconciliationSourceRole sourceRole;

    @Schema(description = "逻辑来源命名空间")
    private String sourceNamespace;

    @Schema(description = "不可变来源快照身份")
    private StableIdentity snapshotIdentity;

    @Schema(description = "来源快照版本")
    private String snapshotVersion;

    @Schema(description = "来源快照覆盖范围")
    private SnapshotCoverage coverage;

    @Schema(description = "来源成员集合 SHA-256")
    private String sourceDigest;

    @Schema(description = "归一化比较事实")
    private List<NormalizedComparisonFactInput> facts;

    @Schema(description = "来源事实语义 SHA-256")
    private String semanticDigest;

    @Schema(description = "来源证据集合 SHA-256")
    private String evidenceBundleDigest;

    @Schema(description = "来源证据引用")
    private List<String> evidenceRefs;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
