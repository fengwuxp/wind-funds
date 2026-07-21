package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
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
 * 对账准入检查请求。
 *
 * <p>职责：承载清算、结算或出款消费方的准入检查对象。</p>
 *
 * <p>边界：请求只引用已落库的对账运行结果并检查差错阻断状态，不携带资金记账、清算确认、结算锁定或出款提交事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CheckReconciliationGateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 804854332547401231L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "准入消费对象类型，例如 CLEARING、SETTLEMENT、PAYOUT")
    @NotNull
    private ReconciliationGateObjectType gateObjectType;

    @Schema(description = "准入消费对象流水号，例如清算候选、结算单或出款单号")
    @NotBlank
    private String gateObjectSn;

    @Schema(description = "本次准入必须消费的对账运行结果流水号")
    @NotBlank
    private String reconciliationRunResultSn;
}
