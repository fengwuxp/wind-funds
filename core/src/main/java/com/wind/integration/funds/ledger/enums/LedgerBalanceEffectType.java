package com.wind.integration.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 余额影响语义。
 */
@AllArgsConstructor
@Getter
public enum LedgerBalanceEffectType implements DescriptiveEnum {

    INCREASE("增加"),

    DECREASE("减少"),

    HOLD("占用"),

    RELEASE("释放"),

    CONSUME("消耗"),

    RESTORE("回补");

    private final String desc;
}
