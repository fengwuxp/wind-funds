package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算单生命周期状态。
 *
 * @author wuxp
 * @since 2026-07-29
 */
@Schema(description = "结算单生命周期状态")
@Getter
@AllArgsConstructor
public enum SettlementOrderState implements DescriptiveEnum {

    /** 结算单草稿。 */
    DRAFT("草稿"),
    /** 结算单等待复核。 */
    REVIEWING("复核中"),
    /** 结算单已经审批。 */
    APPROVED("已审批"),
    /** 结算资金已经锁定。 */
    LOCKED("资金已锁定"),
    /** 结算资金锁定失败。 */
    FAILED("锁定失败"),
    /** 结算单已经取消。 */
    CANCELLED("已取消");

    private final String desc;
}
