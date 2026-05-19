package com.capte.funds.governance.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 交易投影重放差异项，是现有投影与重建投影之间的字段级差异描述。
 *
 * <p>职责：说明哪一笔来源事实的哪个字段出现差异，以及重建期望值和当前实际值分别是什么。</p>
 *
 * <p>能力：支撑差异报告、人工复核、TDD 断言和重放结果审计。</p>
 *
 * <p>边界：差异项只描述问题，不自动触发修复、调账、冲正、清结算补偿或投影覆盖。</p>
 */
@Builder
public record FundsTransactionProjectionDifference(@NonNull String sourceSn,
                                                   @NonNull String fieldName,
                                                   @Nullable Object expectedValue,
                                                   @Nullable Object actualValue) {
}
