package com.capte.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账任务状态。
 */
@AllArgsConstructor
@Getter
public enum ReconciliationTaskStatus implements DescriptiveEnum {

    CREATED("已创建"),

    DATA_COLLECTING("收数中"),

    DATA_READY("数据齐备"),

    MATCHING("匹配中"),

    BALANCED("已对平"),

    DIFF_FOUND("发现差异"),

    BLOCKED("已阻断"),

    CONDITIONAL_RELEASED("有条件放行"),

    FAILED("失败"),

    CLOSED("已关闭");

    private final String desc;
}
