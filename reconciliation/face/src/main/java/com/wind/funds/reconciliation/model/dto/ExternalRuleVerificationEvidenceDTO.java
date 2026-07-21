package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
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
import java.time.LocalDate;

/**
 * 外部规则核验证据 DTO。
 *
 * <p>职责：承载出款准入所需的规则来源、版本、生效、适用范围、法域、核验和确认状态。</p>
 *
 * <p>边界：只保存最小核验摘要和证据引用，不保存完整规则正文、敏感材料或专业确认结论本身。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ExternalRuleVerificationEvidenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3035732847782201398L;

    @Schema(description = "核验证据引用")
    @NotBlank
    private String evidenceRef;

    @Schema(description = "规则来源")
    @NotBlank
    private String ruleSource;

    @Schema(description = "规则版本或发布日期")
    @NotBlank
    private String versionOrPublishedAt;

    @Schema(description = "规则生效日期")
    @NotNull
    private LocalDate effectiveDate;

    @Schema(description = "适用主体或适用范围")
    @NotBlank
    private String applicableScope;

    @Schema(description = "适用法域")
    @NotBlank
    private String jurisdiction;

    @Schema(description = "核验日期")
    @NotNull
    private LocalDate verifiedAt;

    @Schema(description = "确认方")
    @NotBlank
    private String confirmedBy;

    @Schema(description = "确认状态")
    @NotNull
    private ExternalRuleVerificationStatus status;
}
