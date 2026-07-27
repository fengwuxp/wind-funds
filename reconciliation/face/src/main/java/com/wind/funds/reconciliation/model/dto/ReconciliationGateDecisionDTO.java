package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
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
 * 对账准入决策 DTO。
 *
 * <p>职责：向清算、结算、出款和运营返回是否可继续推进、阻断差错、证据引用和解释摘要。</p>
 *
 * <p>边界：结果只代表准入判断，不代表清算、结算、出款或账务事实已经发生。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationGateDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2262330091140544596L;

    @Schema(description = "是否准入通过")
    private boolean passed;

    @Schema(description = "准入决策状态")
    private ReconciliationGateDecisionStatus decisionStatus;

    @Schema(description = "准入消费对象类型")
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "准入消费对象流水号")
    private String gateObjectSn;

    @Schema(description = "本次准入消费的对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "对账运行结果状态")
    private ReconciliationRunResultStatus reconciliationRunResultStatus;

    @Schema(description = "对账运行结果 SHA-256")
    private String reconciliationResultDigest;

    @Schema(description = "阻断差错列表")
    private List<ReconciliationGateBlockingDifferenceDTO> blockingDifferences;

    @Schema(description = "已处理且经当前批次重跑对平的历史差错数量")
    private int resolvedDifferenceCount;

    @Schema(description = "准入证据引用列表")
    private List<String> evidenceRefs;

    @Schema(description = "准入解释摘要")
    private String explanation;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    @Schema(description = "检查人")
    private String checkedBy;
}
