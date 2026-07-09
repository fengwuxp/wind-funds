package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付工具 DTO。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString(exclude = {"instrumentNo", "externalInstrumentId"})
@Accessors(chain = true)
public class PaymentInstrumentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -570825777715240467L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

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

    @Schema(description = "工具资金流向")
    private PaymentInstrumentFlowDirection flowDirection;

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

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
