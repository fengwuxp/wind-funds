package com.wind.funds.wallet.model.dto;

import com.wind.funds.spec.ledger.NegativeAvailablePolicySpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 受控负可用余额策略元数据。
 *
 * @author Codex
 * @date 2026-05-15
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class NegativeAvailablePolicyDTO implements NegativeAvailablePolicySpec, Serializable {

    @Serial
    private static final long serialVersionUID = 6587679411739150799L;

    @Schema(description = "策略编码")
    private String policyCode;

    @Schema(description = "策略版本")
    private Integer policyVersion;

    @Schema(description = "是否必须关联来源事实")
    private Boolean requireSourceFact;

    @Schema(description = "是否必须记录负余额原因")
    private Boolean requireReason;

    @Schema(description = "是否必须有审批或风控规则依据")
    private Boolean requireApprovalOrRiskRule;

    @Schema(description = "是否必须记录风险状态")
    private Boolean requireRiskStatus;

    @Schema(description = "是否必须配置单笔上限")
    private Boolean requireSingleLimit;

    @Schema(description = "是否必须配置累计上限")
    private Boolean requireCumulativeLimit;

    @Schema(description = "是否必须跟踪账龄")
    private Boolean requireAgingTracking;

    @Schema(description = "后续交易是否必须重新校验策略")
    private Boolean recheckFutureTransaction;

    @Schema(description = "后续治理路径")
    private String governancePath;

}
