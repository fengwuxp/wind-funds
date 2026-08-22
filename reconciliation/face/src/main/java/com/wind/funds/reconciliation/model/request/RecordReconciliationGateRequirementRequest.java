package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.model.value.GateRequirementRef;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.RequiredPairRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 发布一份不可变的必需门禁对账对版本。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "记录对账门禁要求请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordReconciliationGateRequirementRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -6116984759556610224L;

    @NotNull
    @Schema(description = "租户 ID")
    private Long tenantId;

    @Valid
    @NotNull
    @Schema(description = "精确阶段动作")
    private GateStageRef stageRef;

    @NotBlank
    @Schema(description = "要求版本")
    private String requirementVersion;

    @Valid
    @NotEmpty
    @Size(max = 100)
    @Schema(description = "必需对账对列表")
    private List<RequiredPairRef> requiredPairs;

    @Valid
    @Schema(description = "CAS 期望的当前要求；首个版本为空")
    private GateRequirementRef expectedCurrentRequirementRef;

    @NotEmpty
    @Size(max = 100)
    @Schema(description = "要求证据引用")
    private List<@NotBlank String> evidenceRefs;
}
