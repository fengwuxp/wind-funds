package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 规则评估请求。
 *
 * @author Codex
 * @date 2026-06-30
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class EvaluateSpendRuleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3138970972552062913L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "支付工具动作")
    @NotNull
    private PaymentInstrumentAction action;

    @Schema(description = "调用方已归一后的本次评估金额，最小货币单位；"
            + "卡授权场景不得让本服务从 requested amount、退款或撤销事实反推授权累计口径")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "商户类别码 MCC，商户类别规则评估时必填")
    private String merchantCategoryCode;

    @Schema(description = "商户标识 MID，商户标识规则评估时必填")
    private String merchantId;

    @Schema(description = "商户国家或地区代码，商户国家规则评估时必填，例如 US")
    private String merchantCountryCode;

    @Schema(description = "卡数据输入能力，卡数据输入能力规则评估时必填，例如 EMV_CHIP、MAGNETIC_STRIPE")
    private String cardDataInputCapability;

    @Schema(description = "卡交易处理类型，卡交易处理类型规则评估时必填，例如 CASH、PIN_CHANGE")
    private String cardTransactionProcessingType;

    @Schema(description = "是否已提供 CVV 事实，CVV 必填规则评估时必填；不得传入 CVV 原文")
    private Boolean cvvProvided;

    @Schema(description = "PAN 录入方式，PAN 录入方式规则评估时必填，例如 MANUAL、CONTACTLESS")
    private String panEntryMode;

    @Schema(description = "POS 类别，POS 类别规则评估时必填，例如 AUTOMATED_TELLER_MACHINE、AUTOMATED_FUEL_DISPENSER")
    private String pointOfServiceCategory;

    @Schema(description = "AVS 邮编校验结果，邮编校验规则评估时必填，例如 MATCH、NO_MATCH；不得传入邮编原文")
    private String postalCodeVerificationResult;

    @Schema(description = "授权发生时间，时间窗口或滚动窗口次数规则评估时必填；"
            + "由调用方按业务或规则时区归一化后传入")
    private LocalDateTime authorizationTime;

    @Schema(description = "控制范围标识，周期金额、周期次数或滚动窗口次数规则评估时必填")
    private String controlScopeId;

    @Schema(description = "控制周期标识，例如 2026-07；周期金额或周期次数规则评估时必填，"
            + "滚动窗口次数不使用")
    private String periodId;

    @Schema(description = "控制目标资金账户或信用账户标识，"
            + "周期金额、周期次数或滚动窗口次数规则评估时可传入账户级范围")
    private FundsAccountId targetAccountId;
}
