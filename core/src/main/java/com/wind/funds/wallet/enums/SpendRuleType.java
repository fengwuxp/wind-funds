package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 规则类型。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleType implements DescriptiveEnum {

    AMOUNT_LIMIT("金额限额"),
    COUNT_LIMIT("笔数限额"),
    MERCHANT_CATEGORY("商户类别控制"),
    MERCHANT_ID("商户标识控制"),
    COUNTRY("国家或地区控制"),
    CARD_DATA_INPUT_CAPABILITY("卡数据输入能力控制"),
    CARD_TRANSACTION_PROCESSING_TYPE("卡交易处理类型控制"),
    CVV_REQUIRED("CVV 必填控制"),
    PAN_ENTRY_MODE("PAN 录入方式控制"),
    POINT_OF_SERVICE_CATEGORY("POS 类别控制"),
    CURRENCY("币种控制"),
    TIME_WINDOW("时间窗口控制"),
    COMPOSITE("组合规则");

    private final String desc;
}
