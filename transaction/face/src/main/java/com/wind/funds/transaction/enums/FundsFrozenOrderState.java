package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金冻结单状态。
 *
 * @author wuxp
 * @since 2026-05-07
 */
@Schema(description = "资金冻结单生命周期状态")
@AllArgsConstructor
@Getter
public enum FundsFrozenOrderState implements DescriptiveEnum {

    CREATED("已创建"),
    FROZEN("已冻结"),
    PARTIALLY_RELEASED("部分释放"),
    RELEASED("已释放"),
    EXPIRED("已过期"),
    CLOSED("已关闭");

    private final String desc;
}
