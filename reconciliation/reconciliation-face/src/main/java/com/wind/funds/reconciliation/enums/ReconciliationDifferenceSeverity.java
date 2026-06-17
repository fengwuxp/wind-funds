package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账差错严重等级。
 *
 * <p>职责：表达差错对清分、清算、结算、出款或关账的影响等级。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationDifferenceSeverity implements DescriptiveEnum {

    /**
     * 致命差错，需要立即阻断并升级。
     */
    S0_CRITICAL("致命差错"),

    /**
     * 重大差错，需要阻断相关批次或出款。
     */
    S1_MAJOR("重大差错"),

    /**
     * 一般差错，可进入人工复核或后续处理。
     */
    S2_MINOR("一般差错");

    private final String desc;
}
