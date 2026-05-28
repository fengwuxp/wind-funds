package com.capte.funds.transaction.model;

import com.capte.funds.transaction.enums.FundsEffectType;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import com.wind.transaction.core.enums.CurrencyIsoCode;
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
import java.util.Map;

/**
 * 标准资金交易参与方。
 *
 * @author Codex
 * @date 2026-05-12
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsTransactionParticipant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1266289387783199362L;

    @Schema(description = "主体 ID")
    @NotBlank
    private String subjectId;

    @Schema(description = "主体类型")
    @NotBlank
    private String subjectType;

    @Schema(description = "参与方角色")
    @NotNull
    private RouteParticipantRole participantRole;

    @Schema(description = "主体视角金额")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "资金业务效果")
    @NotNull
    private FundsEffectType fundsEffectType;

    @Schema(description = "关联主体 ID")
    private String referenceSubjectId;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private Map<String, Object> contextVariables = Map.of();

    public FundsTransactionParticipant setContextVariables(Map<String, Object> contextVariables) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                "fundsTransactionParticipant.contextVariables must not contain sensitive fields");
        this.contextVariables = FundsBenefitSpecValidators.immutableInstructionContext(contextVariables,
                "fundsTransactionParticipant");
        return this;
    }
}
