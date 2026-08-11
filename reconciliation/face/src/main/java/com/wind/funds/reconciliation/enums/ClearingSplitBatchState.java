package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 清分批次状态。
 *
 * <p>{@code CONFIRMED} 表示清分结果快照已冻结，是清分批次成功终态，
 * 但不代表后续清算入账已经发生。</p>
 *
 * @author wuxp
 * @since 2026-07-29
 */
@Schema(description = "清分批次生命周期状态")
@Getter
@AllArgsConstructor
public enum ClearingSplitBatchState implements DescriptiveEnum {

    DRAFT("草稿"),

    REVIEWING("复核中"),

    CONFIRMED("已确认"),

    CANCELLED("已取消");

    private final String desc;
}
