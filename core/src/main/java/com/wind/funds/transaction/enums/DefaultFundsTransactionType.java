package com.wind.funds.transaction.enums;

import com.wind.common.WindConstants;
import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易行为类型（Fund Movement Behavior Type）
 * <p>
 * 描述一笔资金交易的<b>最终业务结果类型</b>，即资金在业务层面“最终做了什么”，
 * 反映资金所有权的转移结果或业务目的，是业务对账、报表统计、利润核算的基础维度。
 * </p>
 *
 * <p><b>本枚举包含的类型：</b></p>
 * <ul>
 *   <li>{@link #TOPUP} 充值：外部资金进入平台账户</li>
 *   <li>{@link #TRANSFER} 转账：系统内资金流转</li>
 *   <li>{@link #PAY} 支付/消费：资金从付款方转移至收款方</li>
 *   <li>{@link #FEE} 手续费：平台收取的服务费用</li>
 *   <li>{@link #REFUND} 退款：资金原路退回（逆向支付）</li>
 *   <li>{@link #WITHDRAW} 提现：资金从平台流向外部账户</li>
 *   <li>{@link #ADJUSTMENT} 调账（调额）人工或系统修正余额（增加）</li>
 * </ul>
 *
 * <p><b>使用定位：</b></p>
 * <ul>
 *   <li>作为 {@code FundsInstructionSpec} 中的核心字段，供业务方明确表达交易目的</li>
 *   <li>驱动账务引擎生成对应的 {@code LedgerEntry} 和事件映射</li>
 * </ul>
 *
 * @author wuxp
 * @date 2026-04-16 09:15
 */
@AllArgsConstructor
@Getter
public enum DefaultFundsTransactionType implements DescriptiveEnum {

    @Schema(description = "Top-up")
    TOPUP("充值"),

    @Schema(description = "Transfer")
    TRANSFER("转账（内部）"),

    /**
     * 支付/消费
     */
    @Schema(description = "Pay")
    PAY("支付"),

    /**
     * 手续费
     */
    @Schema(description = "Fee")
    FEE("手续费"),

    /**
     * 退款
     */
    @Schema(description = "Refund")
    REFUND("退款"),

    /**
     * 提现
     */
    @Schema(description = "Withdraw")
    WITHDRAW("提现"),

    /**
     * 已确认清算批次对应的账户内待清算资金转可用。
     */
    @Schema(description = "Clearing")
    CLEARING("清算确认"),

    /**
     * 已审批结算单对应的账户内可用资金转结算锁定。
     */
    @Schema(description = "Settlement")
    SETTLEMENT("结算锁定"),

    /**
     * 已锁定结算资金的外部出款结果。
     */
    @Schema(description = "Payout")
    PAYOUT("结算出款"),

    /*=================== 调账相关 ======================*/

    /**
     * 人工调账
     */
    @Schema(description = "Adjustment")
    ADJUSTMENT("调账"),

    ;

    private final String desc;

    /**
     * 转换为事件类型
     *
     * @return 事件类型
     */
    public String asEventType() {
        return name().replace(WindConstants.UNDERLINE, WindConstants.DOT).toLowerCase();
    }
}
