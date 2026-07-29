package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 从已确认清分结果快照生成清算候选请求。
 *
 * <p>清算周期和可清算时间由上层清算策略解析后传入；本服务不解析策略，不从余额反推候选金额。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateClearingCandidateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3175255522570359831L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "清分结果快照流水号")
    @NotBlank
    private String splitResultSn;

    @Schema(description = "已解析的清算周期，不是清分周期或账本周期")
    @NotBlank
    private String clearingPeriod;

    @Schema(description = "清算规则编码")
    @NotBlank
    private String clearingRuleCode;

    @Schema(description = "清算规则版本")
    @NotBlank
    private String clearingRuleVersion;

    @Schema(description = "最早可进入清算批次的时间")
    @NotNull
    private LocalDateTime clearingAvailableTime;
}
