package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayoutOrderStatus implements DescriptiveEnum {

    CREATED("已创建"),
    SUBMITTED("已提交"),
    ACCEPTED("外部已受理"),
    PROCESSING("外部处理中"),
    SUCCEEDED("出款成功"),
    FAILED("出款失败"),
    RETURNED("外部退回"),
    MISMATCHED("回单不一致");

    private final String desc;
}
