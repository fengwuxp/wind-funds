package com.wind.funds.wallet.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * 退役 Spend Rule 挂载请求。
 *
 * @author Codex
 * @date 2026-07-13
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RetireSpendRuleBindingRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1770761181298250516L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "Spend Rule 挂载流水号")
    @NotBlank
    private String sn;

    @Schema(description = "描述")
    private String description;
}
