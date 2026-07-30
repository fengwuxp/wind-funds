package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayoutOperationStatus implements DescriptiveEnum {

    SUBMIT_ALLOWED("允许提交"),
    WAITING_EXTERNAL_RESULT("等待外部结果"),
    NO_ACTION_REQUIRED("无需操作"),
    REVIEW_REQUIRED("需要核对");

    private final String desc;
}
