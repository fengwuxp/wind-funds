package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金追偿单状态。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum RecoveryOrderStatus implements DescriptiveEnum {

    /** 已创建，尚未追回资金。 */
    CREATED("待追回"),

    /** 已追回部分应追偿金额。 */
    PARTIALLY_RECOVERED("部分追回"),

    /** 应追偿金额已经全部追回。 */
    RECOVERED("已追回");

    private final String desc;
}
