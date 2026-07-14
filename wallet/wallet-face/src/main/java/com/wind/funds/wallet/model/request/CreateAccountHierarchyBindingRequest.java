package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
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
 * 创建账户层级绑定请求。
 *
 * <p>业务语义：表达资金账户或信用账户之间的父子归属关系，用于交易路由生成可审计的账户层级快照。
 * 该请求不创建账户、不初始化账本、不改变余额。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateAccountHierarchyBindingRequest {

    @Schema(description = "绑定号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "子账户标识")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "父账户标识")
    @NotNull
    private FundsAccountId parentAccountId;

    @Schema(description = "根账户标识")
    @NotNull
    private FundsAccountId rootAccountId;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "操作者")
    private String operatorId;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
