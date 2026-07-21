package com.wind.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路径回放类型。
 */
@AllArgsConstructor
@Getter
public enum RouteReplayType implements DescriptiveEnum {

    RELEASE_HOLD("释放占用"),

    AUTHORIZATION_COMPLETION("授权完成"),

    AUTHORIZATION_REFUND("授权退款"),

    REFUND("退款"),

    FEE_REFUND("费用退款"),

    UNFREEZE("解冻");

    private final String desc;
}
