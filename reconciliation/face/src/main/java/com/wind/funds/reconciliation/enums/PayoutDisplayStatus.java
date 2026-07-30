package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayoutDisplayStatus implements DescriptiveEnum {

    PENDING("待提交"),
    PROCESSING("处理中"),
    SUCCEEDED("出款成功"),
    FAILED("出款失败"),
    RETURNED("已退回"),
    REVIEW_REQUIRED("待核对");

    private final String desc;
}
