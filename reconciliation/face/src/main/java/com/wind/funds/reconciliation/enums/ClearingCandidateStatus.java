package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 清算候选状态。
 *
 * <p>候选只表达清算准入和批次占用，不表达清算资金事实；CLEARED 由后续清算批次在确认资金事实后推进。</p>
 */
@Getter
@AllArgsConstructor
public enum ClearingCandidateStatus implements DescriptiveEnum {

    WAITING_PERIOD("等待账期到达"),

    BLOCKED("被准入守卫阻断"),

    READY("可进入清算批次"),

    LOCKED("已被清算批次锁定"),

    CLEARED("已完成清算事实"),

    EXCLUDED("已排除");

    private final String desc;
}
