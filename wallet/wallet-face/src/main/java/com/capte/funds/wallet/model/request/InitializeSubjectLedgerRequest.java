package com.capte.funds.wallet.model.request;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
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
 * 初始化账务主体账本请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class InitializeSubjectLedgerRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "账务主体 ID")
    @NotBlank
    private String subjectId;

    @Schema(description = "账务主体类型")
    @NotNull
    private FundsSubjectType subjectType;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "ledger profile 编码")
    @NotNull
    private LedgerProfileCode ledgerProfileCode;

    @Schema(description = "账本周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "账本周期标识，periodType 不为 LIFETIME 时必填")
    private String periodId;
}
