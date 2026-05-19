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
    ADJUST("调整"),
    /**
     * @deprecated 争议是业务事件或状态，不是独立资金效果。拒付、退款等争议后的资金回退使用 {@link #RETURN}。
     */
    @Deprecated(since = "1.0.1", forRemoval = false)
    DISPUTE("争议");

    private final String desc;
}
