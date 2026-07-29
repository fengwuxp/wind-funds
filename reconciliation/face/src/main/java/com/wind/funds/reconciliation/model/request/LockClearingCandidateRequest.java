package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 将清算候选锁定到清算批次请求。
 *
 * <p>锁定只占用候选，清算批次仍必须在自己的资金事务中重新执行权威 Gate 并确认资金事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LockClearingCandidateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -8844038019000829829L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算候选流水号")
    @NotBlank
    private String candidateSn;

    @Schema(description = "清算批次流水号")
    @NotBlank
    private String clearingBatchSn;
}
