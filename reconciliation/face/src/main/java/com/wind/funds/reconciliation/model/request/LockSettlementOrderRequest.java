package com.wind.funds.reconciliation.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class LockSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5578433678370178866L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String settlementOrderSn;

    @NotBlank
    private String reconciliationRunResultSn;
}
