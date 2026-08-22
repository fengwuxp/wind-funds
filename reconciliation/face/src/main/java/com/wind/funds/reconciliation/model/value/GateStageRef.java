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
 * 由对账门禁评估的精确阶段动作引用。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "精确对账门禁阶段引用")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class GateStageRef implements Serializable {

    @Serial
    private static final long serialVersionUID = -4330681776018031310L;

    @Schema(description = "稳定阶段动作类型")
    @NotBlank
    private String stageKind;

    @Schema(description = "稳定阶段动作身份")
    @Valid
    @NotNull
    private StableIdentity stageIdentity;
}
