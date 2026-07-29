package com.wind.funds.transaction.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 清算确认资金请求。
 */
@Data
@Accessors(chain = true)
public class FundsClearingConfirmRequest {

    @Schema(description = "清算资金账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "清算确认金额；必须与账户币种一致")
    @NotNull
    private Money amount;

    @Schema(description = "已确认的清算批次流水号，也是本次资金事实的业务幂等键")
    @NotNull
    private String clearingBatchSn;

    @Schema(description = "本批次覆盖的去重来源资金交易流水号；交易层按固定顺序锁定并复核，防止与退款并发冲突")
    @NotEmpty
    private List<String> sourceTransactionSns;

    @Schema(description = "交易描述")
    private String description;
}
