package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
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
 *
 * @author wuxp
 * @since 2026-07-21
 */
@Schema(description = "可清分明细准入结果")
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

    @Schema(description = "来源 CLEARING 入账金额，最小货币单位")
    private Long amount;

    @Schema(description = "清分前已退款金额，最小货币单位")
    private Long refundAmount;

    @Schema(description = "业务线")
    private String businessLine;

    @Schema(description = "清分周期")
    private String splitPeriod;

    @Schema(description = "清分规则编码")
    private String splitRuleCode;

    @Schema(description = "清分规则版本")
    private String splitRuleVersion;

    @Schema(description = "可清分准入结果")
    private ClearingSplittableAdmissionResult admissionResult;

    @Schema(description = "排除原因；可清分时为空")
    private ClearingSplittableExclusionReason exclusionReason;

    @Schema(description = "清分前对账门禁决策结果")
    private ReconciliationGateDecisionResult reconciliationDecisionResult;

    @Schema(description = "清分前消费的对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "清分前消费的对账运行结果 SHA-256")
    private String reconciliationResultDigest;

    @Schema(description = "清分前对账证据引用")
    private List<String> reconciliationEvidenceRefs;

    @Schema(description = "来源 RouteSnapshot SHA-256")
    private String routeSnapshotDigest;

    @Schema(description = "来源事实与规则快照摘要")
    private String sourceDigest;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
