package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支出控制额度变动类型。
 *
 * @author Codex
 * @date 2026-06-20
 */
@AllArgsConstructor
@Getter
public enum SpendControlMovementType implements DescriptiveEnum {

    LIMIT_INCREASED("控制额度调增", true, true, false),

    LIMIT_DECREASED("控制额度调减", true, true, false),

    RESERVED("控制占用", true, false, false),

    CONSUMED("控制消耗", true, false, false),

    REFUND_COMPENSATED("退款控制补偿", true, false, false),

    RELEASED("控制释放", true, false, true);

    private final String desc;

    /**
     * 是否参与预算控制投影。
     */
    private final boolean budgetProjectionMovement;

    /**
     * 是否属于预算控制额度调整流水。
     */
    private final boolean limitAdjustmentMovement;

    /**
     * 是否属于控制占用释放流水。
     */
    private final boolean releaseMovement;

    /**
     * 是否为新的控制额度变动流水可写类型。
     *
     * @return true 表示可以通过控制额度变动流水入口写入
     */
    public boolean isControlMovement() {
        return budgetProjectionMovement;
    }
}
