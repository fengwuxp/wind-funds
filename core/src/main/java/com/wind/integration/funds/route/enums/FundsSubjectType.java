package com.wind.integration.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金账务主体类型。
 */
@AllArgsConstructor
@Getter
public enum FundsSubjectType implements DescriptiveEnum {

    FUNDING_ACCOUNT("资金账户"),

    CREDIT_ACCOUNT("信用账户"),

    BUDGET_GROUP("预算组");

    private final String desc;
}
