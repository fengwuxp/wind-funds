package com.wind.funds.transaction.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资金动作事实稳定引用。
 *
 * @author Codex
 * @date 2026-08-14
 */
@Value
public class FundsActionFactRef implements Serializable {

    @Serial
    private static final long serialVersionUID = -1440925445922987400L;

    @Schema(description = "租户 ID")
    Long tenantId;

    @Schema(description = "资金动作事实稳定身份")
    String identity;
}
