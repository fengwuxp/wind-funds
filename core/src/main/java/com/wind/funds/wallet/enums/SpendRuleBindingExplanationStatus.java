package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支出规则挂载解释状态。
 *
 * @author wuxp
 * @since 2026-06-22
 */
@Schema(description = "支出规则挂载解释展示状态")
@AllArgsConstructor
@Getter
public enum SpendRuleBindingExplanationStatus implements DescriptiveEnum {

    EFFECTIVE("当前有效"),
    NOT_YET_EFFECTIVE("尚未生效"),
    EXPIRED("已过期"),
    SUSPENDED("已暂停"),
    RETIRED("已退役");

    private final String desc;
}
