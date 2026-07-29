package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ClearingCandidateStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 清算候选事实快照。
 *
 * <p>候选是清分结果快照的可清算资格投影，不是清算交易、账本分录或余额。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingCandidateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5928842176725947896L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "清算候选流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "来源清分结果快照流水号")
    private String splitResultSn;

    @Schema(description = "来源清分批次流水号")
    private String splitBatchSn;

    @Schema(description = "来源可清分明细流水号")
    private String splittableDetailSn;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务线")
    private String businessLine;

    @Schema(description = "清算周期，不是账本周期")
    private String clearingPeriod;

    @Schema(description = "当前候选可清算金额，最小货币单位")
    private Long amount;

    @Schema(description = "最早可进入清算批次的时间")
    private LocalDateTime clearingAvailableTime;

    @Schema(description = "资金交易流水号")
    private String fundsTransactionSn;

    @Schema(description = "资金交易明细流水号")
    private String fundsTransactionDetailSn;

    @Schema(description = "账本交易流水号")
    private String ledgerTransactionSn;

    @Schema(description = "记账计划流水号")
    private String postingPlanSn;

    @Schema(description = "账本分录流水号")
    private String ledgerEntrySn;

    @Schema(description = "来源 RouteSnapshot SHA-256")
    private String routeSnapshotDigest;

    @Schema(description = "清算规则编码")
    private String clearingRuleCode;

    @Schema(description = "清算规则版本")
    private String clearingRuleVersion;

    @Schema(description = "对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "对账结果 SHA-256")
    private String reconciliationResultDigest;

    @Schema(description = "对账证据引用")
    private List<String> reconciliationEvidenceRefs;

    @Schema(description = "来源事实摘要")
    private String sourceDigest;

    @Schema(description = "候选摘要")
    private String candidateDigest;

    @Schema(description = "候选状态")
    private ClearingCandidateStatus status;

    @Schema(description = "阻断原因")
    private String blockReason;

    @Schema(description = "排除原因")
    private String exclusionReason;

    @Schema(description = "锁定候选的清算批次流水号")
    private String lockedClearingBatchSn;

    @Schema(description = "状态变更时间")
    private LocalDateTime statusChangedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime modifiedTime;
}
