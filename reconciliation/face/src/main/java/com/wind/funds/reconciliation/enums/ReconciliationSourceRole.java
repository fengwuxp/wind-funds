package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账来源角色。
 *
 * <p>使用基准侧和核对侧，而不是内部和外部，以支持账账、账实及外部报表对账。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationSourceRole implements DescriptiveEnum {

    REFERENCE("基准侧"),
    COMPARISON("核对侧");

    private final String desc;
}
