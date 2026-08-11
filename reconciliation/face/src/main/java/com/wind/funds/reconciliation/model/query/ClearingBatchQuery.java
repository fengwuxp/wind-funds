package com.wind.funds.reconciliation.model.query;

import com.wind.funds.reconciliation.enums.ClearingBatchState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 清算批次查询条件。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingBatchQuery {

    @Schema(description = "租户 ID；为空时使用当前租户隔离上下文")
    private Long tenantId;

    @Schema(description = "批次状态")
    private ClearingBatchState state;

    @Schema(description = "最大修改时间，包含边界")
    private LocalDateTime gmtModifiedMax;
}
