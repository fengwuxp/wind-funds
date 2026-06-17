package com.wind.funds.wallet.model.dto;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.spec.ledger.LedgerProfileItemSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;

/**
 * LedgerProfile 科目明细。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerProfileItemDTO implements LedgerProfileItemSpec, Serializable {

    @Serial
    private static final long serialVersionUID = -3691606869013497836L;

    @Schema(description = "账本科目编码")
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "科目分类")
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "正常余额方向")
    private EntrySide normalBalanceSide;

    @Schema(description = "是否允许负余额")
    private Boolean allowNegative;

    @Schema(description = "是否必建账本")
    private Boolean required;

    @Schema(description = "周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "结算策略表达式")
    private String settlementPolicy;

    @Schema(description = "日切时间")
    private LocalTime cutOffTime;

    @Schema(description = "描述")
    private String description;

}
