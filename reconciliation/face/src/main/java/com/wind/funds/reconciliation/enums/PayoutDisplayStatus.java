package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面向调用方展示的出款状态。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum PayoutDisplayStatus implements DescriptiveEnum {

    /** 待提交。 */
    PENDING("待提交"),
    /** 已提交且尚未取得终态。 */
    PROCESSING("处理中"),
    /** 外部出款成功。 */
    SUCCEEDED("出款成功"),
    /** 外部出款失败。 */
    FAILED("出款失败"),
    /** 出款后资金被外部退回。 */
    RETURNED("已退回"),
    /** 外部结果无法自动确认，需要人工核对。 */
    REVIEW_REQUIRED("待核对");

    private final String desc;
}
