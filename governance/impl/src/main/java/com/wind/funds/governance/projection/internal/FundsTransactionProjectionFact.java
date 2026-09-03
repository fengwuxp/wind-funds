package com.wind.funds.governance.projection.internal;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 交易投影重放来源事实，是从交易事实和路由快照中抽取出来的只读重建原料。
 *
 * <p>职责：为投影重建提供来源单号、主体、展示类型、展示状态、金额、币种、发生时间和扩展载荷。</p>
 *
 * <p>能力：屏蔽底层交易表、交易明细或 route snapshot 来源差异，让重建逻辑只依赖统一事实结构。</p>
 *
 * <p>边界：该对象只能作为只读投影原料，不能作为新交易事实、账本分录、余额调整或补账依据。</p>
 *
 * @param viewDomain 投影视图域
 * @param ownerType 投影归属主体类型
 * @param ownerId 投影归属主体标识
 * @param sourceSn 来源资金交易流水
 * @param displayType 展示交易类型
 * @param displayStatus 展示状态
 * @param amount 最小货币单位金额
 * @param currency 币种
 * @param occurredTime 资金事实发生时间
 * @param payload 可解释投影载荷
 * @author wuxp
 * @since 2026-08-31
 */
@Builder
public record FundsTransactionProjectionFact(@NonNull String viewDomain,
                                             @NonNull String ownerType,
                                             @NonNull String ownerId,
                                             @NonNull String sourceSn,
                                             @NonNull String displayType,
                                             @NonNull String displayStatus,
                                             long amount,
                                             @NonNull CurrencyIsoCode currency,
                                             @NonNull LocalDateTime occurredTime,
                                             @NonNull Map<String, Object> payload) {
}
