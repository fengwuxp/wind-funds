package com.wind.funds.reconciliation.model.request;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资金追偿单创建请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "资金追偿单创建请求")
@Data
@Accessors(chain = true)
public class CreateRecoveryOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2089266100494722056L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "追偿来源类型")
    @NotBlank
    private String sourceType;

    @Schema(description = "追偿来源流水号")
    @NotBlank
    private String sourceSn;

    @Schema(description = "责任主体类型")
    @NotBlank
    private String responsibleSubjectType;

    @Schema(description = "责任主体标识")
    @NotBlank
    private String responsibleSubjectId;

    @Schema(description = "应追偿金额，最小货币单位")
    @NotNull
    private Long expectedAmount;

    @Schema(description = "追偿币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "追偿来源稳定摘要")
    @NotBlank
    private String sourceDigest;

    @Schema(description = "追偿审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "支持追偿的证据引用")
    @NotBlank
    private String evidenceRef;
}
