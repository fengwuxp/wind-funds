package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算锁定资金的释放去向。
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Getter
@AllArgsConstructor
@Schema(description = "结算锁定资金释放去向")
public enum SettlementReleaseDisposition implements DescriptiveEnum {

    /**
     * 将结算锁定资金释放到同一主体的冻结余额。
     */
    FROZEN("释放到冻结余额");

    private final String desc;
}
