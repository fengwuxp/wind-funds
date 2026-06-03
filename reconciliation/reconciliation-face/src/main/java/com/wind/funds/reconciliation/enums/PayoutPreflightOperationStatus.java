package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入操作状态。
 *
 * <p>职责：表达调用方是否可以继续提交出款动作。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightOperationStatus implements DescriptiveEnum {

    /**
     * 可提交。
     */
    SUBMITTABLE("可提交"),

    /**
     * 已阻断。
     */
    BLOCKED("已阻断");

    private final String desc;
}
