package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.PayoutDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutOperationStatus;
import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class PayoutOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2459657973728854434L;

    private String sn;

    private Long tenantId;

    private String settlementOrderSn;

    private String settlementSubjectType;

    private String settlementSubjectId;

    private Long amount;

    private CurrencyIsoCode currency;

    private PayoutOrderStatus factStatus;

    private PayoutDisplayStatus displayStatus;

    private PayoutOperationStatus operationStatus;

    private String payoutAccountRef;

    private String payeeEndpointRef;

    private String channelRef;

    private String externalReference;

    private String reconciliationRunResultSn;

    private String reconciliationResultDigest;

    private String admissionDecisionDigest;

    private List<String> admissionEvidenceRefs;

    private String completionFundsTransactionSn;

    private String rollbackFundsTransactionSn;

    private String lastReceiptDigest;

    private String failureCode;

    private String failureReason;

    private LocalDateTime submittedTime;

    private LocalDateTime completedTime;
}
