package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金指令类型。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsInstructionType implements DescriptiveEnum {

    TRANSFER("直接资金交易"),

    AUTHORIZATION("授权交易"),

    BALANCE_CONTROL("余额控制");

    private final String desc;
}
