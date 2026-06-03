package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本余额桶编码。
 *
 * <p>只表达资金或额度在主体中的余额状态，不表达主体归属。</p>
 *
 * @author wuxp
 * @date 2026-04-23
 */
@AllArgsConstructor
@Getter
public enum LedgerSubjectCode implements DescriptiveEnum {

    /**
     * 现金或外部资金池的内部映射。
     */
    CASH("现金/外部资金映射"),

    /**
     * 可用余额或可用额度。
     */
    AVAILABLE("可用余额"),

    /**
     * 授权占用。
     */
    AUTHORIZATION("授权占用"),

    /**
     * 冻结余额。
     */
    FROZEN("冻结余额"),

    /**
     * 额度池总量。
     */
    LIMIT("额度池总量"),

    /**
     * 预收款/中间过渡余额。
     */
    PREPAYMENT("预收款"),

    /**
     * 清分/清算中余额。
     */
    CLEARING("清算中余额"),

    /**
     * 结算归集余额。
     */
    SETTLEMENT("结算归集"),

    /**
     * 手续费归集。
     */
    FEE("手续费"),

    /**
     * 调账/异常调整中间余额。
     */
    ADJUSTMENT("调整余额"),

    /**
     * 在途资金，保留给后续增强场景。
     */
    IN_TRANSIT("在途资金"),

    /**
     * 风险准备金，保留给后续增强场景。
     */
    RISK_RESERVE("风险准备金"),

    /**
     * 预收入，保留给后续增强场景。
     */
    DEFERRED_REVENUE("预收入"),

    /**
     * 挂账余额，保留给异常挂账场景。
     */
    SUSPENSE("挂账余额");

    private final String desc;
}
