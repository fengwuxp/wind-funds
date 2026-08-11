package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算释放所依赖批次谱系的取代状态。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算释放批次谱系取代状态")
@Getter
@AllArgsConstructor
public enum SettlementReleaseLineageSupersessionStatus implements DescriptiveEnum {

    /**
     * 当前批次仍是有效谱系节点。
     */
    CURRENT("当前批次仍然有效"),

    /**
     * 当前批次已被后续批次取代。
     */
    SUPERSEDED("当前批次已被取代"),

    /**
     * 无法确认批次取代状态。
     */
    UNKNOWN("批次取代状态未知");

    private final String desc;
}
