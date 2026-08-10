package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款单当前允许的操作状态。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum PayoutOperationStatus implements DescriptiveEnum {

    /** 允许首次提交到外部通道。 */
    SUBMIT_ALLOWED("允许提交"),
    /** 已提交，等待外部通道终态。 */
    WAITING_EXTERNAL_RESULT("等待外部结果"),
    /** 已进入终态，无需继续操作。 */
    NO_ACTION_REQUIRED("无需操作"),
    /** 状态不明或回执不一致，需要人工核对。 */
    REVIEW_REQUIRED("需要核对");

    private final String desc;
}
