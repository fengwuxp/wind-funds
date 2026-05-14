package com.capte.funds.transaction.model.query;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 资金主体当前余额查询条件。
 *
 * @author Codex
 * @date 2026-05-12
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsSubjectBalanceQuery {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "账务主体引用列表，返回结果顺序与入参顺序一致")
    @NotEmpty
    private List<FundsAccountId> subjectRefs;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "账本科目编码，不传表示返回主体下全部当前账本余额")
    private List<LedgerSubjectCode> ledgerSubjectCodes;

    @Schema(description = "周期类型")
    private AccountBalancePeriodType periodType = AccountBalancePeriodType.LIFETIME;

    @Schema(description = "周期标识，periodType 不为 LIFETIME 时必填")
    private String periodId;
}
