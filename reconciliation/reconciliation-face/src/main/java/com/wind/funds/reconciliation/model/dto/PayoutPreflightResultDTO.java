package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightFactStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightOperationStatus;
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
 * <p>职责：返回出款是否可继续提交、阻断原因、外部规则核验状态和审计证据引用。</p>
 *
 * <p>边界：结果只代表准入检查结论，不代表已经创建出款单、调用通道或写入账务事实。</p>
 */
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

    @Schema(description = "事实状态")
    private PayoutPreflightFactStatus factStatus;

    @Schema(description = "展示状态")
    private PayoutPreflightDisplayStatus displayStatus;

    @Schema(description = "操作状态")
    private PayoutPreflightOperationStatus operationStatus;

    @Schema(description = "外部规则核验状态")
    private ExternalRuleVerificationStatus externalRuleVerificationStatus;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    @Schema(description = "检查人")
    private String checkedBy;

    @Schema(description = "准入结果过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "证据引用列表")
    private List<String> evidenceRefs;
}
