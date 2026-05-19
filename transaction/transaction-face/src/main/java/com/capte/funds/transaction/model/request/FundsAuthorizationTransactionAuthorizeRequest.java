package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.InternationalRegionCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

/**
 * 授权交易授权请求
 *
 * @author wuxp
 * @date 2026-04-21 08:58
 **/
@Schema(description = "授权交易授权请求")
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class FundsAuthorizationTransactionAuthorizeRequest {

    @Schema(description = "账户 id")
    @NonNull
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "授权交易金额")
    @NonNull
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "业务流水号（如果是卡交易则为授权交易流水号）")
    @NonNull
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NonNull
    @NotNull
    private String businessScene;

    @Schema(description = "授权是否通过")
    @NonNull
    @NotNull
    private Boolean approved;

    @Schema(description = "交易授权时间")
    private LocalDateTime authorizedTime;

    @Schema(description = "交易国家或地区")
    private InternationalRegionCode transactionCountry;

    @Schema(description = "商户信息")
    private MerchantInfoRequest merchantInfo;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "授权拒绝原因，仅授权失败时需要，不表示结算后的拒付/争议")
    private String declineReason;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;
}
