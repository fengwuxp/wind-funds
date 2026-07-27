package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出款前准入操作状态。
 *
 * <p>职责：表达调用方是否仍需在真实出款提交命令中重新校验完整准入条件。</p>
 */
@AllArgsConstructor
@Getter
public enum PayoutPreflightOperationStatus implements DescriptiveEnum {

    /**
     * 当前证据预检通过，但真实提交前仍需重新校验。
     */
    SUBMISSION_REVALIDATION_REQUIRED("提交前需重新校验"),

    /**
     * 已阻断。
     */
    BLOCKED("已阻断");

    private final String desc;
}
