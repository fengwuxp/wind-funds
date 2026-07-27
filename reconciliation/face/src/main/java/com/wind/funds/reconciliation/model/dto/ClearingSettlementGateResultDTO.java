package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
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
 * 清算 / 结算对账准入消费结果。
 *
 * <p>职责：返回清算或结算对象的对账 gate 时点状态，以及阻断差错、证据引用和解释摘要。</p>
 *
 * <p>边界：结果不是最终放行凭证，不代表清算候选、清算批次、结算单或账务事实已经产生。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingSettlementGateResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5698978715418952457L;

    /**
     * 本次时点检查是否通过。
     */
    @Schema(description = "本次时点检查是否通过；不能作为最终放行凭证")
    private boolean passed;

    /**
     * 准入决策状态。
     */
    @Schema(description = "准入决策状态")
    private ReconciliationGateDecisionStatus decisionStatus;

    /**
     * 准入消费对象类型。
     */
    @Schema(description = "准入消费对象类型")
    private ReconciliationGateObjectType gateObjectType;

    /**
     * 准入消费对象流水号。
     */
    @Schema(description = "准入消费对象流水号")
    private String gateObjectSn;

    @Schema(description = "本次准入消费的对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "对账运行结果 SHA-256")
    private String reconciliationResultDigest;

    /**
     * 阻断差错列表。
     */
    @Schema(description = "阻断差错列表")
    private List<ReconciliationGateBlockingDifferenceDTO> blockingDifferences;

    @Schema(description = "已处理且经当前批次重跑对平的历史差错数量")
    private int resolvedDifferenceCount;

    /**
     * 准入证据引用列表。
     */
    @Schema(description = "准入证据引用列表")
    private List<String> evidenceRefs;

    /**
     * 准入解释摘要。
     */
    @Schema(description = "准入解释摘要")
    private String explanation;

    /**
     * 检查时间。
     */
    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    /**
     * 检查人。
     */
    @Schema(description = "检查人")
    private String checkedBy;
}
