package com.capte.funds.transaction.model.query;

import com.capte.funds.transaction.enums.PaymentInstrumentDirection;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付工具查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentQuery {

    @Schema(description = "工具号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "工具归属主体 ID")
    private String ownerId;

    @Schema(description = "工具归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "工具类型")
    private String instrumentType;

    @Schema(description = "工具方向")
    private PaymentInstrumentDirection instrumentDirection;

    @Schema(description = "工具展示号或稳定识别号")
    private String instrumentNo;

    @Schema(description = "通道编码")
    private String channelCode;

    @Schema(description = "外部工具 ID")
    private String externalInstrumentId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "状态")
    private FundsAccountStatus status;
}
