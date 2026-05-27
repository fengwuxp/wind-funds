package com.capte.funds.ledger.request;

import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.model.FundsContextVariables;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 账户账本交易
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class UpdateLedgerTransactionRequest {

    @Schema(description = "")
    @NotNull
    private Long id;

    @Schema(description = "交易状态")
    private LedgerTransactionStatus status;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private Map<String, Object> contextVariable;

    public UpdateLedgerTransactionRequest setContextVariable(Map<String, Object> contextVariable) {
        this.contextVariable = contextVariable == null ? null : FundsContextVariables.immutableCopy(contextVariable);
        return this;
    }

}
