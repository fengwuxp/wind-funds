package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算单公共事实。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算单公共事实")
@Data
@Accessors(chain = true)
public class SettlementOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -878598498433192870L;

    @Schema(description = "结算单流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "结算主体类型")
    private String settlementSubjectType;

    @Schema(description = "结算主体标识")
    private String settlementSubjectId;

    @Schema(description = "结算币种")
    private CurrencyIsoCode currency;

    @Schema(description = "结算账期")
    private String settlementPeriod;

    @Schema(description = "结算净额，最小货币单位")
    private Long netAmount;

    @Schema(description = "结算单状态")
    private SettlementOrderState state;

    @Schema(description = "结算策略快照")
    private SettlementPolicySnapshotDTO policySnapshot;

    @Schema(description = "结算单明细")
    private List<SettlementOrderItemDTO> items;

    @Schema(description = "结算审批引用")
    private String settlementApprovalRef;

    @Schema(description = "结算锁定资金交易流水号")
    private String lockFundsTransactionSn;

    @Schema(description = "结算释放资金交易流水号")
    private String releaseFundsTransactionSn;

    @Schema(description = "承接释放金额的冻结单流水号")
    private String releaseFreezeOrderSn;

    @Schema(description = "结算锁定资金释放去向")
    private SettlementReleaseDisposition releaseDisposition;

    @Schema(description = "结算释放事实摘要")
    private String releaseDigest;

    @Schema(description = "结算释放成功消费的阶段门禁证据引用")
    private String releaseGateEvidenceRef;

    @Schema(description = "释放消费的当前谱系批次流水号")
    private String releaseCurrentLineageBatchSn;

    @Schema(description = "释放来源闭合摘要")
    private String releaseSourceClosureDigest;

    @Schema(description = "释放授权决定摘要")
    private String releaseAuthorityDecisionDigest;

    @Schema(description = "释放授权证据引用")
    private List<String> releaseAuthorityEvidenceRefs;

    @Schema(description = "释放审批引用")
    private String releaseApprovalRef;

    @Schema(description = "结算锁定成功消费的阶段门禁证据引用")
    private String lockGateEvidenceRef;

    @Schema(description = "结算金额摘要")
    private String amountDigest;

    @Schema(description = "结算来源摘要")
    private String sourceDigest;

    @Schema(description = "结算策略快照摘要")
    private String policySnapshotDigest;

    @Schema(description = "结算单稳定摘要")
    private String orderDigest;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "提交时间")
    private LocalDateTime submittedTime;

    @Schema(description = "审批时间")
    private LocalDateTime approvedTime;

    @Schema(description = "资金锁定时间")
    private LocalDateTime lockedTime;

    @Schema(description = "资金释放时间")
    private LocalDateTime releasedTime;

    @Schema(description = "退回草稿时间")
    private LocalDateTime returnedTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "失败时间")
    private LocalDateTime failedTime;

    @Schema(description = "最近一次退回、取消、失败或释放原因")
    private String reason;
}
