package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.RecoveryOrderStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class RecoveryOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3927603104028972332L;

    private String sn;

    private Long tenantId;

    private String sourceType;

    private String sourceSn;

    private String responsibleSubjectType;

    private String responsibleSubjectId;

    private Long expectedAmount;

    private Long recoveredAmount;

    private Long remainingAmount;

    private CurrencyIsoCode currency;

    private RecoveryOrderStatus status;

    private String lastFundsTransactionSn;

    private LocalDateTime recoveredTime;
}
