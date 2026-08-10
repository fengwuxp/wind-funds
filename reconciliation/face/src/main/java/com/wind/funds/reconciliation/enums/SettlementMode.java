package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算资金承载模式。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum SettlementMode implements DescriptiveEnum {

    /** 通过中间资金账户承接结算金额。 */
    INTERMEDIARY_ACCOUNT("中间户模式"),
    /** 通过冻结余额承接结算金额。 */
    FROZEN("冻结模式"),
    /** 生成账单事实，不直接执行资金移动。 */
    BILL("账单模式");

    private final String desc;
}
