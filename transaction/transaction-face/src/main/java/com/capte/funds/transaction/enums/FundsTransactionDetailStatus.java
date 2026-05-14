package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易明细处理状态。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsTransactionDetailStatus implements DescriptiveEnum {

    PROCESSING("处理中"),
    SUCCEEDED("成功"),
    FAILED("失败"),
    REJECTED("已拒绝");

    private final String desc;
}
