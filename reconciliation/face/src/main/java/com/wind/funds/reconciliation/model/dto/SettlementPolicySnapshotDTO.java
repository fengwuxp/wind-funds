package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算单持有的结算策略快照。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算策略快照")
@Data
@Accessors(chain = true)
public class SettlementPolicySnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6630849547493475742L;

    @Schema(description = "结算策略编码")
    private String policyCode;

    @Schema(description = "结算策略版本")
    private String policyVersion;

    @Schema(description = "结算模式")
    private SettlementMode settlementMode;

    @Schema(description = "结算目标类型")
    private SettlementDestination settlementDestination;

    @Schema(description = "结算触发模式")
    private SettlementTriggerMode triggerMode;

    @Schema(description = "结算账期")
    private String settlementPeriod;

    @Schema(description = "结算时区 ID")
    private String timezone;

    @Schema(description = "结算截止时间口径")
    private String cutoff;

    @Schema(description = "策略审批引用")
    private String policyApprovalRef;
}
