package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算释放所依赖来源的收齐状态。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算释放来源收齐状态")
@Getter
@AllArgsConstructor
public enum SettlementReleaseCoverageStatus implements DescriptiveEnum {

    /**
     * 必需来源已经完整收齐。
     */
    COMPLETE("来源已完整收齐"),

    /**
     * 必需来源尚未完整收齐。
     */
    INCOMPLETE("来源尚未完整收齐"),

    /**
     * 无法确认必需来源是否收齐。
     */
    UNKNOWN("来源收齐状态未知");

    private final String desc;
}
