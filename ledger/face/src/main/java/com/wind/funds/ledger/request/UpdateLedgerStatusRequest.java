package com.wind.funds.ledger.request;

import com.wind.funds.ledger.enums.LedgerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 更新账本状态请求。
 *
 * @author Codex
 * @date 2026-07-09
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UpdateLedgerStatusRequest {

    @Schema(description = "账本 ID")
    @NotNull
    private Long id;

    @Schema(description = "目标账本状态")
    @NotNull
    private LedgerStatus status;
}
