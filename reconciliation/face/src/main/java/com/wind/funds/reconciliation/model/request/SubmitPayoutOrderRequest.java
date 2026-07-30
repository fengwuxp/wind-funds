package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class SubmitPayoutOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2716102830871511513L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String payoutOrderSn;

    @NotBlank
    private String payoutAccountRef;

    @NotBlank
    private String payeeEndpointRef;

    @NotBlank
    private String channelRef;

    @NotBlank
    private String approvalRef;

    @Valid
    @NotNull
    private ExternalRuleVerificationEvidenceDTO externalRuleVerificationEvidence;

    @NotBlank
    private String reconciliationRunResultSn;
}
