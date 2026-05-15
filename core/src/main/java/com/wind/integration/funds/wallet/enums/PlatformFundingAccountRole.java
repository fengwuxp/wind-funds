package com.wind.integration.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
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
     * 现金映射。
     */
    CASH_MAPPING(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.CASH, "现金映射"),

    /**
     * 预收待付。
     */
    PREPAYMENT(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.PREPAYMENT, "预收待付"),

    /**
     * 清算过渡。
     */
    CLEARING(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.CLEARING, "清算过渡"),

    /**
     * 结算应付。
     */
    SETTLEMENT(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.SETTLEMENT, "结算应付"),

    /**
     * 费用归集。
     */
    FEE(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.FEE, "费用归集"),

    /**
     * 调整挂账。
     */
    ADJUSTMENT(LedgerProfileCode.FUNDING_PLATFORM, LedgerSubjectCode.ADJUSTMENT, "调整挂账");

    private final LedgerProfileCode ledgerProfileCode;

    private final LedgerSubjectCode ledgerSubjectCode;

    private final String desc;
}
