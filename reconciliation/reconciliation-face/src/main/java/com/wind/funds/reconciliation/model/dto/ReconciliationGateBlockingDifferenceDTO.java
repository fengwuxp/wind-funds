package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对账准入阻断差错 DTO。
 *
 * <p>职责：向清算、结算、出款和运营解释具体是哪一个差错阻断了准入。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReconciliationGateBlockingDifferenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3111841020139319524L;

    @Schema(description = "对账差错流水号")
    private String differenceSn;

    @Schema(description = "对账差错状态")
    private ReconciliationDifferenceStatus status;

    @Schema(description = "对账差错严重等级")
    private ReconciliationDifferenceSeverity severity;

    @Schema(description = "责任方引用")
    private String responsiblePartyRef;

    @Schema(description = "阻断范围")
    private String blockingScope;

    @Schema(description = "来源证据引用")
    private String evidenceRef;

    @Schema(description = "差错处理动作类型")
    private ReconciliationDifferenceActionType actionType;

    @Schema(description = "关联处理动作或调账单号")
    private String adjustmentSn;

    @Schema(description = "处理动作幂等键")
    private String adjustmentIdempotencyKey;

    @Schema(description = "被处理的原始事实引用")
    private String originalFactRef;

    @Schema(description = "关联资金交易流水号")
    private String adjustmentTransactionSn;

    @Schema(description = "最后一次重跑流水号")
    private String lastRerunSn;

    @Schema(description = "最后一次重跑是否对平")
    private Boolean lastRerunBalanced;

    @Schema(description = "最后一次重跑证据引用")
    private String lastRerunEvidenceRef;

    @Schema(description = "阻断原因")
    private String blockingReason;
}
