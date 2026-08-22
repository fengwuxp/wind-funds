package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.value.GateStageRef;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * <p>职责：承载一次精确阶段动作的准入检查。</p>
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

    @Schema(description = "精确阶段动作引用")
    @Valid
    @NotNull
    private GateStageRef stageRef;
}
