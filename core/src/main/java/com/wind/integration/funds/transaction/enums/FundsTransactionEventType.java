package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金生命周期事件类型。
 *
 * <p>优先表达账务层稳定事件语义，不直接暴露业务侧细碎流程节点。</p>
 *
 * @author wuxp
 */
@AllArgsConstructor
@Getter
public enum FundsTransactionEventType implements DescriptiveEnum {

    TOPUP("funds.transaction.topup", "充值"),
    TRANSFER("funds.transaction.transfer", "转账"),
    PAY("funds.transaction.pay", "支付"),
    REFUND("funds.transaction.refund", "退款"),
    WITHDRAW("funds.transaction.withdraw", "提现"),
    FEE_CHARGE("funds.transaction.fee.charge", "手续费收取"),
    FEE_REFUND("funds.transaction.fee.refund", "手续费退回"),

    AUTHORIZE("funds.authorization.authorize", "授权"),
    REVERSAL("funds.authorization.reversal", "撤销/冲正"),
    EXPIRE("funds.authorization.expire", "授权过期"),
    SETTLE("funds.authorization.settle", "结算"),
    AUTH_REFUND("funds.authorization.refund", "授权链退款"),
    CHARGEBACK("funds.authorization.chargeback", "拒付/争议"),

    FREEZE("funds.balance.freeze", "冻结"),
    UNFREEZE("funds.balance.unfreeze", "解冻"),
    BALANCE_ADJUST("funds.balance.adjust", "余额调整"),
    LIMIT_ADJUST("funds.limit.adjust", "额度调整");

    private final String eventName;

    private final String desc;
}
