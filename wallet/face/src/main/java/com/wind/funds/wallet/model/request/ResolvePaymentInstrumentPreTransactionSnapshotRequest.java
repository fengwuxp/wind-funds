package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
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
 * 支付工具预交易快照解析请求。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ResolvePaymentInstrumentPreTransactionSnapshotRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    @NotNull
    private PaymentInstrumentAction action;

    @Schema(description = "交易金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "期望支付工具绑定角色")
    @NotNull
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "期望支付工具绑定版本，用于防止换绑后继续使用旧快照")
    private Integer expectedBindingVersion;

    @Schema(description = "资金责任关系类型")
    @NotNull
    private SpendSubjectFundingRelationType relationType;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;
}
