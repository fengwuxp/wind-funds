package com.wind.funds.reconciliation.model.request;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class CreateRecoveryOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2089266100494722056L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String sourceType;

    @NotBlank
    private String sourceSn;

    @NotBlank
    private String responsibleSubjectType;

    @NotBlank
    private String responsibleSubjectId;

    @NotNull
    private Long expectedAmount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotBlank
    private String sourceDigest;

    @NotBlank
    private String approvalRef;

    @NotBlank
    private String evidenceRef;
}
