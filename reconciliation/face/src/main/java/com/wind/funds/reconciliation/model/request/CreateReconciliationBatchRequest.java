package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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

    @Schema(description = "本次对账作业范围的稳定身份")
    @Valid
    @NotNull
    private StableIdentity scopeIdentity;

    @Schema(description = "本次对账双方关系的稳定身份")
    @Valid
    @NotNull
    private StableIdentity pairIdentity;

    @Schema(description = "本次对账币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "结构化比较规则引用")
    @Valid
    @NotNull
    private ComparisonRuleRef comparisonRuleRef;

    @Schema(description = "对账窗口开始时间，包含该时刻")
    @NotNull
    private LocalDateTime windowStart;

    @Schema(description = "对账窗口结束时间，不包含该时刻")
    @NotNull
    private LocalDateTime windowEnd;

    @Schema(description = "窗口时间语义")
    @NotBlank
    @Size(max = 64)
    private String timeSemantics;

    @Schema(description = "对账窗口 IANA 时区 ID，例如 Asia/Shanghai")
    @NotBlank
    private String timezoneId;

    @Schema(description = "重跑引用的上一批次流水号；首次运行为空")
    private String previousBatchSn;
}
