package com.capte.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 钱包授权结算后的拒付/争议请求。
 * <p>
 * 用于已结算的授权交易发生持卡人或发卡机构争议后，按原授权结算路径回放资金退回；
 * 不表示授权阶段 approved=false 的授权拒绝。
 *
 * @author wuxp
 * @date 2026-05-12
 **/
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权结算后的拒付/争议请求，用于已结算交易的争议退回，不表示授权阶段的授权拒绝")
public class FundsAuthorizationTransactionChargebackRequest {

    @Schema(description = "账户 id")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "拒付/争议退回金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "本次拒付/争议业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号，用于定位原授权结算链路")
    @NotNull
    private String authorizationTransactionSn;

    @Schema(description = "拒付/争议发生时间")
    private LocalDateTime chargebackTime;

    @Schema(description = "拒付/争议描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
