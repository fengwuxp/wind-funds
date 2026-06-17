package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.SourceObjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

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

    @Schema(description = "是否增加余额或额度，true: 增加，false: 减少")
    @NotNull
    private Boolean increase;

    @Schema(description = "业务流水号，调整凭证，例如：余额调账单流水号、额度调整单流水号")
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

    @Schema(description = "调账来源事实类型，例如：余额调整、对账差错调账、外部余额异常")
    private SourceObjectType sourceType;

    @Schema(description = "调账来源事实流水号，例如：差错单、外部异常单或运营调账单流水号")
    private String sourceSn;

    @Schema(description = "调账原因码，用于报表、风控、对账和审计归类")
    private String reasonCode;

    @Schema(description = "外部机构、钱包、Issuer 或 Processor 的脱敏引用")
    private String externalInstitutionRef;

    @Schema(description = "外部账户、支付工具或通道端点的脱敏引用")
    private String externalAccountRef;

    @Schema(description = "外部终局事件、终局回单、清算文件或处理商最终事实引用")
    private String externalFinalEventRef;

    @Schema(description = "外部余额快照引用，用于证明调账时点外部余额事实")
    private String externalBalanceSnapshotRef;

    @Schema(description = "责任归属、追偿、挂账、成本承担或人工处理案件引用")
    private String responsibilityRef;

    @Schema(description = "调账审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "对账差错引用，可空；对账差错调账场景应填写")
    private String reconciliationExceptionRef;

    @Schema(description = "重新对账或复核批次引用，可空；外部余额异常纠偏场景建议填写")
    private String reconciliationRerunRef;

    @Schema(description = "是否允许本次调账形成受控负可用余额")
    private Boolean allowNegativeBalance;

    @Schema(description = "受控负可用策略编码")
    private String negativeAvailablePolicyCode;

    @Schema(description = "受控负可用风险状态")
    private String negativeAvailableRiskStatus;

    @Schema(description = "受控负可用单笔上限")
    private Money negativeAvailableSingleLimit;

    @Schema(description = "受控负可用累计上限")
    private Money negativeAvailableCumulativeLimit;

    @Schema(description = "受控负可用账龄起点")
    private LocalDateTime negativeAvailableAgingStartedAt;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
