package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金交易聚合状态。
 *
 * @author wuxp
 * @since 2026-05-07
 */
@Schema(description = "资金交易聚合生命周期状态")
@AllArgsConstructor
@Getter
public enum FundsTransactionState implements DescriptiveEnum {

    PROCESSING("处理中"),
    OPEN("处理中但生命周期未关闭"),
    CLOSED("已关闭"),
    FAILED("失败"),
    REJECTED("已拒绝");

    private final String desc;
}
