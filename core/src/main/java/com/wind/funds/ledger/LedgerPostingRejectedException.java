package com.wind.funds.ledger;

import java.io.Serial;

/**
 * 账本事实写入前已确定拒绝本次入账。
 *
 * <p>仅用于可安全保留资金失败事实的前置校验；账本写入或余额投影开始后的异常不得转换为该类型。</p>
 */
public class LedgerPostingRejectedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fundsTransactionSn;

    public LedgerPostingRejectedException(String fundsTransactionSn, String message) {
        super(message);
        this.fundsTransactionSn = fundsTransactionSn;
    }

    /**
     * @return 已落库的失败资金交易流水号
     */
    public String getFundsTransactionSn() {
        return fundsTransactionSn;
    }
}
