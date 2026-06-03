package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本分录类型
 *
 * @author wuxp
 * @date 2026-01-29 13:37
 **/
@AllArgsConstructor
@Getter
public enum EntrySide implements DescriptiveEnum {

    /**
     * 借记
     */
    DEBIT("借记","Dr"),

    /**
     * 贷记
     */
    CREDIT("贷记","Cr");

    private final String desc;

    private final String symbol;

    /**
     * 返回相反方向
     */
    public EntrySide reverse() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
