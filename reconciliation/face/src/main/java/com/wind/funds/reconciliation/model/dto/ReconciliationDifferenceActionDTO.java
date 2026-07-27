package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
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
 * 对账差错处理动作事实 DTO。
 *
 * <p>职责：按发生顺序解释上层业务已经完成并回链的处理结果；该对象不是资金执行授权或 Web 操作审计。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationDifferenceActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2713644593564528415L;

    @Schema(description = "差错处理动作记录流水号")
    private String sn;

    @Schema(description = "对账差错流水号")
    private String differenceSn;

    @Schema(description = "差错处理动作类型")
    private ReconciliationDifferenceActionType actionType;

    @Schema(description = "上层处理动作或结果单号")
    private String adjustmentSn;

    @Schema(description = "处理动作幂等键")
    private String idempotencyKey;

    @Schema(description = "被处理的原始事实引用")
    private String originalFactRef;

    @Schema(description = "关联资金交易流水号")
    private String adjustmentTransactionSn;

    @Schema(description = "审批引用")
    private String approvalRef;

    @Schema(description = "处理证据引用")
    private String evidenceRef;

    @Schema(description = "处理原因")
    private String reason;

    @Schema(description = "记录人")
    private String createdBy;

    @Schema(description = "记录时间")
    private LocalDateTime createdTime;
}
