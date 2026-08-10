package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentBindingStatus;
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

    @Schema(description = "绑定生命周期状态")
    private PaymentInstrumentBindingStatus status;

    @Schema(description = "绑定候选生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "绑定候选失效时间")
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

    @Schema(description = "本次变更事实生效时间，不支持未来预约")
    private LocalDateTime effectiveAt;
}
