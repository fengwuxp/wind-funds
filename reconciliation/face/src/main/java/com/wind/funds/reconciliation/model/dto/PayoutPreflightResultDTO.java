package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ExternalRuleVerificationResult;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightDecisionResult;
import com.wind.funds.reconciliation.enums.PayoutPreflightAction;
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
 * 出款前准入检查结果 DTO。
 *
 * <p>职责：返回当前证据预检是否通过、阻断原因、外部规则核验状态和审计证据引用。</p>
 *
 * <p>边界：结果不是出款提交授权；调用方必须在真实提交命令中重新读取权威事实并执行完整门禁。</p>
 *
 * @author wuxp
 * @since 2026-05-23
 */
@Schema(description = "出款前准入检查结果")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PayoutPreflightResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6945420286162805147L;

    @Schema(description = "是否准入通过")
    private boolean passed;

    @Schema(description = "阻断等级")
    private PayoutPreflightBlockingLevel blockingLevel;

    @Schema(description = "阻断原因列表")
    private List<PayoutPreflightBlockingReasonDTO> blockingReasons;

    @Schema(description = "是否需要人工复核")
    private boolean manualReviewRequired;

    @Schema(description = "准入决策结果")
    private PayoutPreflightDecisionResult decisionResult;

    @Schema(description = "展示状态")
    private PayoutPreflightDisplayStatus displayStatus;

    @Schema(description = "建议操作")
    private PayoutPreflightAction action;

    @Schema(description = "外部规则核验结果")
    private ExternalRuleVerificationResult externalRuleVerificationResult;

    @Schema(description = "本次只读预检解释的精确阶段引用")
    private GateStageRef stageRef;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    @Schema(description = "检查人")
    private String checkedBy;

    @Schema(description = "准入结果过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "证据引用列表")
    private List<String> evidenceRefs;
}
