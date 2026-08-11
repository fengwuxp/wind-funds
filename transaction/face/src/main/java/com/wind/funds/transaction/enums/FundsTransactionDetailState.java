package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易明细处理状态。
 *
 * @author wuxp
 * @since 2026-05-07
 */
@Schema(description = "资金交易明细处理状态")
@AllArgsConstructor
@Getter
public enum FundsTransactionDetailState implements DescriptiveEnum {

    PROCESSING("处理中"),
    SUCCEEDED("成功"),
    FAILED("失败"),
    REJECTED("已拒绝");

    private final String desc;
}
