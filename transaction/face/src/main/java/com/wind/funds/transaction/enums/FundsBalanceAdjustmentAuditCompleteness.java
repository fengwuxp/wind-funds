package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 余额调账审计链路完整性。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Getter
@AllArgsConstructor
public enum FundsBalanceAdjustmentAuditCompleteness implements DescriptiveEnum {

    /**
     * 交易事实、RouteSnapshot 和账本事实均存在。
     */
    COMPLETE("完整"),

    /**
     * 交易事实存在，但 RouteSnapshot 缺失。
     */
    INCOMPLETE_ROUTE("缺少 RouteSnapshot"),

    /**
     * 交易事实和 RouteSnapshot 存在，但账本交易或分录缺失。
     */
    INCOMPLETE_LEDGER("缺少账本事实"),

    /**
     * 未找到余额调账交易事实。
     */
    NOT_FOUND("未找到");

    private final String desc;
}
