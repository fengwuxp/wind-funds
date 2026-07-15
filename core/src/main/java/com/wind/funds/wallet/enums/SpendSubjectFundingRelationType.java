package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支出主体和真实资金账户关系类型。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum SpendSubjectFundingRelationType implements DescriptiveEnum {

    FUNDING_SOURCE("出资资金账户"),
    SETTLEMENT_TARGET("结算目标账户");

    private final String desc;
}
