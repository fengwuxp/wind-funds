package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 挂载状态。
 *
 * @author wuxp
 * @since 2026-06-22
 */
@Schema(description = "Spend Rule 挂载生命周期状态")
@AllArgsConstructor
@Getter
public enum SpendRuleBindingState implements DescriptiveEnum {

    ACTIVE("生效"),
    SUSPENDED("暂停"),
    RETIRED("退役");

    private final String desc;
}
