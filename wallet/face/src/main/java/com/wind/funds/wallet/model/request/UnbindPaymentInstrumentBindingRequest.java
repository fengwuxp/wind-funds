package com.wind.funds.wallet.model.request;

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
 * 解除支付工具绑定请求。
 *
 * @author Codex
 * @date 2026-07-15
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UnbindPaymentInstrumentBindingRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "绑定号")
    @NotBlank
    private String bindingSn;

    @Schema(description = "操作者")
    @NotBlank
    private String operatorId;

    @Schema(description = "解绑原因")
    @NotBlank
    private String changeReason;

    @Schema(description = "本次解绑事实生效时间，不支持未来预约")
    private LocalDateTime effectiveAt;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
