package com.wind.funds.reconciliation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出款首次提交准入决定。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "出款首次提交准入决定")
@Data
@Accessors(chain = true)
public class PayoutSubmissionAdmissionDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6534735922165169041L;

    @Schema(description = "是否允许首次提交")
    private boolean passed;

    @Schema(description = "准入决定稳定摘要")
    private String decisionDigest;

    @Schema(description = "支持准入决定的稳定证据引用")
    private List<String> evidenceRefs;

    @Schema(description = "准入决定失效时间")
    private LocalDateTime expiresAt;

    @Schema(description = "未通过准入时的阻断原因")
    private String blockingReason;
}
