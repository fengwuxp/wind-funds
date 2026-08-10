package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.PayoutDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutOperationStatus;
import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出款单公共事实。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "出款单公共事实")
@Data
@Accessors(chain = true)
public class PayoutOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2459657973728854434L;

    @Schema(description = "出款单流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "来源结算单流水号")
    private String settlementOrderSn;

    @Schema(description = "结算主体类型")
    private String settlementSubjectType;

    @Schema(description = "结算主体标识")
    private String settlementSubjectId;

    @Schema(description = "出款金额，最小货币单位")
    private Long amount;

    @Schema(description = "出款币种")
    private CurrencyIsoCode currency;

    @Schema(description = "出款单事实状态")
    private PayoutOrderStatus factStatus;

    @Schema(description = "面向调用方的出款展示状态")
    private PayoutDisplayStatus displayStatus;

    @Schema(description = "当前出款操作状态")
    private PayoutOperationStatus operationStatus;

    @Schema(description = "出款账户稳定引用")
    private String payoutAccountRef;

    @Schema(description = "收款端点稳定引用")
    private String payeeEndpointRef;

    @Schema(description = "出款通道稳定引用")
    private String channelRef;

    @Schema(description = "外部通道业务引用")
    private String externalReference;

    @Schema(description = "出款准入消费的对账运行结果流水号")
    private String reconciliationRunResultSn;

    @Schema(description = "出款准入消费的对账结果摘要")
    private String reconciliationResultDigest;

    @Schema(description = "首次提交准入决定摘要")
    private String admissionDecisionDigest;

    @Schema(description = "首次提交准入证据引用")
    private List<String> admissionEvidenceRefs;

    @Schema(description = "出款完成对应的资金交易流水号")
    private String completionFundsTransactionSn;

    @Schema(description = "出款回退对应的资金交易流水号")
    private String rollbackFundsTransactionSn;

    @Schema(description = "最近一次外部回执摘要")
    private String lastReceiptDigest;

    @Schema(description = "最近一次失败码")
    private String failureCode;

    @Schema(description = "最近一次失败原因")
    private String failureReason;

    @Schema(description = "首次提交时间")
    private LocalDateTime submittedTime;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

}
