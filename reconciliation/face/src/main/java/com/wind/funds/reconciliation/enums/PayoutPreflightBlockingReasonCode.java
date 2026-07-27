package com.wind.funds.reconciliation.enums;

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
     * 出款账户引用缺失。
     */
    PAYOUT_ACCOUNT_REF_MISSING("出款账户引用缺失"),

    /**
     * 收款端点引用缺失。
     */
    PAYEE_ENDPOINT_REF_MISSING("收款端点引用缺失"),

    /**
     * 通道引用缺失。
     */
    CHANNEL_REF_MISSING("通道引用缺失"),

    /**
     * 外部规则未核验。
     */
    EXTERNAL_RULE_UNVERIFIED("外部规则未核验"),

    /**
     * 对账状态阻断。
     */
    RECONCILIATION_BLOCKED("对账状态阻断"),

    /**
     * 需要审批。
     */
    APPROVAL_REQUIRED("需要审批");

    private final String desc;
}
