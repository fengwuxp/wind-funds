package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账准入决策结果。
 *
 * <p>职责：表达清算、结算或出款消费方是否可以继续推进。</p>
 *
 * <p>边界：结果只代表准入判断，不表达清算批次、结算单、出款单或账务事实生命周期。</p>
 *
 * @author wuxp
 * @since 2026-06-18
 */
@Schema(description = "对账准入决策结果")
@Getter
@AllArgsConstructor
public enum ReconciliationGateDecisionResult implements DescriptiveEnum {

    /**
     * 未发现相关阻断差错，准入通过。
     */
    PASSED("通过"),

    /**
     * 存在未闭环或未对平差错，必须阻断。
     */
    BLOCKED("阻断");

    private final String desc;
}
