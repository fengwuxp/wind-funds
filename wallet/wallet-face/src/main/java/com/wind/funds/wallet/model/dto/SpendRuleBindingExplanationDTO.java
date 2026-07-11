package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.SpendRuleBindingExplanationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spend Rule 挂载解释 DTO。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleBindingExplanationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6744329661650188692L;

    @Schema(description = "规则挂载快照")
    private SpendRuleBindingDTO binding;

    @Schema(description = "评估时间")
    private LocalDateTime evaluatedAt;

    @Schema(description = "当前是否有效")
    private Boolean effective;

    @Schema(description = "解释状态")
    private SpendRuleBindingExplanationStatus explanationStatus;

    @Schema(description = "解释说明")
    private String explanationMessage;

    @Schema(description = "证据引用列表")
    private List<String> evidenceRefs;
}
