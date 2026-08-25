package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.value.StableIdentity;
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

/**
 * 单笔可清分明细识别请求。
 *
 * <p>清分周期和规则已经由上层清分策略解析完成；本请求只携带稳定结果，不在资金底座解析调度表达式。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class IdentifyClearingSplittableDetailRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1365221902512127792L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "来源资金动作事实稳定引用")
    @NotNull
    private StableIdentity sourceActionFactRef;

    @Schema(description = "上层清分策略已经确认的业务线")
    @NotBlank
    private String businessLine;

    @Schema(description = "已解析的清分周期")
    @NotBlank
    private String splitPeriod;

    @Schema(description = "清分规则编码")
    @NotBlank
    private String splitRuleCode;

    @Schema(description = "清分规则版本")
    @NotBlank
    private String splitRuleVersion;
}
