package com.wind.funds.ledger.dto;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerStatus;
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

import java.io.Serializable;
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
public class LedgerDTO implements Serializable {

    @Schema(description = "")
    @NotNull
    private Long id;

    @Schema(description = "")
    @NotNull
    private LocalDateTime gmtCreate;

    @Schema(description = "")
    @NotNull
    private LocalDateTime gmtModified;

    @Schema(description = "账务主体 ID")
    @NotNull
    private String subjectId;

    @Schema(description = "账务主体类型")
    @NotNull
    private String subjectType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "profile 编码快照")
    @NotNull
    private String ledgerProfileCode;

    @Schema(description = "profile 版本快照")
    @NotNull
    private Integer ledgerProfileVersion;

    @Schema(description = "账本编码")
    @NotNull
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "科目分类快照")
    @NotNull
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "正常余额方向")
    @NotNull
    private EntrySide normalBalanceSide;

    @Schema(description = "是否允许负余额")
    @NotNull
    private Boolean allowNegative;

    @Schema(description = "借方累计发生额，单位：分")
    @NotNull
    private Long debitAmount;

    @Schema(description = "贷方累计发生额，单位：分")
    @NotNull
    private Long creditAmount;

    @Schema(description = "账本状态，例如 ACTIVE")
    @NotNull
    private LedgerStatus status;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "结算策略")
    @NotNull
    private String settlementPolicy;

    @Schema(description = "结算日切时间")
    @NotNull
    private LocalTime cutOffTime;

    @Schema(description = "周期类型")
    @NotNull
    private AccountBalancePeriodType periodType;

    @Schema(description = "周期标识，例如：\n年 2026\n月 2026-04\n周 2026-W16")
    @NotNull
    @Size(min = 2, max = 20)
    private String periodId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "虚拟字段：按账户正常余额方向计算出的净额余额，单位：分")
    public Long getNormalBalance() {
        if (debitAmount == null || creditAmount == null || normalBalanceSide == null) {
            return null;
        }
        long rawBalance = debitAmount - creditAmount;
        return normalBalanceSide == EntrySide.DEBIT ? rawBalance : -rawBalance;
    }

}
