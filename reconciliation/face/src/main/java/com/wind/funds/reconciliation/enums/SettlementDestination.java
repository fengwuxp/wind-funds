package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算资金目标类型。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum SettlementDestination implements DescriptiveEnum {

    /** 结算到 wind-funds 内部资金账户。 */
    INTERNAL_ACCOUNT("内部账户"),

    /** 结算到外部收款端点，由出款轨道继续处理。 */
    EXTERNAL_ENDPOINT("外部收款端点");

    private final String desc;
}
