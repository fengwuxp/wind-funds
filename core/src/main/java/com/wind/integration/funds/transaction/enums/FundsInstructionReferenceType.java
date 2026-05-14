package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金指令引用类型。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsInstructionReferenceType implements DescriptiveEnum {

    ORIGINAL_TRANSACTION("原交易"),

    AUTHORIZATION("原授权"),

    FREEZE_ORDER("原冻结单"),

    REFUND("原退款"),

    FEE("原手续费"),

    EXTERNAL_TRANSACTION("外部交易");

    private final String desc;
}
