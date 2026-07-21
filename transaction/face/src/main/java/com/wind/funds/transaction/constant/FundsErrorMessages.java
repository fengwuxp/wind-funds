package com.wind.funds.transaction.constant;

/**
 * 错误信息
 *
 * @author wuxp
 * @date 2026-04-29 14:52
 **/
public final class FundsErrorMessages {


    private FundsErrorMessages() {
        throw new AssertionError();
    }

    /**
     * 账户可用余额不足
     */
    public static final String ACCOUNT_AVAILABLE_BALANCE_INSUFFICIENT_MESSAGE = "账户可用余额不足";

    /**
     * 账户冻结余额不足
     */
    public static final String ACCOUNT_FROZEN_BALANCE_INSUFFICIENT_MESSAGE = "账户冻结余额不足";
}
