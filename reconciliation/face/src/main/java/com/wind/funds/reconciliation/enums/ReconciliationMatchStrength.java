package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账匹配强度。
 *
 * <p>职责：说明内部事实和外部来源是否可以自动对平，或只能进入候选、人工或未匹配。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationMatchStrength implements DescriptiveEnum {

    /**
     * 完全匹配，可自动对平。
     */
    EXACT_MATCH("完全匹配"),

    /**
     * 按已发布规则匹配，可自动对平。
     */
    RULE_MATCH("规则匹配"),

    /**
     * 候选匹配，不得自动对平。
     */
    CANDIDATE_MATCH("候选匹配"),

    /**
     * 人工确认可解释，不等于自动对平。
     */
    MANUAL_CONFIRMED("人工确认"),

    /**
     * 未匹配。
     */
    UNMATCHED("未匹配");

    private final String desc;
}
