package com.wind.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Route 原子资金步骤类型。
 */
@AllArgsConstructor
@Getter
public enum RouteLegType implements DescriptiveEnum {

    EXTERNAL_IN("外部入金"),

    EXTERNAL_OUT("外部出金"),

    INTERNAL_TRANSFER("内部划转"),

    HOLD("占用"),

    RELEASE("释放"),

    CONSUME("消耗"),

    RESTORE("回补"),

    ADJUST("调整");

    private final String desc;
}
