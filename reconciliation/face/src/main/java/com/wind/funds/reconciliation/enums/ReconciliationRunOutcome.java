package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账运行结果。
 *
 * <p>只有 {@link #BALANCED} 是可被后续清分、结算或出款准入消费的正向证据。</p>
 *
 * @author wuxp
 * @since 2026-07-21
 */
@Schema(description = "对账运行结果")
@Getter
@AllArgsConstructor
public enum ReconciliationRunOutcome implements DescriptiveEnum {

    /**
     * 已完成匹配且全部对平。
     */
    BALANCED("全部对平"),

    /**
     * 已完成匹配并发现差错。
     */
    DIFFERENCE_FOUND("发现差错");

    private final String desc;
}
