package com.capte.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 钱包授权交易结算（完成）请求
 *
 * @author wuxp
 * @date 2026-04-21 09:02
 **/
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权交易结算（完成）请求")
public class FundsAuthorizationTransactionSettleRequest {

    public static final String SETTLE_MODE_FORCE = "FORCE";

    @Schema(description = "账户 id")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "结算交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "本次结算业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号，普通完成必填，强制完成为空")
    private String authorizationTransactionSn;

    @Schema(description = "完成模式，FORCE 表示无内部授权事实的强制完成")
    private String settleMode;

    @Schema(description = "强制完成策略编码，settleMode=FORCE 时必填")
    private String forceSettlePolicyCode;

    @Schema(description = "强制完成单笔上限金额，settleMode=FORCE 时必填，币种与 transactionAmount 一致")
    private Long forceSettleLimitAmount;

    @Schema(description = "强制完成原因，settleMode=FORCE 时必填")
    private String forceSettleReason;

    @Schema(description = "外部原始事实引用，settleMode=FORCE 时必填，不承载完整原始报文或敏感数据")
    private String externalOriginalFactRef;

    @Schema(description = "强制完成操作凭证引用，settleMode=FORCE 时必填")
    private String forceSettleVoucherRef;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;

    public boolean isForceSettle() {
        return StringUtils.hasText(settleMode) && SETTLE_MODE_FORCE.equalsIgnoreCase(settleMode);
    }
}
