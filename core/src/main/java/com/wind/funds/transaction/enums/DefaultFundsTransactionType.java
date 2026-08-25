package com.wind.funds.transaction.enums;

import com.wind.common.WindConstants;
import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * 粗粒度资金动作族。
 * <p>
 * 表达一组资金生命周期事实所属的稳定业务目的，不单独表达处理模型、精确事件、
 * 账务作用或路由。合法指令由 instructionType、eventType、transactionType 三元组共同确定。
 * 对账、报表和核算应使用完整资金事实，不能仅按本枚举推导结果。
 * </p>
 *
 * @author wuxp
 * @date 2026-04-16 09:15
 */
@AllArgsConstructor
@Getter
public enum DefaultFundsTransactionType implements DescriptiveEnum {

    @Schema(description = "充值")
    TOPUP("充值"),

    @Schema(description = "内部转账")
    TRANSFER("转账（内部）"),

    /**
     * 支付/消费
     */
    @Schema(description = "支付")
    PAY("支付"),

    /**
     * 手续费
     */
    @Schema(description = "手续费")
    FEE("手续费"),

    /**
     * 退款
     */
    @Schema(description = "退款")
    REFUND("退款"),

    /**
     * 提现
     */
    @Schema(description = "提现")
    WITHDRAW("提现"),

    /**
     * 已确认清算批次对应的账户内待清算资金转可用。
     */
    @Schema(description = "清算确认")
    CLEARING("清算确认"),

    /**
     * 已审批结算单对应的账户内可用资金转结算锁定。
     */
    @Schema(description = "结算锁定")
    SETTLEMENT("结算锁定"),

    /**
     * 已锁定结算资金的外部出款结果。
     */
    @Schema(description = "结算出款")
    PAYOUT("结算出款"),

    /**
     * 同一资金主体内的冻结与解冻控制。
     */
    @Schema(description = "余额控制")
    BALANCE_CONTROL("余额控制"),

    /**
     * 受控余额或额度调整。
     */
    @Schema(description = "调账")
    ADJUSTMENT("调账"),

    ;

    private final String desc;

    /**
     * 保留旧版事件字符串投影，仅用于兼容存量调用方。
     * 精确生命周期事件必须读取 {@link FundsTransactionEventType}，不能由动作族推导。
     *
     * @return 旧版点分隔事件字符串
     * @deprecated 动作族与生命周期事件并非一一对应，请迁移到显式 eventType
     */
    @Deprecated(forRemoval = true)
    public String asEventType() {
        return name().replace(WindConstants.UNDERLINE, WindConstants.DOT).toLowerCase();
    }

    /**
     * 判断资金处理模型、生命周期事件和动作族是否为当前可执行组合。
     *
     * @param instructionType 资金处理模型
     * @param eventType 生命周期事件
     * @param transactionType 粗粒度动作族
     * @return true 表示组合可进入资金路由
     */
    public static boolean isValidInstructionCombination(@NonNull FundsInstructionType instructionType,
                                                        @NonNull FundsTransactionEventType eventType,
                                                        @NonNull DefaultFundsTransactionType transactionType) {
        return switch (eventType) {
            case TOPUP -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == TOPUP;
            case TRANSFER -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == TRANSFER;
            case PAY -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == PAY;
            case REFUND -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == REFUND;
            case WITHDRAW -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == WITHDRAW;
            case FEE_CHARGE -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == FEE;
            case FEE_REFUND -> instructionType == FundsInstructionType.DIRECT_TRANSACTION && transactionType == REFUND;
            case CLEARING_CONFIRM -> instructionType == FundsInstructionType.DIRECT_TRANSACTION
                    && transactionType == CLEARING;
            case SETTLEMENT_LOCK, SETTLEMENT_RELEASE -> instructionType == FundsInstructionType.DIRECT_TRANSACTION
                    && transactionType == SETTLEMENT;
            case PAYOUT_SUCCEEDED, PAYOUT_FAILED -> instructionType == FundsInstructionType.DIRECT_TRANSACTION
                    && transactionType == PAYOUT;
            case AUTHORIZE, REVERSAL, COMPLETE -> instructionType == FundsInstructionType.AUTHORIZATION_TRANSACTION
                    && transactionType == PAY;
            case AUTH_REFUND -> instructionType == FundsInstructionType.AUTHORIZATION_TRANSACTION
                    && transactionType == REFUND;
            case FREEZE, UNFREEZE -> instructionType == FundsInstructionType.BALANCE_CONTROL
                    && transactionType == BALANCE_CONTROL;
            case BALANCE_ADJUST, LIMIT_ADJUST -> instructionType == FundsInstructionType.BALANCE_CONTROL
                    && transactionType == ADJUSTMENT;
        };
    }
}
