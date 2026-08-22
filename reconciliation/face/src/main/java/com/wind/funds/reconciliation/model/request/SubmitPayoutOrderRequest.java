package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出款单首次提交请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "出款单首次提交请求")
@Data
@Accessors(chain = true)
public class SubmitPayoutOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2716102830871511513L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "出款单流水号")
    @NotBlank
    private String payoutOrderSn;

    @Schema(description = "出款账户稳定引用")
    @NotBlank
    private String payoutAccountRef;

    @Schema(description = "收款端点稳定引用")
    @NotBlank
    private String payeeEndpointRef;

    @Schema(description = "出款通道稳定引用")
    @NotBlank
    private String channelRef;

    @Schema(description = "出款审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "外部规则核验证据")
    @Valid
    @NotNull
    private ExternalRuleVerificationEvidenceDTO externalRuleVerificationEvidence;

}
