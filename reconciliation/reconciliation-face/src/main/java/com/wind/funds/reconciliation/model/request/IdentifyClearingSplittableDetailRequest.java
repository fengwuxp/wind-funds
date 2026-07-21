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

    @Schema(description = "来源资金交易流水号")
    @NotBlank
    private String fundsTransactionSn;

    @Schema(description = "来源资金交易明细流水号")
    @NotBlank
    private String fundsTransactionDetailSn;

    @Schema(description = "来源账本分录流水号")
    @NotBlank
    private String ledgerEntrySn;

    @Schema(description = "清分前必须消费的对账运行结果流水号")
    @NotBlank
    private String reconciliationRunResultSn;

    @Schema(description = "已解析的清分周期")
    @NotBlank
    private String clearingPeriod;

    @Schema(description = "清分规则编码")
    @NotBlank
    private String ruleCode;

    @Schema(description = "清分规则版本")
    @NotBlank
    private String ruleVersion;
}
