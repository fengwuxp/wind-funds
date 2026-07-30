package com.wind.funds.transaction.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FundsSettlementLockRequest {

    @NotNull
    private FundsAccountId accountId;

    @NotNull
    private Money amount;

    @NotBlank
    private String settlementOrderSn;

    private String description;
}
