package com.wind.funds.wallet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Spend Rule 决策解释 DTO。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleDecisionExplanationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8718788569873955280L;

    @Schema(description = "规则决策日志快照")
    private SpendRuleDecisionLogDTO decision;

    @Schema(description = "决策是否允许进入后续交易")
    private Boolean admitted;

    @Schema(description = "解释说明")
    private String explanationMessage;

    @Schema(description = "证据引用列表")
    private List<String> evidenceRefs;
}
