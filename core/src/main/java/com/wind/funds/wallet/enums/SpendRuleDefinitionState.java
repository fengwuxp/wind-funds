package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 定义状态。
 *
 * @author wuxp
 * @since 2026-06-22
 */
@Schema(description = "Spend Rule 定义生命周期状态")
@AllArgsConstructor
@Getter
public enum SpendRuleDefinitionState implements DescriptiveEnum {

    ACTIVE("生效"),
    DISABLED("停用");

    private final String desc;
}
