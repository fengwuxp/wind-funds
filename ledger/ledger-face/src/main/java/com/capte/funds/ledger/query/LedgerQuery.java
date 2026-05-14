package com.capte.funds.ledger.query;

import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
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
public class LedgerQuery {

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "profile 编码快照")
    private String ledgerProfileCode;

    @Schema(description = "profile 版本快照")
    private Integer ledgerProfileVersion;

    @Schema(description = "账本编码")
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "科目分类快照")
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "正常余额方向")
    private EntrySide normalBalanceSide;

    @Schema(description = "是否允许负余额")
    private Boolean allowNegative;

    @Schema(description = "借方累计发生额，单位：分")
    private Long debitAmount;

    @Schema(description = "贷方累计发生额，单位：分")
    private Long creditAmount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "结算策略")
    private String settlementPolicy;

    @Schema(description = "结算日切时间")
    private LocalTime cutOffTime;

    @Schema(description = "周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "周期标识，例如：\n年 2026\n月 2026-04\n周 2026-W16")
    private String periodId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "查询到最小 gmtCreate")
    private LocalDateTime gmtCreateMin;

    @Schema(description = "查询到最大 gmtCreate")
    private LocalDateTime gmtCreateMax;

    @Schema(description = "查询到最小 gmtModified")
    private LocalDateTime gmtModifiedMin;

    @Schema(description = "查询到最大 gmtModified")
    private LocalDateTime gmtModifiedMax;

}
