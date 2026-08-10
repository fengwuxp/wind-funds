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
 * 更新支付工具绑定当前态请求。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UpdatePaymentInstrumentBindingRequest {

    @Schema(description = "绑定主键")
    @NotNull
    private Long id;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "绑定号")
    @NotBlank
    private String bindingSn;

    @Schema(description = "期望版本")
    @NotNull
    private Integer expectedVersion;

    @Schema(description = "更新后版本")
    @NotNull
    private Integer nextVersion;

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "绑定生命周期状态")
    private PaymentInstrumentBindingStatus status;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
