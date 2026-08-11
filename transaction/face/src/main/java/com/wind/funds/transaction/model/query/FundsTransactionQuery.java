package com.wind.funds.transaction.model.query;

import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 标准资金交易查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsTransactionQuery {

    @Schema(description = "交易号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "交易模式")
    private FundsTransactionMode transactionMode;

    @Schema(description = "交易类型")
    private DefaultFundsTransactionType transactionType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务号")
    private String businessSn;

    @Schema(description = "引用交易号")
    private String referenceTransactionSn;

    @Schema(description = "交易状态")
    private FundsTransactionState state;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;
}
