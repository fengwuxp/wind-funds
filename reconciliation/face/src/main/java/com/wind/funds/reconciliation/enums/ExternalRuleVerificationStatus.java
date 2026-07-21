package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 外部规则核验状态。
 *
 * <p>职责：表达出款前准入对外部规则、通道规则、风控规则或合规规则的核验结论。</p>
 *
 * <p>边界：状态只表达本次准入检查看到的规则核验结果，不替代法务、合规、财务或通道最终确认。</p>
 */
@AllArgsConstructor
@Getter
public enum ExternalRuleVerificationStatus implements DescriptiveEnum {

    /**
     * 已核验。
     */
    VERIFIED("已核验"),

    /**
     * 未核验。
     */
    UNVERIFIED("未核验");

    private final String desc;
}
