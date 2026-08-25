package com.wind.funds.transaction.application.instrument;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Transaction Provider 内部支付工具授权撤销命令。
 *
 * <p>调用方只提供原授权引用和本次可信撤销事实；账务主体、支付工具快照和控制预留关系必须从原授权事实回放。</p>
 *
 * @author Codex
 * @since 2026-07-29
 */
@Schema(description = "支付工具授权撤销请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReverseAuthorizationByPaymentInstrumentRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "原授权资金交易号")
    @NotBlank
    private String authorizationTransactionSn;

    @Schema(description = "本次撤销金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "撤销币种，必须与原授权一致")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "本次撤销业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次撤销业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "可信撤销发生时间")
    private LocalDateTime reversalTime;

    @Schema(description = "撤销描述")
    private String description;
}
