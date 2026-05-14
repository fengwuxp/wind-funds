package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台资金账户角色。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum PlatformFundingAccountRole implements DescriptiveEnum {

    /**
     * 备付金。
     */
    RESERVE_FUND(LedgerProfileCode.FUNDING_PLATFORM, "备付金"),

    /**
     * 预收款。
     */
    PREPAYMENT(LedgerProfileCode.FUNDING_PLATFORM, "预收款"),

    /**
     * 清算过渡。
     */
    CLEARING(LedgerProfileCode.FUNDING_PLATFORM, "清算过渡"),

    /**
     * 结算归集。
     */
    SETTLEMENT(LedgerProfileCode.FUNDING_PLATFORM, "结算归集"),

    /**
     * 手续费归集。
     */
    FEE(LedgerProfileCode.FUNDING_PLATFORM, "手续费归集");

    private final LedgerProfileCode ledgerProfileCode;

    private final String desc;
}
