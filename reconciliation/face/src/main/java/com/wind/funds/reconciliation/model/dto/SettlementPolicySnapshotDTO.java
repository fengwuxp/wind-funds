package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class SettlementPolicySnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6630849547493475742L;

    private String policyCode;

    private String policyVersion;

    private SettlementMode settlementMode;

    private SettlementDestination settlementDestination;

    private SettlementTriggerMode triggerMode;

    private String settlementPeriod;

    private String timezone;

    private String cutoff;

    private String policyApprovalRef;
}
