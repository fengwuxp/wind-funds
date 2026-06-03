package com.wind.funds.spec;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金账务 DSL 来源事实类型。
 *
 * <p>来源事实用于说明本次可入账事实来自哪类业务对象，避免把资金交易、冻结订单、
 * 清结算单据、对账差错和争议单混在同一个生命周期语义里。</p>
 */
@Getter
@AllArgsConstructor
public enum SourceObjectType implements DescriptiveEnum {

    FUNDS_TRANSACTION("资金交易"),

    FROZEN_ORDER("冻结订单"),

    SETTLEMENT_ORDER("结算单"),

    RECONCILIATION_ADJUSTMENT("对账差错调账"),

    DISPUTE_CASE("争议单"),

    BALANCE_ADJUSTMENT("余额调整"),

    BUDGET_ADJUSTMENT("预算调整");

    private final String desc;
}
