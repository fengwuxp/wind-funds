package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ClearingSplitBatchStatus;
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
 * 清分批次 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ClearingSplitBatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7526894227702782029L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "清分批次流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "唯一账务主体类型")
    private String subjectType;

    @Schema(description = "唯一账务主体 ID")
    private String subjectId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务线")
    private String businessLine;

    @Schema(description = "清分周期")
    private String splitPeriod;

    @Schema(description = "清分规则编码")
    private String splitRuleCode;

    @Schema(description = "清分规则版本")
    private String splitRuleVersion;

    @Schema(description = "明细数量")
    private Integer detailCount;

    @Schema(description = "批次金额汇总，最小货币单位")
    private Long totalAmount;

    @Schema(description = "成员事实摘要")
    private String memberDigest;

    @Schema(description = "批次事实摘要")
    private String batchDigest;

    @Schema(description = "批次状态")
    private ClearingSplitBatchStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "提交复核时间")
    private LocalDateTime submittedTime;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "取消原因")
    private String cancelReason;
}
