package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算释放所依赖来源的迟到数据状态。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算释放迟到数据状态")
@Getter
@AllArgsConstructor
public enum SettlementReleaseLateDataStatus implements DescriptiveEnum {

    /**
     * 迟到数据窗口已经关闭。
     */
    CLOSED("迟到数据窗口已关闭"),

    /**
     * 当前存在尚未处置的迟到数据。
     */
    PRESENT("存在迟到数据"),

    /**
     * 无法确认迟到数据状态。
     */
    UNKNOWN("迟到数据状态未知");

    private final String desc;
}
