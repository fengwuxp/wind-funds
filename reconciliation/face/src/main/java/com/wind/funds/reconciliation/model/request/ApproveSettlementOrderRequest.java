package com.wind.funds.reconciliation.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ApproveSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3920531559624810509L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String settlementOrderSn;

    @NotBlank
    private String settlementApprovalRef;
}
