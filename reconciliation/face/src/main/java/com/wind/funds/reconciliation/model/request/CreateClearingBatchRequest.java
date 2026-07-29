package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * 创建清算批次草稿请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateClearingBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -357174222878473576L;

    public static final int MAX_CANDIDATE_COUNT = 1000;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清算候选流水号列表")
    @NotEmpty
    @Size(max = MAX_CANDIDATE_COUNT)
    private List<String> candidateSns;
}
