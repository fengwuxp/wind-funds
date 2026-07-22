package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
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
 * 对账批次 DTO。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationBatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3495653665636063145L;

    @Schema(description = "对账批次流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "准入对象类型")
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "准入对象流水号")
    private String gateObjectSn;

    @Schema(description = "匹配或对账规则版本")
    private String ruleVersion;

    @Schema(description = "对账窗口开始时间，含")
    private LocalDateTime windowStart;

    @Schema(description = "对账窗口结束时间，不含")
    private LocalDateTime windowEnd;

    @Schema(description = "对账窗口时区 ID")
    private String timezoneId;

    @Schema(description = "上一批次流水号")
    private String previousBatchSn;

    @Schema(description = "批次状态")
    private ReconciliationBatchStatus status;

    @Schema(description = "完成态运行结果流水号")
    private String runResultSn;

    @Schema(description = "对账范围与重跑关系 SHA-256")
    private String batchDigest;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "最后修改时间")
    private LocalDateTime modifiedTime;
}
