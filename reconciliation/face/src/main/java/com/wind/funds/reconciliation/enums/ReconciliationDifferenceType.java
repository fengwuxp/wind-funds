package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账差错类型。
 *
 * <p>职责：标识内部事实、账本、外部来源或匹配结果之间的差异类别。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationDifferenceType implements DescriptiveEnum {

    /**
     * 金额不一致。
     */
    AMOUNT_MISMATCH("金额不一致"),

    /**
     * 状态不一致。
     */
    STATUS_MISMATCH("状态不一致"),

    /**
     * 基准侧事实缺失。
     */
    REFERENCE_MISSING("基准侧事实缺失"),

    /**
     * 核对侧事实缺失。
     */
    COMPARISON_MISSING("核对侧事实缺失"),

    /**
     * 重复来源或重复匹配。
     */
    DUPLICATE("重复记录"),

    /**
     * 主体或账户不一致。
     */
    SUBJECT_MISMATCH("主体不一致"),

    /**
     * 来源未验证或来源质量不足。
     */
    SOURCE_UNVERIFIED("来源未验证");

    private final String desc;
}
