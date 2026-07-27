package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入展示状态。
 *
 * <p>职责：面向商户账单、出款处理台和审计导出表达准入结果的可读状态。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightDisplayStatus implements DescriptiveEnum {

    /**
     * 当前证据预检通过。
     */
    PREFLIGHT_PASSED("预检通过"),

    /**
     * 需要完成对账差错处理。
     */
    RECONCILIATION_REQUIRED("需要对账处理"),

    /**
     * 等待证据。
     */
    WAITING_EVIDENCE("等待证据");

    private final String desc;
}
