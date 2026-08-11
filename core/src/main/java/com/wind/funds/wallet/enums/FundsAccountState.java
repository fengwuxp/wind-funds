package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金账户状态枚举
 *
 * @author wuxp
 * @since 2026-04-13
 **/
@Schema(description = "资金账户生命周期状态")
@AllArgsConstructor
@Getter
public enum FundsAccountState implements DescriptiveEnum {

    ACTIVE("已激活"),

    FROZEN("已冻结"),

    SUSPENDED("风控/合规"),

    CLOSED("已关闭");

    private final String desc;

    /**
     * 是否可以出账（借记）
     *
     * @return 是否可以出账
     */
    public boolean canDebit() {
        return this == ACTIVE;
    }

    /**
     * 是否允许入账（贷记）
     *
     * @return 是否允许入账
     */
    public boolean canCredit() {
        return this == ACTIVE || this == FROZEN;
    }

    /**
     * 是否允许承接退款类收口入账。
     *
     * <p>冻结或挂起不消灭既有退款义务；关闭账户不得被退款自动重开。</p>
     *
     * @return 是否允许承接退款
     */
    public boolean canAcceptRefund() {
        return this != CLOSED;
    }
}
