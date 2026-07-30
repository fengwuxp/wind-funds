package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.SettlementOrderStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class SettlementOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -878598498433192870L;

    private String sn;

    private Long tenantId;

    private String settlementSubjectType;

    private String settlementSubjectId;

    private CurrencyIsoCode currency;

    private String settlementPeriod;

    private Long netAmount;

    private SettlementOrderStatus status;

    private SettlementPolicySnapshotDTO policySnapshot;

    private List<SettlementOrderItemDTO> items;

    private String settlementApprovalRef;

    private String lockFundsTransactionSn;

    private String reconciliationRunResultSn;

    private String reconciliationResultDigest;

    private String reconciliationEvidenceDigest;

    private String amountDigest;

    private String sourceDigest;

    private String policySnapshotDigest;

    private String orderDigest;

    private LocalDateTime createdTime;

    private LocalDateTime submittedTime;

    private LocalDateTime approvedTime;

    private LocalDateTime lockedTime;

    private LocalDateTime returnedTime;

    private LocalDateTime cancelledTime;

    private LocalDateTime failedTime;

    private String reason;
}
