package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入事实状态。
 *
 * <p>职责：表达服务端准入检查看到的事实结论，不代表出款生命周期结果。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightFactStatus implements DescriptiveEnum {

    /**
     * 准入通过。
     */
    PREFLIGHT_PASSED("准入通过"),

    /**
     * 准入阻断。
     */
    PREFLIGHT_BLOCKED("准入阻断");

    private final String desc;
}
