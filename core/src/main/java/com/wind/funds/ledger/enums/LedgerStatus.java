package com.wind.funds.ledger.enums;

import com.wind.common.exception.AssertUtils;
import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本状态。
 *
 * @author Codex
 * @date 2026-07-09
 */
@AllArgsConstructor
@Getter
public enum LedgerStatus implements DescriptiveEnum {

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

    public static void assertPostable(Long ledgerId, LedgerStatus status) {
        assertPostable(ledgerId, status, LedgerPostingAccessType.NORMAL);
    }

    public static void assertPostable(Long ledgerId, LedgerStatus status, LedgerPostingAccessType accessType) {
        AssertUtils.notNull(accessType, "账本入账准入类型不能为空");
        boolean allowed = switch (accessType) {
            case NORMAL -> status != null && status.isOpenForNewPosting();
            case CLOSING -> status != null && status.isOpenForClosingPosting();
        };
        AssertUtils.isTrue(allowed,
                "账本状态不允许入账，ledgerId = {}, status = {}, postingAccessType = {}",
                ledgerId,
                status,
                accessType);
    }

    public static void assertTransitionAllowed(Long ledgerId, LedgerStatus currentStatus, LedgerStatus targetStatus) {
        AssertUtils.notNull(targetStatus, "账本目标状态不能为空");
        if (currentStatus == targetStatus) {
            return;
        }
        boolean activeToTerminal = currentStatus == ACTIVE
                && (targetStatus == SUSPENDED || targetStatus == CLOSED);
        boolean suspendedToActive = currentStatus == SUSPENDED && targetStatus == ACTIVE;
        boolean suspendedToClosed = currentStatus == SUSPENDED && targetStatus == CLOSED;
        boolean allowed = activeToTerminal || suspendedToActive || suspendedToClosed;
        AssertUtils.isTrue(allowed,
                "账本状态流转不允许，ledgerId = {}, currentStatus = {}, targetStatus = {}",
                ledgerId,
                currentStatus,
                targetStatus);
    }
}
