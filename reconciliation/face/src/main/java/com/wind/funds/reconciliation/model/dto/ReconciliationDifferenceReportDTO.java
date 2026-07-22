package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceReportCompleteness;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
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
 * 对账差异报告 DTO。
 *
 * <p>职责：向运营、财务、风控、研发和测试提供单笔对账差错的只读解释视图。</p>
 *
 * <p>边界：报告字段都是事实引用、状态摘要或解释文本，不代表任何新资金事实已经发生。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationDifferenceReportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 775234898038407429L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "对账差错流水号")
    private String differenceSn;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "对账来源记录流水号")
    private String sourceRecordSn;

    @Schema(description = "对账来源质量")
    private ReconciliationSourceQuality sourceQuality;

    @Schema(description = "对账匹配强度")
    private ReconciliationMatchStrength matchStrength;

    @Schema(description = "对账差错类型")
    private ReconciliationDifferenceType differenceType;

    @Schema(description = "对账差错严重等级")
    private ReconciliationDifferenceSeverity severity;

    @Schema(description = "对账差错状态")
    private ReconciliationDifferenceStatus status;

    @Schema(description = "差异币种")
    private CurrencyIsoCode currency;

    @Schema(description = "差异金额，最小货币单位")
    private Long differenceAmount;

    @Schema(description = "责任方引用")
    private String responsiblePartyRef;

    @Schema(description = "阻断范围，例如 CLEARING、SETTLEMENT、PAYOUT")
    private String blockingScope;

    @Schema(description = "阻断对象类型；为空表示历史类型级阻断")
    private ReconciliationGateObjectType blockingObjectType;

    @Schema(description = "阻断对象流水号；为空表示历史类型级阻断")
    private String blockingObjectSn;

    @Schema(description = "匹配或对账规则版本")
    private String ruleVersion;

    @Schema(description = "来源证据引用")
    private String evidenceRef;

    @Schema(description = "差错处理动作类型")
    private ReconciliationDifferenceActionType actionType;

    @Schema(description = "关联处理动作或调账单号")
    private String adjustmentSn;

    @Schema(description = "处理动作幂等键")
    private String adjustmentIdempotencyKey;

    @Schema(description = "被处理的原始事实引用")
    private String originalFactRef;

    @Schema(description = "关联资金交易流水号")
    private String adjustmentTransactionSn;

    @Schema(description = "调账审批引用")
    private String adjustmentApprovalRef;

    @Schema(description = "调账证据引用")
    private String adjustmentEvidenceRef;

    @Schema(description = "处理原因")
    private String adjustmentReason;

    @Schema(description = "最后一次重跑对账运行结果流水号")
    private String lastRerunSn;

    @Schema(description = "最后一次重跑批次流水号")
    private String lastRerunBatchSn;

    @Schema(description = "最后一次重跑规则版本")
    private String lastRerunRuleVersion;

    @Schema(description = "最后一次重跑是否对平")
    private Boolean lastRerunBalanced;

    @Schema(description = "最后一次重跑运行结果证据引用，使用 ReconciliationRunResult.sn")
    private String lastRerunEvidenceRef;

    @Schema(description = "最后一次重跑结果摘要")
    private String lastRerunResultDigest;

    @Schema(description = "重跑次数")
    private Integer rerunCount;

    @Schema(description = "准入 gate 决策状态")
    private ReconciliationGateDecisionStatus gateDecisionStatus;

    @Schema(description = "准入 gate 解释摘要")
    private String gateExplanation;

    @Schema(description = "证据引用列表，只包含安全引用，不包含外部账户、卡号或上下文原文")
    private List<String> evidenceRefs;

    @Schema(description = "报告完整性")
    private ReconciliationDifferenceReportCompleteness completeness;

    @Schema(description = "安全过滤提示")
    private List<String> securityWarnings;

    @Schema(description = "报告解释摘要")
    private String explanation;

    @Schema(description = "查询时间")
    private LocalDateTime checkedAt;

    @Schema(description = "查询人")
    private String checkedBy;
}
