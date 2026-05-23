package com.capte.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入阻断原因编码。
 *
 * <p>职责：为产品、运营、风控、财务和研发提供稳定的阻断原因口径。</p>
 *
 * <p>边界：编码只解释准入失败原因，不承担外部通道回执、清算差错或账务冲正语义。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightBlockingReasonCode implements DescriptiveEnum {

    /**
     * 结算单尚未锁定。
     */
    SETTLEMENT_NOT_LOCKED("结算单尚未锁定"),

    /**
     * 出款账户无效。
     */
    PAYOUT_ACCOUNT_INVALID("出款账户无效"),

    /**
     * 收款端点无效。
     */
    PAYEE_ENDPOINT_INVALID("收款端点无效"),

    /**
     * 通道不可用。
     */
    CHANNEL_UNAVAILABLE("通道不可用"),

    /**
     * 通道限额超限。
     */
    CHANNEL_LIMIT_EXCEEDED("通道限额超限"),

    /**
     * 通道 cut-off 已关闭。
     */
    CUTOFF_CLOSED("通道 cut-off 已关闭"),

    /**
     * 名单筛查阻断。
     */
    WATCHLIST_BLOCKED("名单筛查阻断"),

    /**
     * 外部规则未核验。
     */
    EXTERNAL_RULE_UNVERIFIED("外部规则未核验"),

    /**
     * 负余额阻断。
     */
    NEGATIVE_BALANCE_BLOCKED("负余额阻断"),

    /**
     * 备付或预留余额不足。
     */
    RESERVE_INSUFFICIENT("备付或预留余额不足"),

    /**
     * 对账状态阻断。
     */
    RECONCILIATION_BLOCKED("对账状态阻断"),

    /**
     * 幂等冲突。
     */
    IDEMPOTENCY_CONFLICT("幂等冲突"),

    /**
     * 需要审批。
     */
    APPROVAL_REQUIRED("需要审批");

    private final String desc;
}
