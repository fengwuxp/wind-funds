package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LedgerPostingIntent（账务意图）
 * 定义：这笔账“为什么发生”
 * ⚠️ 注意：
 * - 不等于执行阶段（Phase）
 * - 不等于执行方式（BatchType）
 */
@AllArgsConstructor
@Getter
public enum LedgerPostingIntentType implements DescriptiveEnum {

    // =========================
    // 1. 资金流基础行为
    // =========================

    /**
     * 充值
     */
    TOPUP("充值"),

    /**
     * 提现
     */
    WITHDRAWAL("提现"),

    /**
     * 转账
     */
    TRANSFER("转账"),

    /**
     * 资金占用
     */
    HOLD("占用"),


    // =========================
    // 2. 支付类（核心）
    // =========================

    /**
     * 授权（预占用资金）
     */
    AUTHORIZATION("授权"),

    /**
     * 授权撤销
     */
    AUTHORIZATION_REVERSAL("授权撤销"),

    /**
     * 授权完成（消费成立）
     */
    AUTHORIZATION_COMPLETION("授权完成"),

    // =========================
    // 3. 清结算类
    // =========================

    /**
     * 清算
     */
    SETTLEMENT("清算"),

    /**
     * 退款
     */
    REFUND("退款"),

    /**
     * 冲正 / 撤销
     */
    REVERSAL("冲正"),

    /**
     * 调账
     */
    ADJUSTMENT("调账"),

    // ========= 手续费（关键） =========

    /**
     * 收取手续费
     */
    FEE("手续费"),

    /**
     * 手续费退款
     */
    FEE_REFUND("手续费退回"),

    /**
     * 手续费冲正
     */
    FEE_REVERSAL("手续费冲正"),
    ;

    private final String desc;
}
