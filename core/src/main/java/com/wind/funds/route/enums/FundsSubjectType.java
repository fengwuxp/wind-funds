package com.wind.funds.route.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金主体类型。
 *
 * <p>资金账户和信用账户是可入账的核心账务主体；预算组保留为预算控制兼容主体，
 * 不得作为 route leg、posting plan、ledger entry 或余额投影的可入账主体。
 */
@AllArgsConstructor
@Getter
public enum FundsSubjectType implements DescriptiveEnum {

    FUNDING_ACCOUNT("资金账户"),

    CREDIT_ACCOUNT("信用账户"),

    BUDGET_GROUP("预算组");

    private final String desc;

    /**
     * 是否允许作为核心账本可入账主体。
     *
     * @return 允许写入账本分录和余额投影返回 {@code true}，控制视图或兼容主体返回 {@code false}
     */
    public boolean isLedgerPostable() {
        return this == FUNDING_ACCOUNT || this == CREDIT_ACCOUNT;
    }

    /**
     * 是否仅用于预算、规则或控制视图。
     *
     * @return 仅用于控制视图且不允许入账时返回 {@code true}
     */
    public boolean isControlScopeOnly() {
        return this == BUDGET_GROUP;
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
