package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易模式。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsTransactionMode implements DescriptiveEnum {

    /**
     * 无授权的直接交易。
     */
    DIRECT("直接交易"),

    /**
     * 授权、完成、撤销和退款生命周期交易。
     */
    AUTHORIZATION("授权交易"),

    /**
     * 余额、额度、预算控制交易。
     */
    BALANCE_CONTROL("余额控制");

    private final String desc;
}
