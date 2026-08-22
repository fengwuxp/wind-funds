package com.wind.funds.reconciliation.model.value;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 所有方范围内的稳定身份。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "所有方范围内的稳定身份")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class StableIdentity implements Serializable {

    @Serial
    private static final long serialVersionUID = 4573487898806286819L;

    @Schema(description = "身份所有方命名空间")
    @NotBlank
    private String ownerNamespace;

    @Schema(description = "所有方命名空间内的稳定值")
    @NotBlank
    private String value;
}
