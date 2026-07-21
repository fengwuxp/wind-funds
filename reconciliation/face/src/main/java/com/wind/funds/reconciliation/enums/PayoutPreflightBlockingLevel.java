package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入阻断等级。
 *
 * <p>职责：让调用方区分准入通过、需要人工复核和必须阻断。</p>
 *
 * <p>边界：等级只控制出款提交前的准入结果，不表达出款单、结算单或账务事实的生命周期状态。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightBlockingLevel implements DescriptiveEnum {

    /**
     * 准入通过。
     */
    PASSED("通过"),

    /**
     * 需要人工复核。
     */
    MANUAL_REVIEW("人工复核"),

    /**
     * 必须阻断。
     */
    BLOCKED("阻断");

    private final String desc;
}
