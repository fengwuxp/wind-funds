package com.wind.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金主体类型。
 *
 * <p>仅表达可进入 route leg、posting plan、ledger entry 和余额投影的核心账务主体。
 * 支出控制范围属于 Spend Rule / Spend Control 的控制范围对象，不是资金主体。
 */
@AllArgsConstructor
@Getter
public enum FundsSubjectType implements DescriptiveEnum {

    FUNDING_ACCOUNT("资金账户"),

    CREDIT_ACCOUNT("信用账户");

    private final String desc;

    /**
     * 是否允许作为核心账本可入账主体。
     *
     * @return 当前枚举均为可入账主体
     */
    public boolean isLedgerPostable() {
        return this == FUNDING_ACCOUNT || this == CREDIT_ACCOUNT;
    }

    /**
     * 按枚举名称判断主体类型是否允许进入核心账本。
     *
     * @param subjectTypeName 主体类型名称
     * @return 名称对应可入账主体时返回 {@code true}，未知名称、空值或控制视图主体返回 {@code false}
     */
    public static boolean isLedgerPostableName(String subjectTypeName) {
        for (FundsSubjectType subjectType : values()) {
            if (subjectType.name().equals(subjectTypeName)) {
                return subjectType.isLedgerPostable();
            }
        }
        return false;
    }
}
