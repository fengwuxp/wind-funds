package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
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
 * 对账运行结果 DTO。
 *
 * <p>该对象是一次运行的不可变结果证据，不代表清分、结算、出款或账务事实已经发生。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationRunResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1729187885525786921L;

    @Schema(description = "对账运行结果流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "准入对象类型")
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "准入对象流水号")
    private String gateObjectSn;

    @Schema(description = "对账运行结果状态")
    private ReconciliationRunResultStatus status;

    @Schema(description = "匹配或对账规则版本")
    private String ruleVersion;

    @Schema(description = "归一化内部事实集合 SHA-256")
    private String internalSourceDigest;

    @Schema(description = "归一化外部来源事实集合 SHA-256")
    private String externalSourceDigest;

    @Schema(description = "由内部与外部来源摘要生成的组合 SHA-256")
    private String sourceDigest;

    @Schema(description = "对账运行结果 SHA-256，由资金底座生成")
    private String resultDigest;

    @Schema(description = "参与本次运行的记录总数")
    private Integer totalCount;

    @Schema(description = "成功匹配的记录数")
    private Integer matchedCount;

    @Schema(description = "发现差错的记录数")
    private Integer differenceCount;

    @Schema(description = "证据引用列表")
    private List<String> evidenceRefs;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
