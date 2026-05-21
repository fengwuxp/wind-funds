package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易动作对资金的业务效果。
 *
 * <p>该枚举描述交易事实对资金的结果口径，不描述争议、风控、清结算等业务状态。</p>
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
    ADJUST("调整");

    private final String desc;
}
