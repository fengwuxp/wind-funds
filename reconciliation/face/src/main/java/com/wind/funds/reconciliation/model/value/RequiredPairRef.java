package com.wind.funds.reconciliation.model.value;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 一个门禁要求所需的必需范围与对账对。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "必需对账对引用")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RequiredPairRef implements Serializable {

    @Serial
    private static final long serialVersionUID = -1670883051756068160L;

    @Schema(description = "精确对账范围身份")
    @Valid
    @NotNull
    private StableIdentity scopeIdentity;

    @Schema(description = "精确比较对身份")
    @Valid
    @NotNull
    private StableIdentity pairIdentity;

    @Schema(description = "该对账对要求的比较规则版本")
    @Valid
    @NotNull
    private ComparisonRuleRef comparisonRuleRef;
}
