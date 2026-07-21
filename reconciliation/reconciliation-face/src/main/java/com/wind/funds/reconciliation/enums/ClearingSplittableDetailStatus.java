package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可清分明细准入状态。
 *
 * <p>只表达单笔来源事实能否进入后续清分，不表示已经清分、清算或结算。</p>
 */
@Getter
@AllArgsConstructor
public enum ClearingSplittableDetailStatus implements DescriptiveEnum {

    SPLIT_READY("可进入清分"),

    EXCLUDED("已排除");

    private final String desc;
}
