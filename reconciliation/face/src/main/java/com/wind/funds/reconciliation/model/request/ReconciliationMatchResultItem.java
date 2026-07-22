package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对账匹配结果项。
 *
 * <p>一项表示一次基准侧事实与核对侧事实的匹配结论；缺失类差异允许其中一侧引用为空。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationMatchResultItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 2754849563169512824L;

    @Schema(description = "基准侧事实稳定引用；REFERENCE_MISSING 时为空")
    private String referenceSourceRef;

    @Schema(description = "核对侧事实稳定引用；COMPARISON_MISSING 时为空")
    private String comparisonSourceRef;

    @Schema(description = "来源质量")
    @NotNull
    private ReconciliationSourceQuality sourceQuality;

    @Schema(description = "匹配强度")
    @NotNull
    private ReconciliationMatchStrength matchStrength;

    @Schema(description = "差错类型；自动对平项为空")
    private ReconciliationDifferenceType differenceType;

    @Schema(description = "差错严重等级；自动对平项为空")
    private ReconciliationDifferenceSeverity severity;

    @Schema(description = "差异币种；存在差异金额时必填")
    private CurrencyIsoCode currency;

    @Schema(description = "差异金额，最小货币单位；自动对平项为空")
    @PositiveOrZero
    private Long differenceAmount;

    @Schema(description = "该匹配结论的稳定证据引用")
    @NotBlank
    private String evidenceRef;
}
