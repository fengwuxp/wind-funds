package com.wind.funds.ledger.enums;

import com.wind.common.exception.AssertUtils;
import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本状态。
 *
 * @author wuxp
 * @since 2026-07-09
 */
@Schema(description = "账本生命周期状态")
@AllArgsConstructor
@Getter
public enum LedgerState implements DescriptiveEnum {

    /**
     * 可入账。
     */
    ACTIVE("可入账"),

    /**
     * 已挂起，只允许关闭收口入账。
     */
    SUSPENDED("已挂起"),

    /**
     * 已关账，不允许新增入账。
     */
    CLOSED("已关账");

    private final String desc;

    public boolean isOpenForNewPosting() {
        return this == ACTIVE;
    }

    public boolean isOpenForClosingPosting() {
        return this == ACTIVE || this == SUSPENDED;
    }

    public static void assertPostable(Long ledgerId, LedgerState state) {
        assertPostable(ledgerId, state, LedgerPostingAccessType.NORMAL);
    }

    public static void assertPostable(Long ledgerId, LedgerState state, LedgerPostingAccessType accessType) {
        AssertUtils.notNull(accessType, "账本入账准入类型不能为空");
        boolean allowed = switch (accessType) {
            case NORMAL -> state != null && state.isOpenForNewPosting();
            case CLOSING -> state != null && state.isOpenForClosingPosting();
        };
        AssertUtils.isTrue(allowed,
                "账本状态不允许入账，ledgerId = {}, state = {}, postingAccessType = {}",
                ledgerId,
                state,
                accessType);
    }

    public static void assertTransitionAllowed(Long ledgerId, LedgerState currentState, LedgerState targetState) {
        AssertUtils.notNull(targetState, "账本目标状态不能为空");
        if (currentState == targetState) {
            return;
        }
        boolean activeToTerminal = currentState == ACTIVE
                && (targetState == SUSPENDED || targetState == CLOSED);
        boolean suspendedToActive = currentState == SUSPENDED && targetState == ACTIVE;
        boolean suspendedToClosed = currentState == SUSPENDED && targetState == CLOSED;
        boolean allowed = activeToTerminal || suspendedToActive || suspendedToClosed;
        AssertUtils.isTrue(allowed,
                "账本状态流转不允许，ledgerId = {}, currentState = {}, targetState = {}",
                ledgerId,
                currentState,
                targetState);
    }
}
