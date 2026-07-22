package com.wind.funds.reconciliation.enums;

/**
 * 对账运行结果状态。
 *
 * <p>只有 {@link #BALANCED} 是可被后续清分、结算或出款准入消费的正向证据。</p>
 */
public enum ReconciliationRunResultStatus {

    /**
     * 已完成匹配且全部对平。
     */
    BALANCED,

    /**
     * 已完成匹配并发现差错。
     */
    DIFFERENCE_FOUND
}
