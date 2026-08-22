package com.wind.funds.reconciliation.model.value;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已冻结的来源快照覆盖范围。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "来源快照覆盖范围")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SnapshotCoverage implements Serializable {

    @Serial
    private static final long serialVersionUID = 8200525535583612778L;

    @Schema(description = "声明的来源范围是否已完整覆盖")
    @NotNull
    private Boolean complete;

    @Schema(description = "来源方水位标识")
    private String watermark;

    @Schema(description = "快照中的归一化事实数量")
    @NotNull
    @PositiveOrZero
    private Integer memberCount;
}
