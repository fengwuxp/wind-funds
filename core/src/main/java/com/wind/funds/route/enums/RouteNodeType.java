package com.wind.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Route 节点类型。
 */
@AllArgsConstructor
@Getter
public enum RouteNodeType implements DescriptiveEnum {

    SUBJECT("真实资金主体"),

    PLATFORM_FUNDING_ACCOUNT("平台资金账户"),

    EXTERNAL_ACCOUNT("外部账户"),

    PAYMENT_INSTRUMENT("支付工具");

    private final String desc;
}
