package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 与载体无关的归一化比较事实输入。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "归一化比较事实输入")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class NormalizedComparisonFactInput implements Serializable {

    @Serial
    private static final long serialVersionUID = 891460731209804275L;

    @Valid
    @NotNull
    @Schema(description = "来源方事实身份")
    private StableIdentity sourceFactRef;

    @Valid
    @NotNull
    @Schema(description = "用于比较双方来源的身份")
    private StableIdentity comparisonIdentity;

    @NotNull
    @Positive
    @Schema(description = "最小货币单位金额")
    private Long amount;

    @NotNull
    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Valid
    @NotNull
    @Schema(description = "比较规则引用")
    private ComparisonRuleRef comparisonRuleRef;

    @NotBlank
    @Schema(description = "规则范围内的比较状态码")
    private String comparisonStatusCode;

    @NotNull
    @Schema(description = "规则所有方是否已证明比较状态")
    private Boolean comparisonProven;

    @NotBlank
    @Schema(description = "规则范围内的声明类型")
    private String claimKind;

    @NotBlank
    @Schema(description = "规则范围内的经济组成")
    private String economicComponent;

    @NotBlank
    @Schema(description = "规则范围内的经济方向")
    private String direction;

    @NotBlank
    @Schema(description = "归一化契约版本")
    private String normalizationVersion;

    @NotNull
    @Size(max = 100)
    @Schema(description = "外部证据引用")
    private List<@NotBlank String> evidenceRefs;
}
