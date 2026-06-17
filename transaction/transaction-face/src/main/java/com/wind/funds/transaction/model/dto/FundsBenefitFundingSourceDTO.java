package com.wind.funds.transaction.model.dto;

import com.wind.funds.transaction.enums.FundsBenefitFundingSourceType;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 权益让利来源引用。
 *
 * <p>用于追踪本次让利来自哪类权益来源、哪条规则，以及该来源贡献的金额。
 * 它不是资金账户、支付工具或账务主体。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsBenefitFundingSourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6687161674591598648L;

    @Schema(description = "权益让利来源类型，例如 COUPON、VOUCHER、PAYMENT_DISCOUNT、MERCHANT_DISCOUNT")
    private FundsBenefitFundingSourceType sourceType;

    @Schema(description = "权益来源标识，例如券实例 ID、代金券 ID、活动补贴单 ID 或外部决策 ID")
    private String sourceId;

    @Schema(description = "权益规则 ID")
    private String ruleId;

    @Schema(description = "权益规则版本")
    private String ruleVersion;

    @Schema(description = "该权益来源贡献的让利金额")
    private Money amount;
}
