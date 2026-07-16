package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付工具能力准入请求。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ResolvePaymentInstrumentCapabilityRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "业务动作")
    @NotNull
    private PaymentInstrumentAction action;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "期望绑定角色；不需要解析绑定快照时可不传")
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "期望绑定版本，用于防止换绑后继续使用旧快照；传入时必须同时指定绑定角色")
    private Integer expectedBindingVersion;
}
