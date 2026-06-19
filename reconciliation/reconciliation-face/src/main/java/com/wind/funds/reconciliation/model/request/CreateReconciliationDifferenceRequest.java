package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建对账差错请求。
 *
 * <p>职责：承载来源质量、匹配强度、差异金额、责任方、阻断范围和证据引用。</p>
 *
 * <p>边界：请求只登记差错运营对象，不直接发起补事实、冲正、调账或追偿。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateReconciliationDifferenceRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -737023897466599657L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账差错流水号，用于幂等")
    @NotBlank
    private String differenceSn;

    @Schema(description = "对账批次流水号")
    @NotBlank
    private String reconciliationBatchSn;

    @Schema(description = "对账来源记录流水号")
    @NotBlank
    private String sourceRecordSn;

    @Schema(description = "对账来源质量")
    @NotNull
    private ReconciliationSourceQuality sourceQuality;

    @Schema(description = "对账匹配强度")
    @NotNull
    private ReconciliationMatchStrength matchStrength;

    @Schema(description = "对账差错类型")
    @NotNull
    private ReconciliationDifferenceType differenceType;

    @Schema(description = "对账差错严重等级")
    @NotNull
    private ReconciliationDifferenceSeverity severity;

    @Schema(description = "差异币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "差异金额，最小货币单位")
    @NotNull
    @Positive
    private Long differenceAmount;

    @Schema(description = "责任方引用")
    @NotBlank
    private String responsiblePartyRef;

    @Schema(description = "阻断范围，例如 CLEARING、SETTLEMENT、PAYOUT")
    @NotBlank
    private String blockingScope;

    @Schema(description = "阻断对象类型，例如 CLEARING、SETTLEMENT、PAYOUT；为空表示历史类型级阻断")
    private ReconciliationGateObjectType blockingObjectType;

    @Schema(description = "阻断对象流水号，例如清算候选、结算单或出款单号；为空表示历史类型级阻断")
    private String blockingObjectSn;

    @Schema(description = "匹配或对账规则版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "来源证据引用")
    @NotBlank
    private String evidenceRef;

    @Schema(description = "描述")
    private String description;
}
