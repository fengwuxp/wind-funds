package com.wind.funds.transaction.projection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影解释查询条件。
 *
 * <p>当前最小可用版本只按资金交易流水号解释已经落库的交易事实；支付工具流水、冻结单视图、
 * 预算控制视图和治理重放视图由后续独立切片扩展。</p>
 */
@Builder
public record FundsTransactionProjectionExplainQuery(
        @Schema(description = "资金交易流水号")
        @NonNull String fundsTransactionSn) {
}
