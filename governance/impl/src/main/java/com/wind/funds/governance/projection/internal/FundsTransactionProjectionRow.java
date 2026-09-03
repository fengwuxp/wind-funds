package com.wind.funds.governance.projection.internal;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 交易投影重建行，是根据来源事实重建出来的只读视图行。
 *
 * <p>职责：承载即将用于差异比对、影子写入或正式投影写入的交易展示数据。</p>
 *
 * <p>能力：统一表达投影视图中的主体、来源单、展示口径、金额币种、发生时间和扩展载荷。</p>
 *
 * <p>边界：该对象只写入交易投影或影子投影，不反向修改交易事实、账本事实、余额投影或业务单据。</p>
 *
 * @param projectionSn 投影行流水
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
public record FundsTransactionProjectionRow(@NonNull String projectionSn,
                                            @NonNull String viewDomain,
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
