package com.wind.funds.ledger.query;


import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 账户账本条目
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerEntryQuery {

    @Schema(description = "账目分录流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账目编码（会计科目编码）\n例如：CASH（现金）、AVAILABLE （余额）、AUTHORIZATION（授权）")
    private LedgerSubjectCode ledgerSubjectCode;

    @Schema(description = "账目类型")
    private LedgerSubjectCategory ledgerSubjectCategory;

    @Schema(description = "账本交易流水号")
    private String ledgerTransactionSn;

    @Schema(description = "账本分录类型（借贷）")
    private EntrySide entryType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务 sn（订单号、业务交易流水）")
    private String businessSn;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "原始金额币种")
    private String originalCurrency;

    @Schema(description = "结算状态")
    private String settlementStatus;

    @Schema(description = "结算周期（账期）")
    private String settlementPeriod;

    @Schema(description = "查询到最小结算完成时间")
    private LocalDateTime settlementCompletedTimeMin;

    @Schema(description = "查询到最大结算完成时间")
    private LocalDateTime settlementCompletedTimeMax;

    @Schema(description = "对账状态")
    private String reconcileStatus;

    @Schema(description = "对账批次（示例：2026040900015）")
    private String reconciliationBatch;

    @Schema(description = "查询到最小对账完成时间")
    private LocalDateTime reconciliationCompletedTimeMin;

    @Schema(description = "查询到最大对账完成时间")
    private LocalDateTime reconciliationCompletedTimeMax;

    @Schema(description = "查询到最小 gmtCreate")
    private LocalDateTime gmtCreateMin;

    @Schema(description = "查询到最大 gmtCreate")
    private LocalDateTime gmtCreateMax;

    @Schema(description = "查询到最小 gmtModified")
    private LocalDateTime gmtModifiedMin;

    @Schema(description = "查询到最大 gmtModified")
    private LocalDateTime gmtModifiedMax;

}
