package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可清分准入排除原因。
 */
@Getter
@AllArgsConstructor
public enum ClearingSplittableExclusionReason implements DescriptiveEnum {

    TRANSACTION_NOT_ELIGIBLE("资金交易未形成可清分的成功事实"),

    TRANSACTION_DETAIL_NOT_SUCCEEDED("资金交易明细未成功"),

    SOURCE_FACT_INCOMPLETE("来源事实引用不完整"),

    SOURCE_FACT_MISMATCH("来源事实之间不一致"),

    LEDGER_ENTRY_NOT_CLEARING("来源分录未命中 CLEARING 账目"),

    LEDGER_ENTRY_NOT_CLEARING_INFLOW("来源事实不是 CLEARING 正向待清分入账"),

    REFUND_EXISTS("清分前已发生退款"),

    RECONCILIATION_BLOCKED("清分前对账门禁阻断");

    private final String desc;
}
