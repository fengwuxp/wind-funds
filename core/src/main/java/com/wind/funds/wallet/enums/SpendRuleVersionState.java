package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 版本状态。
 *
 * @author wuxp
 * @since 2026-06-22
 */
@Schema(description = "Spend Rule 版本生命周期状态")
@AllArgsConstructor
@Getter
public enum SpendRuleVersionState implements DescriptiveEnum {

    PUBLISHED("已发布"),
    RETIRED("已退役");

    private final String desc;
}
