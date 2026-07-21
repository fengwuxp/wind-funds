package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本入账准入类型。
 *
 * @author Codex
 * @date 2026-07-09
 */
@AllArgsConstructor
@Getter
public enum LedgerPostingAccessType implements DescriptiveEnum {

    /**
     * 普通新增入账。
     */
    NORMAL("普通入账"),

    /**
     * 关闭收口入账。
     */
    CLOSING("收口入账");

    private final String desc;

    public static LedgerPostingAccessType fromIntent(LedgerPostingIntentType intent) {
        if (intent == null) {
            return NORMAL;
        }
        return switch (intent) {
            case AUTHORIZATION_REVERSAL,
                    AUTHORIZATION_COMPLETION,
                    SETTLEMENT,
                    REFUND,
                    REVERSAL,
                    ADJUSTMENT,
                    FEE_REFUND,
                    FEE_REVERSAL -> CLOSING;
            default -> NORMAL;
        };
    }
}
