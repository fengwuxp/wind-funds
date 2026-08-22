package com.wind.funds.transaction.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 资金动作事实集合查询条件。
 *
 * @author Codex
 * @date 2026-08-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsActionFactQuery {

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号")
    private String businessSn;
}
