package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提供方计算的严格精确比较结果。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "严格精确对账结果类型")
@Getter
@AllArgsConstructor
public enum ReconciliationMatchResultKind implements DescriptiveEnum {

    MATCHED("Matched"),
    NOT_COMPARABLE("Not comparable"),
    REFERENCE_MISSING("Reference fact missing"),
    COMPARISON_MISSING("Comparison fact missing"),
    CURRENCY_MISMATCH("Currency mismatch"),
    MONEY_MISMATCH("Money mismatch"),
    STATUS_MISMATCH("Status mismatch"),
    SEMANTICS_MISMATCH("Semantics mismatch"),
    RULE_MISMATCH("Rule mismatch"),
    IDENTITY_CONFLICT("Identity conflict");

    private final String desc;
}
