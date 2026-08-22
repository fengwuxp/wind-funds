package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.SettlementReleaseCoverageStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseLateDataStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseResultReplacementStatus;
import com.wind.funds.reconciliation.enums.SettlementReleaseLineageSupersessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算锁定资金释放请求。
 *
 * <p>调用方必须提交来源收齐、规则结果、当前谱系和迟到/替代状态；服务在同一事务内重查权威 Gate 后决定是否释放。</p>
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算锁定资金释放请求")
@Data
@Accessors(chain = true)
public class ReleaseSettlementOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -7110665515020317235L;

    public static final int MAX_REASON_LENGTH = 512;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "待释放的结算单流水号")
    @NotBlank
    private String settlementOrderSn;

    @Schema(description = "必需来源收齐状态")
    @NotNull
    private SettlementReleaseCoverageStatus coverageStatus;

    @Schema(description = "来源覆盖范围稳定摘要")
    @NotBlank
    private String coverageDigest;

    @Schema(description = "来源处理水位")
    @NotNull
    private LocalDateTime watermark;

    @Schema(description = "来源收集截止时间")
    @NotNull
    private LocalDateTime cutoff;

    @Schema(description = "释放准入规则版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "释放准入规则决定摘要")
    @NotBlank
    private String ruleDecisionDigest;

    @Schema(description = "当前有效谱系批次流水号")
    @NotBlank
    private String currentLineageBatchSn;

    @Schema(description = "迟到数据状态")
    @NotNull
    private SettlementReleaseLateDataStatus lateDataStatus;

    @Schema(description = "结果替代状态")
    @NotNull
    private SettlementReleaseResultReplacementStatus resultReplacementStatus;

    @Schema(description = "批次谱系取代状态")
    @NotNull
    private SettlementReleaseLineageSupersessionStatus lineageSupersessionStatus;

    @Schema(description = "业务审批或人工授权引用")
    @NotBlank
    private String approvalRef;

    @Schema(description = "释放原因")
    @NotBlank
    @Size(max = MAX_REASON_LENGTH)
    private String reason;

    @Schema(description = "支持来源闭合与释放授权的稳定证据引用")
    @NotEmpty
    private List<@NotBlank String> evidenceRefs;
}
