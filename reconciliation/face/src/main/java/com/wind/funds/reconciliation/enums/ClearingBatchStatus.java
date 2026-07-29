package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 清算批次状态。
 *
 * <p>CONFIRMED 是成功终态；FAILED 只表示资金明确失败且没有形成账本事实。</p>
 */
@Getter
@AllArgsConstructor
public enum ClearingBatchStatus implements DescriptiveEnum {

    DRAFT("草稿"),

    REVIEWING("待复核确认"),

    CONFIRMED("已确认清算"),

    CANCELLED("已取消"),

    FAILED("资金明确失败");

    private final String desc;
}
