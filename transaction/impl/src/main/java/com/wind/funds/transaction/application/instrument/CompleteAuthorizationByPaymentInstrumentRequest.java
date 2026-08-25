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
 * Transaction Provider 内部支付工具授权完成命令，
 * 只携带可信完成事实并沿原授权快照回放账务与控制关系。
 *
 * @author wuxp
 * @since 2026-07-29
 */
@Schema(description = "支付工具授权完成请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CompleteAuthorizationByPaymentInstrumentRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "原授权资金交易号")
    @NotBlank
    private String authorizationTransactionSn;

    @Schema(description = "本次完成金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "完成币种，必须与原授权一致")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "本次完成业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次完成业务流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "可信完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "完成说明")
    private String description;
}
