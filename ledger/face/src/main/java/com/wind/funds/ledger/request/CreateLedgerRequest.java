package com.wind.funds.ledger.request;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalTime;

/**
 * 账户账本
 *
 * @author wuxp
 * @since 2026-04-24
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateLedgerRequest {

    @Schema(description = "租户ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "profile 编码快照")
    private String ledgerProfileCode;

    @Schema(description = "profile 版本快照")
    private Integer ledgerProfileVersion;

    @Schema(description = "账本编码")
    @NotNull
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "科目分类快照")
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "正常余额方向")
    private EntrySide normalBalanceSide;

    @Schema(description = "是否允许负余额")
    private Boolean allowNegative;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "结算策略")
    private String settlementPolicy;

    @Schema(description = "结算日切时间")
    private LocalTime cutOffTime;

    @Schema(description = "周期类型")
    @NotNull
    private AccountBalancePeriodType periodType;

    @Schema(description = "周期标识，例如：\n年 2026\n月 2026-04\n周 2026-W16")
    @NotNull
    @Size(min = 2, max = 20)
    private String periodId;

}
