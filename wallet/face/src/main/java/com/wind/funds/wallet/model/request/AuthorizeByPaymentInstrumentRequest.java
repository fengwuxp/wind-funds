package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.transaction.core.enums.InternationalRegionCode;
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
 * 支付工具授权请求。
 *
 * <p>该请求是 wallet application 层的外部业务入口模型，不替代 transaction 层账户主体型授权请求。</p>
 *
 * @author Codex
 * @date 2026-06-18
 */
@Schema(description = "支付工具授权请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AuthorizeByPaymentInstrumentRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "授权金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "授权币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "业务流水号，通常为外部授权流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "授权是否通过")
    @NotNull
    private Boolean approved;

    @Schema(description = "期望支付工具绑定版本，用于防止换绑后继续使用旧快照")
    private Integer expectedBindingVersion;

    @Schema(description = "Spend Rule 标识，可选回显字段，不作为授权准入依据")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本，可选回显字段，不作为授权准入依据")
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 挂载流水号，可选回显字段；适用挂载由 wallet 解析")
    private String spendRuleBindingSn;

    @Schema(description = "Spend Rule 控制范围类型，可选回显字段；适用范围由 wallet 解析")
    private SpendRuleScopeType spendRuleScopeType;

    @Schema(description = "Spend Rule 控制范围标识，可选回显字段；适用范围由 wallet 解析")
    private String spendRuleScopeId;

    @Schema(description = "Spend Rule 决策引用；存在适用挂载时必须可被 wallet 回读并验真")
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果，可选回显字段，不作为授权准入依据")
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 最终决策摘要，可选回显字段，不作为授权准入依据")
    private String spendDecisionDigest;

    @Schema(description = "控制范围标识；租户存在有效 SPEND_CONTROL_SCOPE 挂载时必须由可信上层提供")
    private String controlScopeId;

    @Schema(description = "控制周期标识；需要预算控制预留时必须由可信上层提供")
    private String periodId;

    @Schema(description = "Spend Rule 决策拒绝原因，可选回显字段，不作为授权准入依据")
    private String spendDecisionRejectReason;

    @Schema(description = "交易授权时间")
    private LocalDateTime authorizedTime;

    @Schema(description = "交易国家或地区")
    private InternationalRegionCode transactionCountry;

    @Schema(description = "授权拒绝原因，仅 approved=false 时使用")
    private String declineReason;

    @Schema(description = "交易描述")
    private String description;
}
