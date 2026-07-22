package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
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
 * 对账差错处理回链请求。
 *
 * <p>职责：把外部审批后的调账、冲正、挂账、追偿或核销处理动作回链到差错单。</p>
 *
 * <p>边界：请求只登记处理结果引用，不由 reconciliation 直接创建资金事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LinkReconciliationDifferenceAdjustmentRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -8801181134542705736L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "对账差错流水号")
    @NotBlank
    private String differenceSn;

    @Schema(description = "上层业务已确认的差错处理动作类型")
    @NotNull
    private ReconciliationDifferenceActionType actionType;

    @Schema(description = "处理动作、调账、冲正、挂账、追偿或核销单号")
    @NotBlank
    private String adjustmentSn;

    @Schema(description = "处理动作幂等键，用于避免重复调账、重复冲正、重复挂账、重复追偿或重复核销")
    @NotBlank
    private String idempotencyKey;

    @Schema(description = "被处理的原始资金事实、外部事实、对账匹配结果或差错来源引用")
    @NotBlank
    private String originalFactRef;

    @Schema(description = "关联资金交易流水号，可为空")
    private String adjustmentTransactionSn;

    @Schema(description = "审批引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "处理证据引用")
    @NotBlank
    private String evidenceRef;

    @Schema(description = "处理原因")
    @NotBlank
    private String reason;
}
