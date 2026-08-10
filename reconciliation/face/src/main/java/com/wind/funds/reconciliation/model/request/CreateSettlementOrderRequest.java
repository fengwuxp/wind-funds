package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 结算单创建请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算单创建请求")
@Data
@Accessors(chain = true)
public class CreateSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3743281674326227367L;

    public static final int MAX_CLEARING_BATCH_COUNT = 1000;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "纳入本次结算的已确认清算批次流水号")
    @NotEmpty
    @Size(max = MAX_CLEARING_BATCH_COUNT)
    private List<String> clearingBatchSns;

    @Schema(description = "结算账期")
    @NotBlank
    private String settlementPeriod;

    @Schema(description = "结算模式")
    @NotNull
    private SettlementMode settlementMode;

    @Schema(description = "结算目标类型")
    @NotNull
    private SettlementDestination settlementDestination;

    @Schema(description = "结算触发模式")
    @NotNull
    private SettlementTriggerMode triggerMode;

    @Schema(description = "结算时区 ID")
    @NotBlank
    private String timezone;

    @Schema(description = "结算截止时间口径")
    @NotBlank
    private String cutoff;

    @Schema(description = "结算策略编码")
    @NotBlank
    private String policyCode;

    @Schema(description = "结算策略版本")
    @NotBlank
    private String policyVersion;

    @Schema(description = "结算策略审批引用")
    private String policyApprovalRef;
}
