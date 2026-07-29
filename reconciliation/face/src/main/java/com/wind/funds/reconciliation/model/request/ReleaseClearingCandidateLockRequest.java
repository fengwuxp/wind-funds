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
 * 释放清算候选批次锁定请求。
 *
 * <p>只适用于清算批次已经确定性撤回且尚未形成清算资金事实的场景。外部调用结果未知时不得调用，候选必须继续保持 LOCKED。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReleaseClearingCandidateLockRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -6046953342277785067L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算候选流水号")
    @NotBlank
    private String candidateSn;

    @Schema(description = "原锁定清算批次流水号")
    @NotBlank
    private String clearingBatchSn;
}
