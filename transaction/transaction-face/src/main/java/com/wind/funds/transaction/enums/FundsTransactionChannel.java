package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易渠道。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsTransactionChannel implements DescriptiveEnum {

    /**
     * 系统内部处理。
     */
    INTERNAL("内部交易"),

    /**
     * 银行转账。
     */
    WIRE_TRANSFER("银行转账");

    private final String desc;
}
