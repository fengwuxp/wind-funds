package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.enums.ReconciliationSourceType;
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

    @Schema(description = "来源事实类型")
    private ReconciliationSourceType sourceType;

    @Schema(description = "来源成员集合 SHA-256")
    private String sourceDigest;

    @Schema(description = "来源成员数")
    private Integer recordCount;

    @Schema(description = "来源成员稳定引用")
    private List<String> sourceItemRefs;

    @Schema(description = "来源证据引用")
    private List<String> evidenceRefs;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
