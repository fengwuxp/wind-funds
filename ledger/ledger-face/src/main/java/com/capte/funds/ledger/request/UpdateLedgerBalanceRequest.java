package com.capte.funds.ledger.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 账户账本余额
 *
 * @author wuxp
 * @since 2026-04-24
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UpdateLedgerBalanceRequest {

    @Schema(description = "")
    @NotNull
    private Long id;

    @Schema(description = "借方累计发生额差值，单位：分")
    private Long debitAmountDelta;

    @Schema(description = "贷方累计发生额差值，单位：分")
    private Long creditAmountDelta;

    @Schema(description = "本次更新后的最小正常余额，null 表示不额外限制")
    private Long minimumNormalBalance;
}
