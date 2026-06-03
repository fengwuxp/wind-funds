package com.capte.funds.wallet.model.request;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
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
 * 创建支付工具绑定请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreatePaymentInstrumentBindingRequest {

    @Schema(description = "绑定号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "绑定角色")
    @NotNull
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "内部主体 ID")
    @NotBlank
    private String subjectId;

    @Schema(description = "内部主体类型")
    @NotNull
    private FundsSubjectType subjectType;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "路由优先级")
    private Integer priority;

    @Schema(description = "是否默认绑定")
    private Boolean defaultBinding;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "操作者")
    private String operatorId;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "请求号")
    private String requestSn;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
