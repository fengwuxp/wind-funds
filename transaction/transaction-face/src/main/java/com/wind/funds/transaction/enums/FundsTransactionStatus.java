package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易聚合状态。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsTransactionStatus implements DescriptiveEnum {

    PROCESSING("处理中"),
    OPEN("处理中但生命周期未关闭"),
    CLOSED("已关闭"),
    EXPIRED("已过期"),
    FAILED("失败"),
    REJECTED("已拒绝");

    private final String desc;
}
