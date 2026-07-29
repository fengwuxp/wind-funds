package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ClearingBatchStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 清算批次 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingBatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3425376318095609698L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "清算批次流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "账务主体类型")
    private String subjectType;

    @Schema(description = "账务主体 ID")
    private String subjectId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务线")
    private String businessLine;

    @Schema(description = "清算周期，不是账本周期")
    private String clearingPeriod;

    @Schema(description = "清算规则编码")
    private String clearingRuleCode;

    @Schema(description = "清算规则版本")
    private String clearingRuleVersion;

    @Schema(description = "候选数量")
    private Integer candidateCount;

    @Schema(description = "清算总金额，最小货币单位")
    private Long totalAmount;

    @Schema(description = "候选经济范围 SHA-256")
    private String amountDigest;

    @Schema(description = "成功或明确失败的资金交易流水号")
    private String fundsTransactionSn;

    @Schema(description = "批次状态")
    private ClearingBatchStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "提交复核时间")
    private LocalDateTime submittedTime;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "退回草稿时间")
    private LocalDateTime returnedTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "失败时间")
    private LocalDateTime failedTime;

    @Schema(description = "最近一次退回、取消或失败原因")
    private String reason;
}
