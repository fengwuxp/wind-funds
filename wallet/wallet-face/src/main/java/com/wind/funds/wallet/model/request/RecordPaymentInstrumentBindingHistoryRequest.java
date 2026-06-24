package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
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
 * 记录支付工具绑定历史请求。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordPaymentInstrumentBindingHistoryRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "绑定号")
    @NotBlank
    private String bindingSn;

    @Schema(description = "工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "变更类型")
    @NotNull
    private PaymentInstrumentBindingChangeType changeType;

    @Schema(description = "绑定版本")
    @NotNull
    private Integer version;

    @Schema(description = "变更前快照")
    private String beforeSnapshot;

    @Schema(description = "变更后快照")
    @NotBlank
    private String afterSnapshot;

    @Schema(description = "操作者")
    @NotBlank
    private String operatorId;

    @Schema(description = "变更原因")
    @NotBlank
    private String changeReason;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveAt;

    @Schema(description = "请求号")
    private String requestSn;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
