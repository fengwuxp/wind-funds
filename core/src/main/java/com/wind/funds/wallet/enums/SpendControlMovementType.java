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

    ADMISSION_RECORDED("准入已记录", false, false, false, true),

    REJECTED_RECORDED("拒绝已记录", false, false, false, true),

    LIMIT_INCREASED("控制额度调增", true, true, false, false),

    LIMIT_DECREASED("控制额度调减", true, true, false, false),

    RESERVED("控制占用", true, false, false, false),

    CONSUMED("控制消耗", true, false, false, false),

    REFUND_COMPENSATED("退款控制补偿", true, false, false, false),

    RELEASED("控制释放", true, false, true, false),

    EXPIRED("控制过期", true, false, true, false),

    REVERSED("控制撤销", true, false, true, false);

    private static final String DECISION_RECORD_PRODUCT_SEMANTIC = "SpendRuleDecisionRecord";

    private static final String CONTROL_MOVEMENT_PRODUCT_SEMANTIC = "SpendControlMovement";

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
     * 是否属于释放、过期或撤销类控制占用释放流水。
     */
    private final boolean releaseMovement;

    /**
     * 是否为历史决策记录兼容变动类型。
     */
    private final boolean decisionRecordType;

    /**
     * 是否为新的控制额度变动流水可写类型。
     *
     * @return true 表示可以通过控制额度变动流水入口写入
     */
    public boolean isControlMovement() {
        return budgetProjectionMovement;
    }

    /**
     * 获取兼容期产品目标语义。
     *
     * @return 产品目标语义
     */
    public String getProductSemantic() {
        if (decisionRecordType) {
            return DECISION_RECORD_PRODUCT_SEMANTIC;
        }
        return CONTROL_MOVEMENT_PRODUCT_SEMANTIC;
    }
}
