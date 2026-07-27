package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
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

/**
 * 对账差错 DTO。
 *
 * <p>职责：向清结算、出款、运营和审计侧解释差错来源、阻断状态、处理回链和重跑结果。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationDifferenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2716954475322493472L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "对账差错流水号")
    private String differenceSn;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "对账逐笔匹配结果流水号")
    private String reconciliationMatchResultSn;

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

    @Schema(description = "金额差异的币种；非金额差异为空")
    private CurrencyIsoCode currency;

    @Schema(description = "金额差异，最小货币单位；非金额差异为空")
    private Long differenceAmount;

    @Schema(description = "责任方引用")
    private String responsiblePartyRef;

    @Schema(description = "阻断对象类型，例如 CLEARING、SETTLEMENT、PAYOUT")
    private ReconciliationGateObjectType blockingObjectType;

    @Schema(description = "阻断对象流水号，例如清算候选、结算单或出款单号")
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

    @Schema(description = "最后一次重跑对账运行结果流水号")
    private String lastRerunSn;

    @Schema(description = "最后一次重跑批次流水号")
    private String lastRerunBatchSn;

    @Schema(description = "重跑次数")
    private Integer rerunCount;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "处理人")
    private String adjustedBy;

    @Schema(description = "关闭人")
    private String resolvedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "处理时间")
    private LocalDateTime adjustedTime;

    @Schema(description = "关闭时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "描述")
    private String description;
}
