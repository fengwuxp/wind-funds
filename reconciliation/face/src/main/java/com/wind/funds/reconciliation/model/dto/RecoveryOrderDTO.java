package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.enums.RecoveryOrderState;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资金追偿单公共事实。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "资金追偿单公共事实")
@Data
@Accessors(chain = true)
public class RecoveryOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3927603104028972332L;

    @Schema(description = "追偿单流水号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "追偿来源类型")
    private String sourceType;

    @Schema(description = "追偿来源流水号")
    private String sourceSn;

    @Schema(description = "责任主体类型")
    private String responsibleSubjectType;

    @Schema(description = "责任主体标识")
    private String responsibleSubjectId;

    @Schema(description = "应追偿金额，最小货币单位")
    private Long expectedAmount;

    @Schema(description = "已追偿金额，最小货币单位")
    private Long recoveredAmount;

    @Schema(description = "剩余待追偿金额，最小货币单位")
    private Long remainingAmount;

    @Schema(description = "追偿币种")
    private CurrencyIsoCode currency;

    @Schema(description = "追偿单状态")
    private RecoveryOrderState state;

    @Schema(description = "最近一次追偿资金交易流水号")
    private String lastFundsTransactionSn;

    @Schema(description = "完成追偿时间")
    private LocalDateTime recoveredTime;
}
