package com.capte.funds.wallet.model.request;

import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 变更支付工具绑定请求。
 *
 * @author Codex
 * @date 2026-05-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ChangePaymentInstrumentBindingRequest {

    @Schema(description = "绑定号")
    @NotBlank
    private String bindingSn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;

    @Schema(description = "操作者")
    @NotBlank
    private String operatorId;

    @Schema(description = "变更原因")
    @NotBlank
    private String changeReason;

    @Schema(description = "请求号")
    private String requestSn;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveAt;
}
