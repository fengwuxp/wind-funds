package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账差错处理动作类型。
 *
 * <p>职责：约束差错闭环允许登记的白名单处理动作，避免运营回链退化为任意备注或任意单号。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationDifferenceActionType implements DescriptiveEnum {

    /**
     * 补充标准资金事实。
     */
    SUPPLEMENT_FACT("补事实"),

    /**
     * 冲正原资金事实。
     */
    REVERSE("冲正"),

    /**
     * 执行受控调账。
     */
    ADJUST("调账"),

    /**
     * 挂账等待后续处理。
     */
    SUSPENSE("挂账"),

    /**
     * 发起追偿。
     */
    RECOVER("追偿"),

    /**
     * 核销差错。
     */
    WRITE_OFF("核销");

    private final String desc;
}
