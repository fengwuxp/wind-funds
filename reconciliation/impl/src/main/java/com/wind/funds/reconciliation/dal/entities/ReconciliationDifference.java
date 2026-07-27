package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账差错。
 */
@Data
@Table(ReconciliationDifference.TABLE_NAME)
@FieldNameConstants
public class ReconciliationDifference implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -3635821104608317030L;

    public static final String TABLE_NAME = "t_reconciliation_difference";

    /**
     * 自增主键。
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 最后修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 对账差错流水号。
     */
    @NotNull
    private String differenceSn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 对账批次流水号。
     */
    @NotNull
    private String reconciliationBatchSn;

    /**
     * 对账逐笔匹配结果流水号。
     */
    @NotNull
    private String reconciliationMatchResultSn;

    /**
     * 对账来源质量。
     */
    @NotNull
    private ReconciliationSourceQuality sourceQuality;

    /**
     * 对账匹配强度。
     */
    @NotNull
    private ReconciliationMatchStrength matchStrength;

    /**
     * 对账差错类型。
     */
    @NotNull
    private ReconciliationDifferenceType differenceType;

    /**
     * 对账差错严重等级。
     */
    @NotNull
    private ReconciliationDifferenceSeverity severity;

    /**
     * 对账差错状态。
     */
    @NotNull
    private ReconciliationDifferenceStatus status;

    /**
     * 金额差异的币种；非金额差异为空。
     */
    private CurrencyIsoCode currency;

    /**
     * 金额差异，最小货币单位；非金额差异为空。
     */
    private Long differenceAmount;

    /**
     * 责任方引用。
     */
    @NotNull
    private String responsiblePartyRef;

    /**
     * 阻断对象类型，例如清算候选、结算单或出款单。
     */
    @NotNull
    private ReconciliationGateObjectType blockingObjectType;

    /**
     * 阻断对象流水号。
     */
    @NotNull
    private String blockingObjectSn;

    /**
     * 匹配或对账规则版本。
     */
    @NotNull
    private String ruleVersion;

    /**
     * 来源证据引用。
     */
    @NotNull
    private String evidenceRef;

    /**
     * 差错处理动作类型。
     */
    private ReconciliationDifferenceActionType actionType;

    /**
     * 关联处理动作或调账单号。
     */
    private String adjustmentSn;

    /**
     * 处理动作幂等键。
     */
    private String adjustmentIdempotencyKey;

    /**
     * 被处理的原始事实引用。
     */
    private String originalFactRef;

    /**
     * 关联资金交易流水号。
     */
    private String adjustmentTransactionSn;

    /**
     * 调账审批引用。
     */
    private String adjustmentApprovalRef;

    /**
     * 调账证据引用。
     */
    private String adjustmentEvidenceRef;

    /**
     * 处理原因。
     */
    private String adjustmentReason;

    /**
     * 最后一次重跑对账运行结果流水号。
     */
    private String lastRerunSn;

    /**
     * 最后一次重跑批次流水号。
     */
    private String lastRerunBatchSn;

    /**
     * 最后一次重跑规则版本。
     */
    private String lastRerunRuleVersion;

    /**
     * 最后一次重跑是否对平。
     */
    private Boolean lastRerunBalanced;

    /**
     * 最后一次重跑运行结果证据引用，使用 ReconciliationRunResult.sn。
     */
    private String lastRerunEvidenceRef;

    /**
     * 最后一次重跑结果摘要。
     */
    private String lastRerunResultDigest;

    /**
     * 重跑次数。
     */
    @NotNull
    private Integer rerunCount;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 处理人。
     */
    private String adjustedBy;

    /**
     * 关闭人。
     */
    private String resolvedBy;

    /**
     * 处理时间。
     */
    private LocalDateTime adjustedTime;

    /**
     * 关闭时间。
     */
    private LocalDateTime resolvedTime;

    /**
     * 描述。
     */
    private String description;

    /**
     * 乐观锁版本号。
     */
    @NotNull
    @Column(version = true)
    private Integer version;
}
