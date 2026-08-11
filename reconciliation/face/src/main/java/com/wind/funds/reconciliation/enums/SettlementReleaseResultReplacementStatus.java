package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算释放所依赖结果的替代状态。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算释放结果替代状态")
@Getter
@AllArgsConstructor
public enum SettlementReleaseResultReplacementStatus implements DescriptiveEnum {

    /**
     * 当前结果未被新结果替代。
     */
    CURRENT("当前结果未被替代"),

    /**
     * 当前结果已经被新结果替代。
     */
    REPLACED("当前结果已被替代"),

    /**
     * 无法确认结果替代状态。
     */
    UNKNOWN("结果替代状态未知");

    private final String desc;
}
