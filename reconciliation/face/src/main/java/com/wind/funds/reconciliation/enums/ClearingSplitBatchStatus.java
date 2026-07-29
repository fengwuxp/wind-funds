package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 清分批次状态。
 *
 * <p>{@code CONFIRMED} 表示清分结果快照已冻结，是清分批次成功终态，
 * 但不代表后续清算入账已经发生。</p>
 */
@Getter
@AllArgsConstructor
public enum ClearingSplitBatchStatus implements DescriptiveEnum {

    DRAFT("草稿"),

    REVIEWING("复核中"),

    CONFIRMED("已确认"),

    CANCELLED("已取消");

    private final String desc;
}
