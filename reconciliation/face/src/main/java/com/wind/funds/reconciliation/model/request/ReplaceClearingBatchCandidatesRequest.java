package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 替换清算批次草稿候选请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReplaceClearingBatchCandidatesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3596408652681848043L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算批次流水号")
    @NotBlank
    private String clearingBatchSn;

    @Schema(description = "替换后的清算候选流水号列表")
    @NotEmpty
    @Size(max = CreateClearingBatchRequest.MAX_CANDIDATE_COUNT)
    private List<String> candidateSns;
}
