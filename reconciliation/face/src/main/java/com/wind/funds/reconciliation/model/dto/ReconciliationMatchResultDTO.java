package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
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
 * 对账逐笔匹配结果 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationMatchResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3924094959104185699L;

    @Schema(description = "逐笔匹配结果流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "对账批次流水号")
    private String reconciliationBatchSn;

    @Schema(description = "基准侧事实稳定引用")
    private String referenceSourceRef;

    @Schema(description = "核对侧事实稳定引用")
    private String comparisonSourceRef;

    @Schema(description = "来源质量")
    private ReconciliationSourceQuality sourceQuality;

    @Schema(description = "匹配强度")
    private ReconciliationMatchStrength matchStrength;

    @Schema(description = "差错类型；自动对平项为空")
    private ReconciliationDifferenceType differenceType;

    @Schema(description = "差错严重等级；自动对平项为空")
    private ReconciliationDifferenceSeverity severity;

    @Schema(description = "差异币种")
    private CurrencyIsoCode currency;

    @Schema(description = "差异金额，最小货币单位")
    private Long differenceAmount;

    @Schema(description = "匹配结论证据引用")
    private String evidenceRef;

    @Schema(description = "来源对身份 SHA-256")
    private String matchIdentityDigest;

    @Schema(description = "逐笔匹配结果 SHA-256")
    private String matchDigest;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
