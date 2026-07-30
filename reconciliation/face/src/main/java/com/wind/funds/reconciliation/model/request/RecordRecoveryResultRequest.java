package com.wind.funds.reconciliation.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class RecordRecoveryResultRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5067142681553565060L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String recoveryOrderSn;

    @NotBlank
    private String fundsTransactionSn;

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    private String approvalRef;

    @NotBlank
    private String evidenceRef;
}
