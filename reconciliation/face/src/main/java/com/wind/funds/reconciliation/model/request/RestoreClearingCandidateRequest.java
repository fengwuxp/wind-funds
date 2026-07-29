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
 * 重新评估清算候选请求。
 *
 * <p>恢复只重新计算账期和对账 Gate，不改变来源清分快照，也不产生资金事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RestoreClearingCandidateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2542012497248924837L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算候选流水号")
    @NotBlank
    private String candidateSn;
}
