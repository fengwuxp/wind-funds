package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SettlementTriggerMode implements DescriptiveEnum {

    HOST_COMMAND("宿主命令");

    private final String desc;
}
