package com.wind.funds.reconciliation.model.query;

import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 清算候选查询条件。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingCandidateQuery {

    @Schema(description = "租户 ID；为空时使用当前租户隔离上下文")
    private Long tenantId;

    @Schema(description = "候选状态")
    private ClearingCandidateState state;

    @Schema(description = "最晚可清算时间，包含边界")
    private LocalDateTime clearingAvailableTimeMax;

    @Schema(description = "最大状态变更时间，包含边界")
    private LocalDateTime stateChangedTimeMax;

    @Schema(description = "当前锁定清算批次号")
    private String lockedClearingBatchSn;
}
