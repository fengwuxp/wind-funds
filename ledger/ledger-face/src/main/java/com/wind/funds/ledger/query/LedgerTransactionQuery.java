package com.wind.funds.ledger.query;

import com.wind.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 账户账本交易
 *
 * @author wuxp
 * @since 2026-04-14
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class LedgerTransactionQuery {

    @Schema(description = "账本交易流水号")
    private String sn;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "标准业务交易流水")
    private String fundsTransactionSn;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "交易状态")
    private LedgerTransactionStatus status;

    @Schema(description = "交易币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务单号")
    private String businessSn;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "关联账本交易单号")
    private String referenceLedgerTransactionSn;

    @Schema(description = "查询到最小交易时间")
    private LocalDateTime transactionTimeMin;

    @Schema(description = "查询到最大交易时间")
    private LocalDateTime transactionTimeMax;

    @Schema(description = "查询到最小 gmtCreate")
    private LocalDateTime gmtCreateMin;

    @Schema(description = "查询到最大 gmtCreate")
    private LocalDateTime gmtCreateMax;

    @Schema(description = "查询到最小 gmtModified")
    private LocalDateTime gmtModifiedMin;

    @Schema(description = "查询到最大 gmtModified")
    private LocalDateTime gmtModifiedMax;

}
