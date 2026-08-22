package com.wind.funds.reconciliation.model.value;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
 * 不可变门禁要求版本引用。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "门禁要求引用")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class GateRequirementRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 713820747232962141L;

    @Schema(description = "本要求约束的阶段动作")
    @Valid
    @NotNull
    private GateStageRef stageRef;

    @Schema(description = "提供方生成的要求身份")
    @Valid
    @NotNull
    private StableIdentity requirementIdentity;

    @Schema(description = "要求版本")
    @NotBlank
    private String requirementVersion;

    @Schema(description = "要求语义 SHA-256")
    @NotBlank
    private String semanticDigest;

    @Schema(description = "要求证据包 SHA-256")
    @NotBlank
    private String evidenceBundleDigest;
}
