package com.wind.funds.wallet.model.request;

import com.wind.funds.route.enums.FundsSubjectType;
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
 * 资金责任解析请求。
 *
 * @author Codex
 * @date 2026-06-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ResolveFundingResponsibilityRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支出控制主体 ID")
    @NotBlank
    private String spendSubjectId;

    @Schema(description = "支出控制主体类型")
    @NotNull
    private FundsSubjectType spendSubjectType;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "关系类型")
    @NotNull
    private SpendSubjectFundingRelationType relationType;
}
