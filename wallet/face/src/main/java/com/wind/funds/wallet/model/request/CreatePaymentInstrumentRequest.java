package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
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
 * 创建支付工具请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString(exclude = {"instrumentNo", "externalInstrumentId"})
@Accessors(chain = true)
public class CreatePaymentInstrumentRequest {

    @Schema(description = "工具号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "工具归属主体 ID")
    @NotBlank
    private String ownerId;

    @Schema(description = "工具归属主体类型")
    @NotNull
    private FundsAccountOwnerType ownerType;

    @Schema(description = "工具类型")
    @NotBlank
    private String instrumentType;

    @Schema(description = "工具资金流向")
    @NotNull
    private PaymentInstrumentFlowDirection flowDirection;

    @Schema(description = "工具展示号或稳定识别号")
    @NotBlank
    private String instrumentNo;

    @Schema(description = "支付工具接入方或提供方编码")
    private String channelCode;

    @Schema(description = "外部工具 ID")
    private String externalInstrumentId;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "状态")
    private FundsAccountState state;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
