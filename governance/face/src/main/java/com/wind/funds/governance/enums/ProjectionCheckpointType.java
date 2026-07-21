package com.wind.funds.governance.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易投影重放检查点类型。
 *
 * <p>职责：标识交易投影重放使用的 checkpoint 域，避免调用方传入无类型的处理水位。</p>
 *
 * <p>能力：为重放请求校验提供明确枚举，保证交易投影重放只消费交易投影自己的处理边界。</p>
 *
 * <p>边界：该枚举只表达交易投影自身的 checkpoint 类型；余额、归档、报表等其他域应定义自己的契约。</p>
 */
@AllArgsConstructor
@Getter
public enum ProjectionCheckpointType implements DescriptiveEnum {

    TRANSACTION_PROJECTION("交易投影");

    private final String desc;
}
