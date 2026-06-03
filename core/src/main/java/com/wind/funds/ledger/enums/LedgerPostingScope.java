package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记账计划影响范围。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum LedgerPostingScope implements DescriptiveEnum {

    WITHIN_SUBJECT("同主体余额桶迁移"),

    BETWEEN_SUBJECTS("跨主体转移"),

    CONTROL_HOLD("控制主体占用"),

    CONTROL_CONSUME("控制主体消耗"),

    PLATFORM_EXTERNAL("平台对外资金路径"),

    FEE("手续费"),

    ADJUSTMENT("调账或调额");

    private final String desc;
}
