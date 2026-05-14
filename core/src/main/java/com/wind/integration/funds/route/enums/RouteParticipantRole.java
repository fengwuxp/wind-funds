package com.wind.integration.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易参与方角色。
 */
@AllArgsConstructor
@Getter
public enum RouteParticipantRole implements DescriptiveEnum {

    PAYER("付款方"),

    PAYEE("收款方"),

    REAL_FUNDING_SOURCE("真实资金来源"),

    AUTH_HOLDER("授权持有人"),

    BUDGET_CONTROLLER("预算控制主体"),

    PLATFORM_FUNDING_ACCOUNT("平台资金主体"),

    EXTERNAL_COUNTERPARTY("外部对手方"),

    FEE_RECEIVER("手续费收款方");

    private final String desc;
}
