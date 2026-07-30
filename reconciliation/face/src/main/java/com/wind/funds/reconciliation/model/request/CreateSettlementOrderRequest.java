package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class CreateSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3743281674326227367L;

    public static final int MAX_CLEARING_BATCH_COUNT = 1000;

    @NotNull
    private Long tenantId;

    @NotEmpty
    @Size(max = MAX_CLEARING_BATCH_COUNT)
    private List<String> clearingBatchSns;

    @NotBlank
    private String settlementPeriod;

    @NotNull
    private SettlementMode settlementMode;

    @NotNull
    private SettlementDestination settlementDestination;

    @NotNull
    private SettlementTriggerMode triggerMode;

    @NotBlank
    private String timezone;

    @NotBlank
    private String cutoff;

    @NotBlank
    private String policyCode;

    @NotBlank
    private String policyVersion;

    private String policyApprovalRef;
}
