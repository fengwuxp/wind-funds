package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 钱包授权交易完成请求
 *
 * @author wuxp
 * @date 2026-04-21 09:02
 **/
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权交易完成请求")
public class FundsAuthorizationTransactionCompleteRequest {

    public static final String COMPLETION_MODE_FORCE = "FORCE";

    @Schema(description = "普通完成必须为原授权主账户；强制完成时为实际扣款账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "本次完成金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "本次完成业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号，普通完成必填，强制完成为空")
    private String authorizationTransactionSn;

    @Schema(description = "完成模式，FORCE 表示无内部授权事实的强制完成")
    private String completionMode;

    @Schema(description = "强制完成策略编码，completionMode=FORCE 时必填")
    private String forceCompletionPolicyCode;

    @Schema(description = "强制完成单笔上限金额，completionMode=FORCE 时必填，币种与 transactionAmount 一致")
    private Long forceCompletionLimitAmount;

    @Schema(description = "强制完成原因，completionMode=FORCE 时必填")
    private String forceCompletionReason;

    @Schema(description = "外部原始事实引用，completionMode=FORCE 时必填，不承载完整原始报文或敏感数据")
    private String externalOriginalFactRef;

    @Schema(description = "强制完成操作凭证引用，completionMode=FORCE 时必填")
    private String forceCompletionVoucherRef;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;

    public boolean isForceCompletion() {
        return StringUtils.hasText(completionMode) && COMPLETION_MODE_FORCE.equalsIgnoreCase(completionMode);
    }
}
