package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class HandlePayoutReceiptRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5779825888828459962L;

    @NotNull
    private Long tenantId;

    @NotBlank
    private String payoutOrderSn;

    @NotBlank
    private String channelRef;

    @NotBlank
    private String externalReceiptRef;

    @NotBlank
    private String externalReference;

    @NotNull
    private PayoutOrderStatus status;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotBlank
    private String sourceReceiptDigest;

    @NotBlank
    private String evidenceRef;

    @NotNull
    private LocalDateTime externalOccurredAt;

    private String failureCode;

    private String failureReason;
}
