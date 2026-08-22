package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.value.GateRequirementRef;
import com.wind.funds.reconciliation.model.value.GateStageRef;
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
 *
 * @author wuxp
 * @since 2026-06-18
 */
@Schema(description = "对账准入决策")
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

    @Schema(description = "准入决策结果")
    private ReconciliationGateDecisionResult decisionResult;

    @Schema(description = "精确阶段动作")
    private GateStageRef stageRef;

    @Schema(description = "当前门禁要求")
    private GateRequirementRef requirementRef;

    @Schema(description = "每个必需对账对的决策")
    private List<ReconciliationGatePairDecisionDTO> pairDecisions;

    @Schema(description = "规范化决策 SHA-256")
    private String decisionDigest;

    @Schema(description = "准入证据引用列表")
    private List<String> evidenceRefs;

    @Schema(description = "准入解释摘要")
    private String explanation;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    @Schema(description = "检查人")
    private String checkedBy;
}
