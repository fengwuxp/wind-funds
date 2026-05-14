package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易动作对资金的业务效果。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsEffectType implements DescriptiveEnum {

    DIRECT("直接入账"),
    HOLD("占用"),
    RELEASE("释放"),
    CONSUME("消耗"),
    RETURN("回补"),
    ADJUST("调整"),
    DISPUTE("争议");

    private final String desc;
}
