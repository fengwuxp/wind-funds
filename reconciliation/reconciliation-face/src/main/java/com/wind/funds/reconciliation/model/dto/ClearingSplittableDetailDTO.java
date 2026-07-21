package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ClearingSplittableDetailStatus;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
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
 * 可清分明细准入结果。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingSplittableDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7147732213556560179L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "可清分明细流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "来源资金交易流水号")
    private String fundsTransactionSn;

    @Schema(description = "来源资金交易明细流水号")
    private String fundsTransactionDetailSn;

    @Schema(description = "来源账本交易流水号")
    private String ledgerTransactionSn;

    @Schema(description = "来源记账计划流水号")
    private String postingPlanSn;

    @Schema(description = "来源账本分录流水号")
    private String ledgerEntrySn;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "本金金额，最小货币单位")
    private Long principalAmount;

    @Schema(description = "清分前已退款金额，最小货币单位")
    private Long refundAmount;

    @Schema(description = "清分周期")
    private String clearingPeriod;

    @Schema(description = "清分规则编码")
    private String ruleCode;

    @Schema(description = "清分规则版本")
    private String ruleVersion;

    @Schema(description = "可清分准入状态")
    private ClearingSplittableDetailStatus status;

    @Schema(description = "排除原因；可清分时为空")
    private ClearingSplittableExclusionReason exclusionReason;

    @Schema(description = "清分前对账门禁结论")
    private ReconciliationGateDecisionStatus reconciliationDecisionStatus;

    @Schema(description = "清分前消费的对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "清分前消费的对账运行结果 SHA-256")
    private String reconciliationResultDigest;

    @Schema(description = "清分前对账证据引用")
    private List<String> reconciliationEvidenceRefs;

    @Schema(description = "来源事实与规则快照摘要")
    private String sourceDigest;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
