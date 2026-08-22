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
 * 一个比较规则版本的稳定引用。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "比较规则引用")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ComparisonRuleRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 155485135110053967L;

    @Schema(description = "规则所有方命名空间")
    @NotBlank
    private String namespace;

    @Schema(description = "规则身份")
    @NotBlank
    private String identity;

    @Schema(description = "规则版本")
    @NotBlank
    private String version;
}
