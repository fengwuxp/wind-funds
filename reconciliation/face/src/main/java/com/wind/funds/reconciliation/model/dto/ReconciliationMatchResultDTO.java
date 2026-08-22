package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationMatchResultKind;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.value.StableIdentity;
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
    private StableIdentity referenceFactRef;

    @Schema(description = "核对侧事实稳定引用")
    private StableIdentity comparisonFactRef;

    @Schema(description = "比较身份")
    private StableIdentity comparisonIdentity;

    @Schema(description = "提供方计算的严格比较结果")
    private ReconciliationMatchResultKind resultKind;

    @Schema(description = "绝对差额币种；仅 MONEY_MISMATCH 非空")
    private CurrencyIsoCode absoluteDifferenceCurrency;

    @Schema(description = "绝对差额，最小货币单位；仅 MONEY_MISMATCH 非空")
    private Long absoluteDifferenceAmount;

    @Schema(description = "金额较大侧；仅 MONEY_MISMATCH 非空")
    private ReconciliationSourceRole largerSide;

    @Schema(description = "来源对身份 SHA-256")
    private String matchIdentityDigest;

    @Schema(description = "逐笔匹配结果 SHA-256")
    private String resultDigest;

    @Schema(description = "匹配结论证据引用")
    private List<String> evidenceRefs;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
