package com.wind.funds.reconciliation.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class CancelSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5400604943092953095L;

    public static final int MAX_REASON_LENGTH = 512;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String settlementOrderSn;

    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;
}
