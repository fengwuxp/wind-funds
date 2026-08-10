package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算单触发模式。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum SettlementTriggerMode implements DescriptiveEnum {

    /** 由宿主系统显式命令触发。 */
    HOST_COMMAND("宿主命令");

    private final String desc;
}
