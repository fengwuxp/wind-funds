package com.wind.funds.reconciliation.model.dto;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算单资金金额项。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算单资金金额项")
@Data
@Accessors(chain = true)
public class SettlementOrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1703181723463422769L;

    @Schema(description = "结算金额项流水号")
    private String sn;

    @Schema(description = "来源类型，当前固定 CLEARING_BATCH")
    private String sourceType;

    @Schema(description = "已确认清算批次流水号")
    private String sourceSn;

    @Schema(description = "金额项类型，当前固定 PRINCIPAL")
    private String itemType;

    @Schema(description = "金额方向，当前固定 ADD")
    private String direction;

    @Schema(description = "金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;
}
