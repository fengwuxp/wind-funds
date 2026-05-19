package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 交易投影重放范围，是限制本次重放影响面的范围契约。
 *
 * <p>职责：用单笔来源单号、主体、时间窗口或批次描述需要读取和重建的交易投影事实边界。</p>
 *
 * <p>能力：支持按单笔排查、按主体修复、按时间窗口重放和按批次重放，并通过 {@link #isBounded()}
 * 阻止无范围的全量误操作。</p>
 *
 * <p>边界：该范围只用于限定投影重放读取面，不承担权限控制、业务筛选规则或事实修正语义。</p>
 */
@Builder
public record FundsTransactionProjectionReplayRange(@Nullable String sourceSn,
                                                    @Nullable String ownerType,
                                                    @Nullable String ownerId,
                                                    @Nullable LocalDateTime startTime,
                                                    @Nullable LocalDateTime endTime,
                                                    @Nullable String batchType,
                                                    @Nullable String batchSn) {

    public boolean isBounded() {
        return hasText(sourceSn)
                || (hasText(ownerType) && hasText(ownerId))
                || (startTime != null && endTime != null && startTime.isBefore(endTime))
                || (hasText(batchType) && hasText(batchSn));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
