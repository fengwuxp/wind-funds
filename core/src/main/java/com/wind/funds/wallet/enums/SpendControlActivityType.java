package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支出控制活动类型。
 *
 * @author Codex
 * @date 2026-06-20
 */
@AllArgsConstructor
@Getter
public enum SpendControlActivityType implements DescriptiveEnum {

    ADMISSION_RECORDED("准入已记录"),

    REJECTED_RECORDED("拒绝已记录"),

    RESERVED("控制占用"),

    CONSUMED("控制消耗"),

    REFUND_COMPENSATED("退款控制补偿"),

    RELEASED("控制释放"),

    EXPIRED("控制过期"),

    REVERSED("控制撤销");

    private final String desc;
}
