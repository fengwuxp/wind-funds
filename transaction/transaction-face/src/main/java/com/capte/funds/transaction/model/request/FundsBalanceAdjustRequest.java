package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 资金余额调整请求
 *
 * @author wuxp
 * @date 2026-04-21 09:06
 **/
@Data
@Accessors(chain = true)
public class FundsBalanceAdjustRequest {

    @Schema(description = "调账账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "调账金额")
    @NotNull
    private Money amount;

    @Schema(description = "是否为为增加额度，true: 增加额度，false: 减少额度")
    @NotNull
    private Boolean increase;

    @Schema(description = "业务流水号，调额凭证，例如：调整余额申请单流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "调账原因")
    @NotBlank
    private String adjustReason;

    @Schema(description = "调账凭证或证据引用，例如：审批附件、财务凭证或外部证据流水")
    @NotBlank
    private String adjustEvidenceRef;

    @Schema(description = "调账审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "对账差错引用，可空；对账差错调账场景应填写")
    private String reconciliationExceptionRef;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;
}
