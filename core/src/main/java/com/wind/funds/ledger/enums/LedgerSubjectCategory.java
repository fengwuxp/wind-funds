package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会计科目类别（Account Category）
 * <p>
 * 定义账户的会计属性及其正常余额方向，用于：
 * - 自动生成分录时的默认方向提示
 * - 余额合法性校验（如资产类账户不应为负）
 * - 财务报表的科目聚合
 *
 * @author wuxp
 * @date 2026-04-13 11:12
 **/
@Getter
@AllArgsConstructor
public enum LedgerSubjectCategory implements DescriptiveEnum {

    /**
     * 资产类账户（用户/商户资金）
     * 正常余额：借（Debit）
     */
    ASSET("资产类", EntrySide.DEBIT),

    /**
     * 负债类账户（平台代管资金）
     * 正常余额：贷（Credit）
     */
    LIABILITY("负债类", EntrySide.CREDIT),

    /**
     * 控制类账户（额度、预算等非资金本金科目）
     * 正常余额：由 LedgerProfileItem 定义
     */
    CONTROL("控制类账户", null),

    /**
     * 收入类（平台收入，如手续费）
     * 正常余额：贷
     */
    REVENUE("收入类", EntrySide.CREDIT),

    /**
     * 成本类（平台支出）
     * 正常余额：借
     */
    EXPENSE("成本类", EntrySide.DEBIT),

    /**
     * 权益类账户（平台利润）
     * 账户余额：贷
     */
    EQUITY("权益", EntrySide.CREDIT),

    /**
     * 清算过渡账户（Clearing / Pending）
     * 正常余额：可正可负（特殊处理）
     */
    CLEARING("清算账户", null),

    /**
     * 挂账过渡账户
     * 正常余额：可正可负（特殊处理）
     */
    SUSPENSE("挂账账户", null),

    /**
     * 备查类账户（Memo）
     * 账户余额：可正可负（特殊处理）
     */
    MEMO("备查类账户", null);

    private final String desc;

    /**
     * 账本 normal Balance
     */
    private final EntrySide normalBalance;
}
