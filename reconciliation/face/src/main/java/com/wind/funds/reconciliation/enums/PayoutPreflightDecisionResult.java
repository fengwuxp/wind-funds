package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入决策结果。
 *
 * <p>职责：表达服务端准入检查看到的事实结论，不代表出款生命周期结果。</p>
 *
 * @author wuxp
 * @since 2026-05-23
 */
@Schema(description = "出款前准入决策结果")
@AllArgsConstructor
@Getter
public enum PayoutPreflightDecisionResult implements DescriptiveEnum {

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
