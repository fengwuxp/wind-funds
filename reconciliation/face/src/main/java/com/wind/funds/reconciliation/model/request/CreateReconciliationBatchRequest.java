package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建对账批次请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateReconciliationBatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3811486815856063676L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "本次对账作业范围的稳定业务引用")
    @NotBlank
    @Size(max = 128)
    private String reconciliationScopeRef;

    @Schema(description = "本批次运行结果适用的准入对象类型；纯对账时为空")
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "本批次运行结果适用的准入对象流水号；纯对账时为空")
    private String gateObjectSn;

    @Schema(description = "匹配或对账规则版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "对账窗口开始时间，包含该时刻")
    @NotNull
    private LocalDateTime windowStart;

    @Schema(description = "对账窗口结束时间，不包含该时刻")
    @NotNull
    private LocalDateTime windowEnd;

    @Schema(description = "对账窗口 IANA 时区 ID，例如 Asia/Shanghai")
    @NotBlank
    private String timezoneId;

    @Schema(description = "重跑引用的上一批次流水号；首次运行为空")
    private String previousBatchSn;
}
