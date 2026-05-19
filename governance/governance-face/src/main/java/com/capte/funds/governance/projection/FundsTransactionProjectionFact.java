package com.capte.funds.governance.projection;

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
 */
@Builder
public record FundsTransactionProjectionFact(@NonNull String viewDomain,
                                             @NonNull String ownerType,
                                             @NonNull String ownerId,
                                             @NonNull String sourceSn,
                                             @NonNull String displayType,
                                             @NonNull String displayStatus,
                                             long amount,
                                             @NonNull String currency,
                                             @NonNull LocalDateTime occurredTime,
                                             @NonNull Map<String, Object> payload) {
}
