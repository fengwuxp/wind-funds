package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账准入消费对象类型。
 *
 * <p>职责：标识当前准入判断服务于清算、结算还是出款消费方。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationGateObjectType implements DescriptiveEnum {

    /**
     * 清算候选或清算确认前置准入。
     */
    CLEARING("清算"),

    /**
     * 结算锁定或结算确认前置准入。
     */
    SETTLEMENT("结算"),

    /**
     * 出款提交前置准入。
     */
    PAYOUT("出款");

    private final String desc;
}
